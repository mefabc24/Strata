package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.render.sprite.SpriteSheetGrid
import com.mefabc24.strata.render.sprite.SpriteSource
import com.mefabc24.strata.render.sprite.PreparedVisualDefinition
import com.mefabc24.strata.render.sprite.ResolvedVisual
import com.mefabc24.strata.render.sprite.SpriteDefinitionBuilder
import com.mefabc24.strata.render.sprite.StatefulSpriteBuilder
import com.mefabc24.strata.render.sprite.VisualDefinition
import com.mefabc24.strata.render.sprite.VisualPlayback
import com.mefabc24.strata.render.sprite.VisualStateId
import com.mefabc24.strata.render.sprite.spriteSource
import com.mefabc24.strata.render.sprite.alphaMasksFromAtlasClasspath
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import kotlin.reflect.KClass

/**
 * Configures the visual appearance of an object type.
 */
class ObjectSpriteSettings {
    var offsetX: Float = 0f
    var offsetY: Float = 0f

    var width: Float? = null
    var height: Float? = null
    var scale: Float = 1f

    internal fun validate() {
        require(offsetX.isFinite() && offsetY.isFinite()) {
            "Sprite offsets must be finite."
        }
        require(width == null || (width!!.isFinite() && width!! > 0f)) {
            "Sprite width must be finite and positive."
        }
        require(height == null || (height!!.isFinite() && height!! > 0f)) {
            "Sprite height must be finite and positive."
        }
        require(scale.isFinite() && scale > 0f) {
            "Sprite scale must be finite and positive."
        }
    }
}

/**
 * Describes one registered object type and its prepared rendering metadata.
 *
 * The visual references registry-owned texture data and must not be disposed
 * by callers.
 */
class ObjectEntry internal constructor(
    val type: KClass<out Placeable>,
    internal val definition: VisualDefinition<PlacedObject, SpriteSource>,
    private val factory: (() -> Placeable)?,
    internal val settings: ObjectSpriteSettings
) {
    internal val sources = definition.sources()
    val spritePath: String = sources.first().assetPaths.first()

    private var preparedDefinition:
        PreparedVisualDefinition<PlacedObject, ObjectVisual>? = null
    private var preparedVisuals: List<ObjectVisual> = emptyList()

    val visual: ObjectVisual
        get() {
            check(definition is VisualDefinition.Single) {
                "Object type $type is stateful and requires a runtime object."
            }
            return preparedVisuals.singleOrNull()
                ?: error("Object type $type is not prepared.")
        }

    val isPrepared: Boolean
        get() = preparedDefinition != null

    val isConstructible: Boolean
        get() = factory != null

    /** Creates a new placeable through the explicitly registered factory. */
    fun create(): Placeable {
        val create = factory
            ?: error("Object type $type has no registered factory.")
        val placeable = create()

        check(type.isInstance(placeable)) {
            "The factory for $type created ${placeable::class}."
        }

        return placeable
    }

    internal fun prepare(
        definition: PreparedVisualDefinition<PlacedObject, ObjectVisual>,
        visuals: List<ObjectVisual>
    ) {
        preparedDefinition = definition
        preparedVisuals = visuals
    }

    internal fun resolve(
        placed: PlacedObject,
        animationTime: Float
    ): ResolvedVisual<ObjectVisual> {
        return preparedDefinition?.resolve(placed, animationTime)
            ?: error("Object type $type is not prepared.")
    }
}

/** The active object visual and the clock used for its current frame. */
class ResolvedObjectVisual internal constructor(
    val visual: ObjectVisual,
    val stateTime: Float,
    val state: VisualStateId?
) {
    val frame: ObjectVisualFrame
        get() = visual.frameAt(stateTime)
}

/**
 * Maps placeable types to their visual configuration in registration order.
 *
 * Registration is available only during scene setup. Runtime reads remain
 * available after the owning scene closes registration and prepares entries.
 */
