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
 * Runtime configuration for the isometric world-grid overlay.
 */
class DebugGridSettings {

    var enabled: Boolean = false

    var renderLayer: DebugGridRenderLayer =
        DebugGridRenderLayer.BELOW_OBJECTS

    private var storedColor = Color(
        0.4f,
        0.8f,
        0.5f,
        1f
    )

    var color: Color
        get() = storedColor.cpy()
        set(value) {
            storedColor = value.cpy()
        }

    private var storedHoverColor = Color(
        1f,
        0.85f,
        0.2f,
        1f
    )

    var hoverColor: Color
        get() = storedHoverColor.cpy()
        set(value) {
            storedHoverColor = value.cpy()
        }

    var lineWidth: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug grid line width must be finite and positive."
            }

            field = value
        }

    private var storedBackgroundColor: Color? = null

    var backgroundColor: Color?
        get() = storedBackgroundColor?.cpy()
        set(value) {
            storedBackgroundColor = value?.cpy()
        }

    private var storedHoverBackgroundColor: Color? = null

    var hoverBackgroundColor: Color?
        get() = storedHoverBackgroundColor?.cpy()
        set(value) {
            storedHoverBackgroundColor = value?.cpy()
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
