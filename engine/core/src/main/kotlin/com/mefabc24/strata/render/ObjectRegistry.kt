package com.mefabc24.strata.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
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
}

/**
 * Maps placeable types to their visual configuration.
 */
class ObjectRegistry(
    directory: String,
    private val assets: StrataAssets
) {
    private data class Registration(
        val path: String,
        val settings: ObjectSpriteSettings
    )

    private val baseDirectory = directory.trimEnd('/')

    private val registrations =
        mutableMapOf<KClass<out Placeable>, Registration>()

    private val alphaMasks = mutableMapOf<String, AlphaMask>()

    private val visuals =
        mutableMapOf<KClass<out Placeable>, ObjectVisual>()

    /**
     * Registers an object type and queues its texture.
     */
    fun <T : Placeable> register(
        type: KClass<T>,
        sprite: String,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        require(type !in registrations) {
            "Object type $type is already registered."
        }

        require(sprite.isNotBlank()) {
            "Sprite path must not be blank."
        }

        val settings = ObjectSpriteSettings().apply(configure)

        val path = if (baseDirectory.isEmpty()) {
            sprite
        } else {
            "$baseDirectory/$sprite"
        }

        assets.queueTexture(path)

        registrations[type] = Registration(
            path = path,
            settings = settings
        )
    }

    /**
     * Registers a sprite using a reified placeable type.
     */
    inline fun <reified T : Placeable> register(
        sprite: String,
        noinline configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        register(T::class, sprite, configure)
    }

    /**
     * Resolves textures and builds alpha masks after loading.
     */
    fun prepare() {
        for ((type, registration) in registrations) {
            if (type in visuals) continue

            val path = registration.path
            val settings = registration.settings

            val region = assets.region(path)

            val alphaMask = alphaMasks.getOrPut(path) {
                val pixmap = Pixmap(Gdx.files.classpath(path))

                try {
                    AlphaMask.fromPixmap(pixmap)
                } finally {
                    pixmap.dispose()
                }
            }

            visuals[type] = ObjectVisual(
                texture = region,
                offsetX = settings.offsetX,
                offsetY = settings.offsetY,
                alphaMask = alphaMask,
                width = settings.width,
                height = settings.height,
                scale = settings.scale
            )
        }
    }

    /**
     * Returns the prepared visual for a placed object.
     */
    fun get(placed: PlacedObject): ObjectVisual? {
        return visuals[placed.placeable::class]
    }
}