class ObjectRegistry internal constructor(
    directory: String,
    private val queueTexture: (String) -> Unit,
    private val regionFor: (String) -> TextureRegion,
    private val loadAlphaMask: (String) -> AlphaMask?,
    private val loadSpriteSheetAlphaMasks: (
        path: String,
        frameWidth: Int,
        frameHeight: Int,
        frameCount: Int?
    ) -> List<AlphaMask?> = ::alphaMasksFromSpriteSheetClasspath,
    private val queueAtlas: (String) -> Unit = {},
    private val atlasFor: (String) -> TextureAtlas = {
        error("Atlas resolver is not configured.")
    },
    private val loadAtlasAlphaMasks: (
        List<SpriteSource>
    ) -> Map<SpriteSource, List<AlphaMask?>> = ::alphaMasksFromAtlasClasspath
) {
    constructor(
        directory: String,
        assets: StrataAssets
    ) : this(
        directory = directory,
        queueTexture = assets::queueTexture,
        regionFor = assets::region,
        loadAlphaMask = ::alphaMaskFromClasspath,
        loadSpriteSheetAlphaMasks = ::alphaMasksFromSpriteSheetClasspath,
        queueAtlas = assets::queueAtlas,
        atlasFor = assets::atlas
    )

    private val baseDirectory = directory.trimEnd('/')
    private val registrations =
        linkedMapOf<KClass<out Placeable>, ObjectEntry>()

    private val alphaMasks = mutableMapOf<String, AlphaMask?>()
    private val spriteSheetAlphaMasks =
        mutableMapOf<SpriteSheetMaskKey, List<AlphaMask?>>()

    private var registrationOpen = true

    /** A snapshot of registered entries in registration order. */
    val entries: List<ObjectEntry>
        get() = registrations.values.toList()

    /** A registration-order snapshot containing entries with factories. */
    val constructibleEntries: List<ObjectEntry>
        get() = registrations.values.filter { it.isConstructible }

    /** Registers a static object sprite. */
    fun <T : Placeable> register(
        type: KClass<T>,
        sprite: String,
        factory: (() -> T)? = null,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        registerVisual(
            type = type,
            factory = factory,
            configure = configure
        ) { sprite(sprite) }
    }

    /**
     * Registers a static atlas region. The atlas path is used as supplied and
     * is not resolved relative to the object directory.
     */
    fun <T : Placeable> registerAtlas(
        type: KClass<T>,
        atlas: String,
        region: String,
        factory: (() -> T)? = null,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        registerVisual(
            type,
            factory,
            configure
        ) { atlas(atlas, region) }
    }

    /** Registers an object animation from ordered image files. */
    fun <T : Placeable> registerAnimated(
        type: KClass<T>,
        frames: List<String>,
        frameDuration: Float,
        factory: (() -> T)? = null,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        registerVisual(
            type = type,
            factory = factory,
            configure = configure
        ) { animated(frames, frameDuration) }
    }

    /** Registers indexed atlas regions as a looping animation. */
    fun <T : Placeable> registerAnimatedAtlas(
        type: KClass<T>,
        atlas: String,
        region: String,
        frameDuration: Float,
        factory: (() -> T)? = null,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        registerVisual(
            type,
            factory,
            configure
        ) { animatedAtlas(atlas, region, frameDuration) }
    }

    /** Registers an object animation from a tight spritesheet. */
    fun <T : Placeable> registerAnimated(
        type: KClass<T>,
        spriteSheet: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        frameCount: Int? = null,
        factory: (() -> T)? = null,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        registerVisual(
            type = type,
            factory = factory,
            configure = configure
        ) {
            spriteSheet(
                spriteSheet,
                frameWidth,
                frameHeight,
                frameDuration,
                frameCount
            )
        }
    }

    inline fun <reified T : Placeable> register(
        sprite: String,
        noinline factory: (() -> T)? = null,
        noinline configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        register(T::class, sprite, factory, configure)
    }

    inline fun <reified T : Placeable> registerAtlas(
        atlas: String,
        region: String,
        noinline factory: (() -> T)? = null,
        noinline configure: ObjectSpriteSettings.() -> Unit = {}
    ) = registerAtlas(T::class, atlas, region, factory, configure)

    inline fun <reified T : Placeable> registerAnimated(
        frames: List<String>,
        frameDuration: Float,
        noinline factory: (() -> T)? = null,
        noinline configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        registerAnimated(
            type = T::class,
            frames = frames,
            frameDuration = frameDuration,
            factory = factory,
            configure = configure
        )
    }

    inline fun <reified T : Placeable> registerAnimatedAtlas(
        atlas: String,
        region: String,
        frameDuration: Float,
        noinline factory: (() -> T)? = null,
        noinline configure: ObjectSpriteSettings.() -> Unit = {}
    ) = registerAnimatedAtlas(
        T::class,
        atlas,
        region,
        frameDuration,
        factory,
        configure
    )

    inline fun <reified T : Placeable> registerAnimated(
        spriteSheet: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        frameCount: Int? = null,
        noinline factory: (() -> T)? = null,
        noinline configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        registerAnimated(
            type = T::class,
            spriteSheet = spriteSheet,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            frameDuration = frameDuration,
            frameCount = frameCount,
            factory = factory,
            configure = configure
        )
    }

    /** Registers one sprite source through the shared sprite-definition DSL. */
    fun <T : Placeable> registerVisual(
        type: KClass<T>,
        factory: (() -> T)? = null,
        configure: ObjectSpriteSettings.() -> Unit = {},
        visual: SpriteDefinitionBuilder.() -> Unit
    ) {
        registerDefinition(
            type = type,
            definition = VisualDefinition.Single(
                spriteSource(::resolvePath, visual)
            ),
            factory = factory,
            configure = configure
        )
    }

    inline fun <reified T : Placeable> registerVisual(
        noinline factory: (() -> T)? = null,
        noinline configure: ObjectSpriteSettings.() -> Unit = {},
        noinline visual: SpriteDefinitionBuilder.() -> Unit
    ) = registerVisual(T::class, factory, configure, visual)

    /**
     * Registers game-defined states for a placed object type.
     *
     * Every state's assets are queued during scene setup. The resolver runs at
     * runtime and receives both engine placement data and the typed game object.
     * States use local playback by default, restarting when the state changes.
     */
    fun <T : Placeable> registerStateful(
        type: KClass<T>,
        factory: (() -> T)? = null,
        stateFor: (PlacedObject) -> VisualStateId,
        configure: ObjectSpriteSettings.() -> Unit = {},
        states: StatefulSpriteBuilder.() -> Unit
    ) = registerStateful(
        type,
        factory,
        { placed, _ -> stateFor(placed) },
        configure,
        states
    )

    fun <T : Placeable> registerStateful(
        type: KClass<T>,
        factory: (() -> T)? = null,
        stateFor: (PlacedObject, T) -> VisualStateId,
        configure: ObjectSpriteSettings.() -> Unit = {},
        states: StatefulSpriteBuilder.() -> Unit
    ) {
        checkRegistrationOpen()
        val definitions = StatefulSpriteBuilder(
            ::resolvePath,
            VisualPlayback.LOCAL
        ).apply(states).build()
        registerDefinition(
            type = type,
            definition = VisualDefinition.Stateful(
                states = definitions,
                stateFor = { placed ->
                    stateFor(placed, type.java.cast(placed.placeable))
                }
            ),
            factory = factory,
            configure = configure
        )
    }

    inline fun <reified T : Placeable> registerStateful(
        noinline factory: (() -> T)? = null,
        noinline stateFor: (PlacedObject) -> VisualStateId,
        noinline configure: ObjectSpriteSettings.() -> Unit = {},
        noinline states: StatefulSpriteBuilder.() -> Unit
    ) = registerStateful(T::class, factory, stateFor, configure, states)

    inline fun <reified T : Placeable> registerStateful(
        noinline factory: (() -> T)? = null,
        noinline stateFor: (PlacedObject, T) -> VisualStateId,
        noinline configure: ObjectSpriteSettings.() -> Unit = {},
        noinline states: StatefulSpriteBuilder.() -> Unit
    ) = registerStateful(T::class, factory, stateFor, configure, states)

    /** Resolves textures and builds alpha masks after loading. */
    internal fun prepare() {
        val preparedAtlasMasks = loadAtlasAlphaMasks(
            registrations.values.filterNot(ObjectEntry::isPrepared)
                .flatMap(ObjectEntry::sources)
                .filter {
                    it is SpriteSource.AtlasRegion ||
                        it is SpriteSource.AtlasAnimation
                }
        )
        for (entry in registrations.values) {
            if (entry.isPrepared) continue

            val settings = entry.settings
            val visualsBySource = entry.sources.associateWith { source ->
                val sprite = source.prepare(regionFor, atlasFor)
                val alphaMasks = alphaMasksFor(source, preparedAtlasMasks)
                require(alphaMasks.size == sprite.frameCount) {
                    "Object animation alpha-mask count must match its frame count."
                }
                ObjectVisual(
                    sprite = sprite,
                    alphaMasks = alphaMasks,
                    offsetX = settings.offsetX,
                    offsetY = settings.offsetY,
                    width = settings.width,
                    height = settings.height,
                    scale = settings.scale
                )
            }
            entry.prepare(
                definition = entry.definition.prepare(visualsBySource::getValue),
                visuals = entry.sources.map(visualsBySource::getValue)
            )
        }
    }

    internal fun freeze() {
        registrationOpen = false
    }

    fun get(placed: PlacedObject): ObjectVisual? {
        return registrations[placed.placeable::class]?.visual
    }

    /** Resolves the active state and animation clock for [placed]. */
    fun resolve(
        placed: PlacedObject,
        animationTime: Float
    ): ResolvedObjectVisual? {
        val resolved = registrations[placed.placeable::class]
            ?.resolve(placed, animationTime)
            ?: return null
        return ResolvedObjectVisual(
            visual = resolved.visual,
            stateTime = resolved.stateTime,
            state = resolved.state
        )
    }

    private fun <T : Placeable> registerDefinition(
        type: KClass<T>,
        definition: VisualDefinition<PlacedObject, SpriteSource>,
        factory: (() -> T)?,
        configure: ObjectSpriteSettings.() -> Unit
    ) {
        checkRegistrationOpen()
        require(type !in registrations) {
            "Object type $type is already registered."
        }

        val settings = ObjectSpriteSettings().apply(configure)
        settings.validate()
        definition.sources().forEach { it.queue(queueTexture, queueAtlas) }

        registrations[type] = ObjectEntry(
            type = type,
            definition = definition,
            factory = factory?.let { create -> { create() } },
            settings = settings
        )
    }

    private fun alphaMasksFor(
        source: SpriteSource,
        preparedAtlasMasks: Map<SpriteSource, List<AlphaMask?>>
    ): List<AlphaMask?> {
        return when (source) {
            is SpriteSource.Static -> listOf(alphaMaskFor(source.path))
            is SpriteSource.AnimatedFiles -> {
                source.assetPaths.map(::alphaMaskFor)
            }

            is SpriteSource.SpriteSheet -> {
                val key = SpriteSheetMaskKey(
                    path = source.path,
                    frameWidth = source.frameWidth,
                    frameHeight = source.frameHeight,
                    frameCount = source.frameCount
                )

                spriteSheetAlphaMasks.getOrPut(key) {
                    loadSpriteSheetAlphaMasks(
                        source.path,
                        source.frameWidth,
                        source.frameHeight,
                        source.frameCount
                    )
                }
            }

            is SpriteSource.SpriteSheetRow -> {
                error("Directional sprite-sheet rows are only valid for entities.")
            }

            is SpriteSource.AtlasRegion,
            is SpriteSource.AtlasAnimation -> {
                checkNotNull(preparedAtlasMasks[source]) {
                    "Atlas alpha masks were not prepared for $source."
                }
            }
        }
    }

    private fun alphaMaskFor(path: String): AlphaMask? {
        if (path in alphaMasks) return alphaMasks[path]

        return loadAlphaMask(path).also {
            alphaMasks[path] = it
        }
    }

    private fun resolvePath(sprite: String): String {
        return if (baseDirectory.isEmpty()) sprite else "$baseDirectory/$sprite"
    }

    private fun checkRegistrationOpen() {
        check(registrationOpen) {
            "Object registry registration is already closed."
        }
    }

    private data class SpriteSheetMaskKey(
        val path: String,
        val frameWidth: Int,
        val frameHeight: Int,
        val frameCount: Int?
    )
}

internal fun alphaMaskFromClasspath(path: String): AlphaMask {
    val pixmap = Pixmap(Gdx.files.classpath(path))

    return try {
        AlphaMask.fromPixmap(pixmap)
    } finally {
        pixmap.dispose()
    }
}

internal fun alphaMasksFromSpriteSheetClasspath(
    path: String,
    frameWidth: Int,
    frameHeight: Int,
    frameCount: Int?
): List<AlphaMask?> {
    val pixmap = Pixmap(Gdx.files.classpath(path))

    return try {
        SpriteSheetGrid.cells(
            sheetWidth = pixmap.width,
            sheetHeight = pixmap.height,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            frameCount = frameCount
        ).map { cell ->
            AlphaMask.fromPixmap(
                pixmap = pixmap,
                x = cell.x,
                y = cell.y,
                width = cell.width,
                height = cell.height
            )
        }
    } finally {
        pixmap.dispose()
    }
}
