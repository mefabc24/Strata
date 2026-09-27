package com.mefabc24.strata.scene

import com.badlogic.gdx.graphics.Color

enum class DebugGridRenderLayer {
    BELOW_OBJECTS,
    ABOVE_OBJECTS
}

enum class DebugGridExtent {
    WORLD,
    VISIBLE
}

/**
 * Groups the scene's debugging facilities.
 */
class DebugSettings {

    val performance = ScenePerformanceLogger()

    val grid = DebugGridSettings()

    /** Runtime settings for placed-object diagnostics. */
    val objects = DebugObjectSettings()

    /** Runtime settings for world-entity diagnostics. */
    val entities = DebugEntitySettings()

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

    fun objects(
        configure: DebugObjectSettings.() -> Unit
    ) {
        objects.apply(configure)
    }

    fun entities(
        configure: DebugEntitySettings.() -> Unit
    ) {
        entities.apply(configure)
    }
}

/** Runtime configuration for placed-object diagnostics. */
class DebugObjectSettings {

    var enabled: Boolean = false

    /** Shows every logical tile returned by the object's footprint. */
    var showOccupiedTiles: Boolean = true

    /** Shows the world tile where the object was placed. */
    var showOriginTile: Boolean = true

    /** Shows the bounds of the object's active sprite frame. */
    var showSpriteBounds: Boolean = false

    var lineWidth: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug object line width must be finite and positive."
            }
            field = value
        }

    private var storedOccupiedTileColor = Color(0.2f, 0.85f, 1f, 1f)
    var occupiedTileColor: Color
        get() = storedOccupiedTileColor.cpy()
        set(value) {
            storedOccupiedTileColor = value.cpy()
        }

    private var storedOccupiedTileFillColor: Color? =
        Color(0.2f, 0.65f, 1f, 0.18f)
    var occupiedTileFillColor: Color?
        get() = storedOccupiedTileFillColor?.cpy()
        set(value) {
            storedOccupiedTileFillColor = value?.cpy()
        }

    private var storedOriginTileColor = Color(1f, 0.35f, 0.2f, 1f)
    var originTileColor: Color
        get() = storedOriginTileColor.cpy()
        set(value) {
            storedOriginTileColor = value.cpy()
        }

    private var storedSpriteBoundsColor = Color(1f, 0.2f, 0.75f, 1f)
    var spriteBoundsColor: Color
        get() = storedSpriteBoundsColor.cpy()
        set(value) {
            storedSpriteBoundsColor = value.cpy()
        }
}

/** Runtime configuration for world-entity diagnostics. */
class DebugEntitySettings {

    var enabled: Boolean = false

    /** Shows the logical tile containing the entity's continuous position. */
    var showCurrentTile: Boolean = true

    /** Shows the entity's exact ground anchor. */
    var showPosition: Boolean = true

    /** Shows the route already stored by the entity's movement state. */
    var showPath: Boolean = true

    /** Shows a ground-plane line in the entity's facing direction. */
    var showDirection: Boolean = false

    /** Shows the bounds of the entity's active state, direction, and frame. */
    var showSpriteBounds: Boolean = false

    var lineWidth: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug entity line width must be finite and positive."
            }
            field = value
        }

    private var storedCurrentTileColor = Color(0.4f, 1f, 0.3f, 1f)
    var currentTileColor: Color
        get() = storedCurrentTileColor.cpy()
        set(value) {
            storedCurrentTileColor = value.cpy()
        }

    private var storedCurrentTileFillColor: Color? =
        Color(0.3f, 1f, 0.3f, 0.16f)
    var currentTileFillColor: Color?
        get() = storedCurrentTileFillColor?.cpy()
        set(value) {
            storedCurrentTileFillColor = value?.cpy()
        }

    private var storedPositionColor = Color(1f, 0.3f, 0.2f, 1f)
    var positionColor: Color
        get() = storedPositionColor.cpy()
        set(value) {
            storedPositionColor = value.cpy()
        }

    private var storedPathColor = Color(1f, 0.85f, 0.2f, 1f)
    var pathColor: Color
        get() = storedPathColor.cpy()
        set(value) {
            storedPathColor = value.cpy()
        }

    private var storedDirectionColor = Color(0.3f, 0.75f, 1f, 1f)
    var directionColor: Color
        get() = storedDirectionColor.cpy()
        set(value) {
            storedDirectionColor = value.cpy()
        }

    private var storedSpriteBoundsColor = Color(1f, 0.3f, 0.9f, 1f)
    var spriteBoundsColor: Color
        get() = storedSpriteBoundsColor.cpy()
        set(value) {
            storedSpriteBoundsColor = value.cpy()
        }
}

/**
 * Runtime configuration for the isometric world-grid overlay.
 */
class DebugGridSettings {

    var enabled: Boolean = false

    var renderLayer: DebugGridRenderLayer =
        DebugGridRenderLayer.BELOW_OBJECTS

    var extent: DebugGridExtent = DebugGridExtent.WORLD

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
            it.extent = extent
            it.color = color
            it.hoverColor = hoverColor
            it.lineWidth = lineWidth
            it.backgroundColor = backgroundColor
            it.hoverBackgroundColor = hoverBackgroundColor
        }
    }
}
