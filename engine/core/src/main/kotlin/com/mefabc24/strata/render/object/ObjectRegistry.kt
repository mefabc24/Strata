package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.render.sprite.AlphaMask
import com.mefabc24.strata.render.sprite.SpriteSource
import com.mefabc24.strata.render.sprite.SpriteAlphaMaskCache
import com.mefabc24.strata.render.sprite.PreparedVisualDefinition
import com.mefabc24.strata.render.sprite.ResolvedVisual
import com.mefabc24.strata.render.sprite.SpriteDefinitionBuilder
import com.mefabc24.strata.render.sprite.StatefulSpriteBuilder
import com.mefabc24.strata.render.sprite.VisualDefinition
import com.mefabc24.strata.render.sprite.VisualPlayback
import com.mefabc24.strata.render.sprite.VisualStateId
import com.mefabc24.strata.render.sprite.spriteSource
import com.mefabc24.strata.render.sprite.alphaMaskFromClasspath
import com.mefabc24.strata.render.sprite.alphaMasksFromAtlasClasspath
import com.mefabc24.strata.render.sprite.alphaMasksFromSpriteSheetClasspath
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import kotlin.reflect.KClass

/**
 * Configures the visual appearance of an object type.
 */
class ObjectSpriteSettings {
    /**
     * Controls this object type's world rendering group.
     * Lower priorities render before higher priorities. The default is `0`.
     */
    var renderPriority: Int = 0

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
    internal val selectionSource: SpriteSource?,
    private val factory: (() -> Placeable)?,
    internal val settings: ObjectSpriteSettings
) {
    internal val sources = definition.sources()
    internal val allSources = (sources + listOfNotNull(selectionSource)).distinct()

    private var preparedDefinition:
        PreparedVisualDefinition<PlacedObject, ObjectVisual>? = null
    private var preparedVisuals: List<ObjectVisual> = emptyList()
    private var preparedSelectionVisual: ObjectVisual? = null

    val visual: ObjectVisual
        get() {
            check(definition is VisualDefinition.Single) {
                "Object type $type is stateful and requires a runtime object."
            }
            return preparedVisuals.singleOrNull()
                ?: error("Object type $type is not prepared.")
        }

    /**
     * Stable visual for construction menus and other contexts that do not
     * have a runtime [PlacedObject]. Stateful registrations with a factory
     * must configure this visual explicitly.
     */
    val selectionVisual: ObjectVisual
        get() = preparedSelectionVisual
            ?: error("Object type $type has no prepared selection visual.")

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
        visuals: List<ObjectVisual>,
        selectionVisual: ObjectVisual?
    ) {
        preparedDefinition = definition
        preparedVisuals = visuals
        preparedSelectionVisual = selectionVisual
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

    private val alphaMaskCache = SpriteAlphaMaskCache(
        loadFile = loadAlphaMask,
        loadSheet = loadSpriteSheetAlphaMasks,
        sheetWidthFor = { path -> regionFor(path).regionWidth }
    )

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
        val source = spriteSource(::resolvePath, visual)
        registerDefinition(
            type = type,
            definition = VisualDefinition.Single(source),
            selectionSource = source,
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
     * A constructible registration must provide [selection] because Strata
     * cannot infer which game-defined state represents the object in a menu.
     */
    fun <T : Placeable> registerStateful(
        type: KClass<T>,
        factory: (() -> T)? = null,
        stateFor: (PlacedObject) -> VisualStateId,
        configure: ObjectSpriteSettings.() -> Unit = {},
        selection: (SpriteDefinitionBuilder.() -> Unit)? = null,
        states: StatefulSpriteBuilder.() -> Unit
    ) = registerStateful(
        type = type,
        factory = factory,
        stateFor = { placed, _ -> stateFor(placed) },
        configure = configure,
        selection = selection,
        states = states
    )

    fun <T : Placeable> registerStateful(
        type: KClass<T>,
        factory: (() -> T)? = null,
        stateFor: (PlacedObject, T) -> VisualStateId,
        configure: ObjectSpriteSettings.() -> Unit = {},
        selection: (SpriteDefinitionBuilder.() -> Unit)? = null,
        states: StatefulSpriteBuilder.() -> Unit
    ) {
        checkRegistrationOpen()
        require(factory == null || selection != null) {
            "Stateful constructible object type $type requires a selection visual."
        }
        val definitions = StatefulSpriteBuilder(
            ::resolvePath,
            VisualPlayback.LOCAL
        ).apply(states).build()
        val selectionSource = selection?.let { spriteSource(::resolvePath, it) }
        registerDefinition(
            type = type,
            definition = VisualDefinition.Stateful(
                states = definitions,
                stateFor = { placed ->
                    stateFor(placed, type.java.cast(placed.placeable))
                },
                playbackIdentityFor = PlacedObject::placeable
            ),
            selectionSource = selectionSource,
            factory = factory,
            configure = configure
        )
    }

    inline fun <reified T : Placeable> registerStateful(
        noinline factory: (() -> T)? = null,
        noinline stateFor: (PlacedObject) -> VisualStateId,
        noinline configure: ObjectSpriteSettings.() -> Unit = {},
        noinline selection: (SpriteDefinitionBuilder.() -> Unit)? = null,
        noinline states: StatefulSpriteBuilder.() -> Unit
    ) = registerStateful(
        type = T::class,
        factory = factory,
        stateFor = stateFor,
        configure = configure,
        selection = selection,
        states = states
    )

    inline fun <reified T : Placeable> registerStateful(
        noinline factory: (() -> T)? = null,
        noinline stateFor: (PlacedObject, T) -> VisualStateId,
        noinline configure: ObjectSpriteSettings.() -> Unit = {},
        noinline selection: (SpriteDefinitionBuilder.() -> Unit)? = null,
        noinline states: StatefulSpriteBuilder.() -> Unit
    ) = registerStateful(
        type = T::class,
        factory = factory,
        stateFor = stateFor,
        configure = configure,
        selection = selection,
        states = states
    )

    /** Resolves textures and builds alpha masks after loading. */
    internal fun prepare() {
        val preparedAtlasMasks = loadAtlasAlphaMasks(
            registrations.values.filterNot(ObjectEntry::isPrepared)
                .flatMap(ObjectEntry::allSources)
                .filter {
                    it is SpriteSource.AtlasRegion ||
                        it is SpriteSource.AtlasAnimation
                }
        )
        for (entry in registrations.values) {
            if (entry.isPrepared) continue

            val settings = entry.settings
            val visualsBySource = entry.allSources.associateWith { source ->
                val sprite = source.prepare(regionFor, atlasFor)
                val alphaMasks = alphaMaskCache.masksFor(
                    source,
                    preparedAtlasMasks
                )
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
                visuals = entry.sources.map(visualsBySource::getValue),
                selectionVisual = entry.selectionSource
                    ?.let(visualsBySource::getValue)
            )
        }
    }

    internal fun freeze() {
        registrationOpen = false
    }

    fun get(placed: PlacedObject): ObjectVisual? {
        return registrations[placed.placeable::class]?.visual
    }

    /** Returns the registered world render priority for [placed]. */
    internal fun renderPriority(placed: PlacedObject): Int =
        registrations[placed.placeable::class]?.settings?.renderPriority ?: 0

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
        selectionSource: SpriteSource?,
        factory: (() -> T)?,
        configure: ObjectSpriteSettings.() -> Unit
    ) {
        checkRegistrationOpen()
        require(type !in registrations) {
            "Object type $type is already registered."
        }

        val settings = ObjectSpriteSettings().apply(configure)
        settings.validate()
        (definition.sources() + listOfNotNull(selectionSource))
            .distinct()
            .forEach { it.queue(queueTexture, queueAtlas) }

        registrations[type] = ObjectEntry(
            type = type,
            definition = definition,
            selectionSource = selectionSource,
            factory = factory?.let { create -> { create() } },
            settings = settings
        )
    }

    private fun resolvePath(sprite: String): String {
        return if (baseDirectory.isEmpty()) sprite else "$baseDirectory/$sprite"
    }

    private fun checkRegistrationOpen() {
        check(registrationOpen) {
            "Object registry registration is already closed."
        }
    }
}
