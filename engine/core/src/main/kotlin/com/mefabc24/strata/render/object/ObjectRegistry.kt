package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.assets.StrataAssets
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
    val spritePath: String,
    private val factory: (() -> Placeable)?,
    internal val settings: ObjectSpriteSettings
) {
    private var preparedVisual: ObjectVisual? = null

    /** The visual used to render this object type. */
    val visual: ObjectVisual
        get() = preparedVisual
            ?: error("Object type $type is not prepared.")

    /** Whether [visual] is ready for use. */
    val isPrepared: Boolean
        get() = preparedVisual != null

    /** Whether this registration can create new object instances. */
    val isConstructible: Boolean
        get() = factory != null

    /**
     * Creates a new placeable through the explicitly registered factory.
     */
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
    private val loadAlphaMask: (String) -> AlphaMask?
) {
    constructor(
        directory: String,
        assets: StrataAssets
    ) : this(
        directory = directory,
        queueTexture = assets::queueTexture,
        regionFor = assets::region,
        loadAlphaMask = ::alphaMaskFromClasspath
    )

    private val baseDirectory = directory.trimEnd('/')

    private val registrations =
        linkedMapOf<KClass<out Placeable>, ObjectEntry>()

    private val alphaMasks = mutableMapOf<String, AlphaMask?>()
    private var registrationOpen = true

    /**
     * A snapshot of registered object entries in registration order.
     *
     * Mutating the returned list cannot change this registry.
     */
    val entries: List<ObjectEntry>
        get() = registrations.values.toList()

    /**
     * A registration-order snapshot containing entries with explicit factories.
     * Every returned entry can safely be passed to [ObjectEntry.create].
     */
    val constructibleEntries: List<ObjectEntry>
        get() = registrations.values.filter {
            it.isConstructible
        }

    /**
     * Registers an object type and queues its texture.
     *
     * [factory] is optional because some registrations are needed only for
     * rendering existing objects. Supply it when game code must construct new
     * instances, such as from a build picker.
     */
    fun <T : Placeable> register(
        type: KClass<T>,
        sprite: String,
        factory: (() -> T)? = null,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        checkRegistrationOpen()

        require(type !in registrations) {
            "Object type $type is already registered."
        }

        require(sprite.isNotBlank()) {
            "Sprite path must not be blank."
        }

        val settings = ObjectSpriteSettings().apply(configure)
        settings.validate()

        val path = if (baseDirectory.isEmpty()) {
            sprite
        } else {
            "$baseDirectory/$sprite"
        }

        queueTexture(path)

        registrations[type] = ObjectEntry(
            type = type,
            spritePath = path,
            factory = factory?.let { create ->
                { create() }
            },
            settings = settings
        )
    }

    /**
     * Registers a sprite using a reified placeable type.
     *
     * The optional [factory] has the same construction semantics as the
     * non-reified overload.
     */
    inline fun <reified T : Placeable> register(
        sprite: String,
        noinline factory: (() -> T)? = null,
        noinline configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        register(
            type = T::class,
            sprite = sprite,
            factory = factory,
            configure = configure
        )
    }

    /**
     * Resolves textures and builds alpha masks after loading.
     */
    internal fun prepare() {
        for (entry in registrations.values) {
            if (entry.isPrepared) continue

            val path = entry.spritePath
            val settings = entry.settings

            val alphaMask = if (path in alphaMasks) {
                alphaMasks[path]
            } else {
                loadAlphaMask(path).also {
                    alphaMasks[path] = it
                }
            }

            entry.prepare(
                ObjectVisual(
                    texture = regionFor(path),
                    offsetX = settings.offsetX,
                    offsetY = settings.offsetY,
                    alphaMask = alphaMask,
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

    /**
     * Returns the prepared visual for a placed object.
     */
    fun get(placed: PlacedObject): ObjectVisual? {
        val entry = registrations[placed.placeable::class]
            ?: return null

        return entry.visual
    }

    private fun checkRegistrationOpen() {
        check(registrationOpen) {
            "Object registry registration is already closed."
        }
    }
}

private fun alphaMaskFromClasspath(path: String): AlphaMask {
    val pixmap = Pixmap(Gdx.files.classpath(path))

    return try {
        AlphaMask.fromPixmap(pixmap)
    } finally {
        pixmap.dispose()
    }
}
