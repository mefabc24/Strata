package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.render.sprite.SpriteSheetGrid
import com.mefabc24.strata.render.sprite.SpriteSource
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
    internal val source: SpriteSource,
    private val factory: (() -> Placeable)?,
    internal val settings: ObjectSpriteSettings
) {
    val spritePath: String = source.assetPaths.first()

    private var preparedVisual: ObjectVisual? = null

    val visual: ObjectVisual
        get() = preparedVisual
            ?: error("Object type $type is not prepared.")

    val isPrepared: Boolean
        get() = preparedVisual != null

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

    internal fun prepare(visual: ObjectVisual) {
        preparedVisual = visual
    }
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
    ) -> List<AlphaMask?> = ::alphaMasksFromSpriteSheetClasspath
) {
    constructor(
        directory: String,
        assets: StrataAssets
    ) : this(
        directory = directory,
        queueTexture = assets::queueTexture,
        regionFor = assets::region,
        loadAlphaMask = ::alphaMaskFromClasspath,
        loadSpriteSheetAlphaMasks = ::alphaMasksFromSpriteSheetClasspath
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
        checkRegistrationOpen()
        require(sprite.isNotBlank()) {
            "Sprite path must not be blank."
        }

        registerSource(
            type = type,
            source = SpriteSource.Static(resolvePath(sprite)),
            factory = factory,
            configure = configure
        )
    }

    /** Registers an object animation from ordered image files. */
    fun <T : Placeable> registerAnimated(
        type: KClass<T>,
        frames: List<String>,
        frameDuration: Float,
        factory: (() -> T)? = null,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        checkRegistrationOpen()
        require(frames.isNotEmpty()) {
            "An animation must contain at least one frame path."
        }
        require(frames.all { it.isNotBlank() }) {
            "Animation frame paths must not be blank."
        }

        registerSource(
            type = type,
            source = SpriteSource.AnimatedFiles(
                paths = frames.map(::resolvePath),
                frameDuration = frameDuration
            ),
            factory = factory,
            configure = configure
        )
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
        checkRegistrationOpen()
        require(spriteSheet.isNotBlank()) {
            "Sprite sheet path must not be blank."
        }

        registerSource(
            type = type,
            source = SpriteSource.SpriteSheet(
                path = resolvePath(spriteSheet),
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                frameDuration = frameDuration,
                frameCount = frameCount
            ),
            factory = factory,
            configure = configure
        )
    }

    inline fun <reified T : Placeable> register(
        sprite: String,
        noinline factory: (() -> T)? = null,
        noinline configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        register(T::class, sprite, factory, configure)
    }

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

    /** Resolves textures and builds alpha masks after loading. */
    internal fun prepare() {
        for (entry in registrations.values) {
            if (entry.isPrepared) continue

            val sprite = entry.source.prepare(regionFor)
            val alphaMasks = alphaMasksFor(entry.source)

            require(alphaMasks.size == sprite.frameCount) {
                "Object animation alpha-mask count must match its frame count."
            }

            val settings = entry.settings
            entry.prepare(
                ObjectVisual(
                    sprite = sprite,
                    alphaMasks = alphaMasks,
                    offsetX = settings.offsetX,
                    offsetY = settings.offsetY,
                    width = settings.width,
                    height = settings.height,
                    scale = settings.scale
                )
            )
        }
    }

    internal fun freeze() {
        registrationOpen = false
    }

    fun get(placed: PlacedObject): ObjectVisual? {
        return registrations[placed.placeable::class]?.visual
    }

    private fun <T : Placeable> registerSource(
        type: KClass<T>,
        source: SpriteSource,
        factory: (() -> T)?,
        configure: ObjectSpriteSettings.() -> Unit
    ) {
        require(type !in registrations) {
            "Object type $type is already registered."
        }

        val settings = ObjectSpriteSettings().apply(configure)
        settings.validate()
        source.assetPaths.forEach(queueTexture)

        registrations[type] = ObjectEntry(
            type = type,
            source = source,
            factory = factory?.let { create -> { create() } },
            settings = settings
        )
    }

    private fun alphaMasksFor(source: SpriteSource): List<AlphaMask?> {
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
