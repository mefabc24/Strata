package com.mefabc24.strata.debug

import com.badlogic.gdx.graphics.Color

/** Runtime configuration for placed-object diagnostics. */
class DebugObjectSettings {
    var showOccupiedTiles: Boolean = false
    var showOriginTile: Boolean = false
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
        set(value) { storedOccupiedTileColor = value.cpy() }

    private var storedOccupiedTileFillColor: Color? = null
    var occupiedTileFillColor: Color?
        get() = storedOccupiedTileFillColor?.cpy()
        set(value) { storedOccupiedTileFillColor = value?.cpy() }

    private var storedOriginTileColor = Color(1f, 0.35f, 0.2f, 1f)
    var originTileColor: Color
        get() = storedOriginTileColor.cpy()
        set(value) { storedOriginTileColor = value.cpy() }

    private var storedSpriteBoundsColor = Color(1f, 0.2f, 0.75f, 1f)
    var spriteBoundsColor: Color
        get() = storedSpriteBoundsColor.cpy()
        set(value) { storedSpriteBoundsColor = value.cpy() }
}

/** Runtime configuration for world-entity diagnostics. */
class DebugEntitySettings {
    var showCurrentTile: Boolean = false
    var showPosition: Boolean = false
    var showPath: Boolean = false
    var showDirection: Boolean = false
    var showSpriteBounds: Boolean = false
    var showMovementTrail: Boolean = false
    var showMovementVector: Boolean = false
    var showNextWaypoint: Boolean = false
    var showMovementSpeed: Boolean = false
    var showPositionTileOffset: Boolean = false

    var trailMaxPositions: Int = 120
        set(value) {
            require(value in 2..4096) {
                "Entity trail maximum positions must be between 2 and 4096."
            }
            field = value
        }

    var trailHistoryDurationSeconds: Float = 5f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Entity trail history duration must be finite and positive."
            }
            field = value
        }

    var trailMinimumDistance: Float = 0.02f
        set(value) {
            require(value.isFinite() && value >= 0f) {
                "Entity trail minimum distance must be finite and non-negative."
            }
            field = value
        }

    var trailOpacity: Float = 0.65f
        set(value) {
            require(value.isFinite() && value in 0f..1f) {
                "Entity trail opacity must be between zero and one."
            }
            field = value
        }

    /** Length of the velocity arrow in seconds of current movement. */
    var movementVectorScaleSeconds: Float = 0.5f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Entity movement vector scale must be finite and positive."
            }
            field = value
        }

    internal var trailClearGeneration: Long = 0L
        private set

    /** Clears engine-owned movement trail history on the next debug update. */
    fun clearMovementTrails() {
        trailClearGeneration++
    }

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
        set(value) { storedCurrentTileColor = value.cpy() }

    private var storedCurrentTileFillColor: Color? = null
    var currentTileFillColor: Color?
        get() = storedCurrentTileFillColor?.cpy()
        set(value) { storedCurrentTileFillColor = value?.cpy() }

    private var storedPositionColor = Color(1f, 0.3f, 0.2f, 1f)
    var positionColor: Color
        get() = storedPositionColor.cpy()
        set(value) { storedPositionColor = value.cpy() }

    private var storedPathColor = Color(1f, 0.85f, 0.2f, 1f)
    var pathColor: Color
        get() = storedPathColor.cpy()
        set(value) { storedPathColor = value.cpy() }

    private var storedDirectionColor = Color(0.3f, 0.75f, 1f, 1f)
    var directionColor: Color
        get() = storedDirectionColor.cpy()
        set(value) { storedDirectionColor = value.cpy() }

    private var storedTrailColor = Color(0.2f, 0.9f, 1f, 1f)
    var trailColor: Color
        get() = storedTrailColor.cpy()
        set(value) { storedTrailColor = value.cpy() }

    private var storedMovementVectorColor = Color(1f, 0.45f, 0.15f, 1f)
    var movementVectorColor: Color
        get() = storedMovementVectorColor.cpy()
        set(value) { storedMovementVectorColor = value.cpy() }

    private var storedNextWaypointColor = Color(1f, 0.2f, 0.75f, 1f)
    var nextWaypointColor: Color
        get() = storedNextWaypointColor.cpy()
        set(value) { storedNextWaypointColor = value.cpy() }

    private var storedPositionTileOffsetColor = Color(0.8f, 0.5f, 1f, 1f)
    var positionTileOffsetColor: Color
        get() = storedPositionTileOffsetColor.cpy()
        set(value) { storedPositionTileOffsetColor = value.cpy() }

    private var storedSpriteBoundsColor = Color(1f, 0.3f, 0.9f, 1f)
    var spriteBoundsColor: Color
        get() = storedSpriteBoundsColor.cpy()
        set(value) { storedSpriteBoundsColor = value.cpy() }
}

internal val DebugObjectSettings.hasActiveVisuals: Boolean
    get() = showOccupiedTiles || showOriginTile || showSpriteBounds ||
        occupiedTileFillColor != null

internal val DebugEntitySettings.hasActiveVisuals: Boolean
    get() = showCurrentTile || showPosition || showPath || showDirection ||
        showSpriteBounds || showMovementTrail || showMovementVector ||
        showNextWaypoint || showMovementSpeed || showPositionTileOffset ||
        currentTileFillColor != null
