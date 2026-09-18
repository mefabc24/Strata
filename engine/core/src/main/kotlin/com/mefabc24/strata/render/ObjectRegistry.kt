package com.mefabc24.strata.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import kotlin.reflect.KClass

/**
 * Configures the visual appearance of an object type.
 */
class ObjectSpriteSettings {
    var offsetX: Float = 0f
    var offsetY: Float = 0f
}

/**
 * Loads and manages sprites for registered placeable types.
 */
class ObjectRegistry(
    directory: String
) : Disposable {

    private val baseDirectory = directory.trimEnd('/')

    private val textures = mutableMapOf<String, Texture>()

    private val visuals = mutableMapOf<KClass<out Placeable>, ObjectVisual>()

    /**
     * Registers a sprite for a placeable type.
     */
    fun <T : Placeable> register(
        type: KClass<T>,
        sprite: String,
        configure: ObjectSpriteSettings.() -> Unit = {}
    ) {
        require(type !in visuals) {
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

        val texture = textures.getOrPut(path) {
            Texture(Gdx.files.classpath(path)).apply {
                setFilter(
                    Texture.TextureFilter.Nearest,
                    Texture.TextureFilter.Nearest
                )
            }
        }

        visuals[type] = ObjectVisual(
            texture = TextureRegion(texture),
            offsetX = settings.offsetX,
            offsetY = settings.offsetY
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
     * Returns the visual registered for a placed object.
     */
    fun get(placed: PlacedObject): ObjectVisual? {
        return visuals[placed.placeable::class]
    }

    /**
     * Releases all textures owned by this registry.
     */
    override fun dispose() {
        textures.values.forEach { it.dispose() }

        textures.clear()
        visuals.clear()
    }
}