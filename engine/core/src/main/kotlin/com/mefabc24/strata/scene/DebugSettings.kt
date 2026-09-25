package com.mefabc24.strata.scene

import com.badlogic.gdx.graphics.Color

enum class DebugGridRenderLayer {
    BELOW_OBJECTS,
    ABOVE_OBJECTS
}

/**
 * Groups the scene's debugging facilities.
 */
class DebugSettings {

    val performance = ScenePerformanceLogger()

    val grid = DebugGridSettings()

    fun performance(
        configure: ScenePerformanceLogger.() -> Unit
    ) {
        performance.apply(configure)
    }

    fun grid(
        configure: DebugGridSettings.() -> Unit
    ) {
        grid.apply(configure)
    }
}

/**
 * Setup-time configuration for the isometric world-grid overlay.
 */
class DebugGridSettings {

    var enabled: Boolean = false

    var renderLayer: DebugGridRenderLayer =
        DebugGridRenderLayer.BELOW_OBJECTS

    var color: Color = Color(
        0.4f,
        0.8f,
        0.5f,
        1f
    )
        set(value) {
            field = value.cpy()
        }

    var hoverColor: Color = Color(
        1f,
        0.85f,
        0.2f,
        1f
    )
        set(value) {
            field = value.cpy()
        }

    var lineWidth: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug grid line width must be finite and positive."
            }

            field = value
        }

    var backgroundColor: Color? = null
        set(value) {
            field = value?.cpy()
        }

    var hoverBackgroundColor: Color? = null
        set(value) {
            field = value?.cpy()
        }

    internal fun copy(): DebugGridSettings {
        return DebugGridSettings().also {
            it.enabled = enabled
            it.renderLayer = renderLayer
            it.color = color
            it.hoverColor = hoverColor
            it.lineWidth = lineWidth
            it.backgroundColor = backgroundColor
            it.hoverBackgroundColor = hoverBackgroundColor
        }
    }
}