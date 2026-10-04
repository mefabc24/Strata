package com.mefabc24.strata.lighting

import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.world.EntityPosition
import java.util.Collections

/**
 * Scene-owned ambient and point lighting state.
 *
 * Point-light positions and radii use logical tile-space units. Lighting is
 * disabled by default so existing scenes retain their original appearance.
 */
class Lighting {

    /** Maximum number of point lights supported by the lighting shader. */
    companion object {
        const val MAX_POINT_LIGHTS = 16
    }

    var enabled: Boolean = false

    var ambientIntensity: Float = 1f
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "Ambient light intensity must be finite and non-negative."
            }
            field = value
        }

    private var storedAmbientColor = Color.WHITE.cpy()

    var ambientColor: Color
        get() = storedAmbientColor.cpy()
        set(value) {
            validateColor(value, "Ambient light color")
            storedAmbientColor = value.cpy()
        }

    private val mutablePointLights = mutableListOf<PointLight>()

    /** Point lights in deterministic insertion order. */
    val pointLights: List<PointLight> =
        Collections.unmodifiableList(mutablePointLights)

    /** Adds a point light and returns its runtime handle. */
    fun addPointLight(
        position: EntityPosition,
        radius: Float,
        intensity: Float,
        color: Color
    ): PointLight {
        check(mutablePointLights.size < MAX_POINT_LIGHTS) {
            "Lighting supports at most $MAX_POINT_LIGHTS point lights."
        }

        return PointLight(
            position = position,
            radius = radius,
            intensity = intensity,
            color = color
        ).also(mutablePointLights::add)
    }

    /** Removes [light] if it belongs to this scene. */
    fun remove(light: PointLight): Boolean {
        return mutablePointLights.remove(light)
    }

    /** Removes all point lights. */
    fun clearPointLights() {
        mutablePointLights.clear()
    }

    internal val shaderAmbientColor: Color
        get() = storedAmbientColor
}

/** A mutable local light on the logical tile plane. */
class PointLight internal constructor(
    position: EntityPosition,
    radius: Float,
    intensity: Float,
    color: Color
) {

    var enabled: Boolean = true

    var position: EntityPosition = position
        set(value) {
            field = value
        }

    var radius: Float = radius
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Point light radius must be finite and positive."
            }
            field = value
        }

    var intensity: Float = intensity
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "Point light intensity must be finite and non-negative."
            }
            field = value
        }

    private var storedColor = color.cpy()

    var color: Color
        get() = storedColor.cpy()
        set(value) {
            validateColor(value, "Point light color")
            storedColor = value.cpy()
        }

    init {
        this.radius = radius
        this.intensity = intensity
        this.color = color
    }

    internal val shaderColor: Color
        get() = storedColor
}

private fun validateColor(color: Color, name: String) {
    require(
        color.r.isFinite() &&
            color.g.isFinite() &&
            color.b.isFinite() &&
            color.a.isFinite()
    ) {
        "$name components must be finite."
    }
}
