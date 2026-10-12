package com.mefabc24.strata.debug

import com.badlogic.gdx.graphics.Color

enum class RenderOrderDebugMode { CALCULATED, ACTUAL }
enum class RenderPriorityFocusMode { OFF, HIGHLIGHT, ISOLATE }
enum class TerrainHeatmapSteps(val colorLevelCount: Int?) {
    PER_TILE(null),
    STEPS_32(32),
    STEPS_16(16),
    STEPS_8(8)
}

/** Activation and visual options for world-picking diagnostics. */
class DebugPickingSettings {
    /** Whether Picking diagnostics collect state and render configured visuals. */
    var enabled: Boolean = false

    var showSpriteBounds: Boolean = false
    var showCursorHit: Boolean = false
}

/** Diagnostics for calculated and actual render order. */
class DebugRenderOrderSettings {
    var mode: RenderOrderDebugMode = RenderOrderDebugMode.CALCULATED
    var showLabels: Boolean = false
    var showPriorityLabels: Boolean = false
    var colorByPriority: Boolean = false
    var priorityFocusMode: RenderPriorityFocusMode = RenderPriorityFocusMode.OFF
    var selectedPriority: Int = 0
    var showSortVolumes: Boolean = false
    var showSortAnchors: Boolean = false
    var showProjectedSortPositions: Boolean = false
    var showTerrainIndices: Boolean = false
    var showTerrainHeatmap: Boolean = false
    var terrainHeatmapSteps: TerrainHeatmapSteps = TerrainHeatmapSteps.PER_TILE

    var priorityColorAlpha: Float = 0.2f
        set(value) {
            require(value.isFinite() && value in 0f..1f) {
                "Render priority color alpha must be between zero and one."
            }
            field = value
        }

    var sortGeometryLineWidth: Float = 2f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Sort geometry line width must be finite and positive."
            }
            field = value
        }

    private var storedPriorityHighlightColor = Color(1f, 0.92f, 0.2f, 1f)
    var priorityHighlightColor: Color
        get() = storedPriorityHighlightColor.cpy()
        set(value) { storedPriorityHighlightColor = value.cpy() }

    private var storedSortVolumeColor = Color(0.25f, 0.9f, 1f, 1f)
    var sortVolumeColor: Color
        get() = storedSortVolumeColor.cpy()
        set(value) { storedSortVolumeColor = value.cpy() }

    private var storedSortBackAnchorColor = Color(0.35f, 1f, 0.4f, 1f)
    var sortBackAnchorColor: Color
        get() = storedSortBackAnchorColor.cpy()
        set(value) { storedSortBackAnchorColor = value.cpy() }

    private var storedSortFrontAnchorColor = Color(1f, 0.35f, 0.25f, 1f)
    var sortFrontAnchorColor: Color
        get() = storedSortFrontAnchorColor.cpy()
        set(value) { storedSortFrontAnchorColor = value.cpy() }

    private var storedProjectedSortPositionColor = Color(1f, 0.3f, 0.9f, 1f)
    var projectedSortPositionColor: Color
        get() = storedProjectedSortPositionColor.cpy()
        set(value) { storedProjectedSortPositionColor = value.cpy() }

    private var storedTerrainHeatmapStartColor = Color(0.1f, 0.65f, 1f, 0.55f)
    var terrainHeatmapStartColor: Color
        get() = storedTerrainHeatmapStartColor.cpy()
        set(value) { storedTerrainHeatmapStartColor = value.cpy() }

    private var storedTerrainHeatmapEndColor = Color(1f, 0.2f, 0.25f, 0.55f)
    var terrainHeatmapEndColor: Color
        get() = storedTerrainHeatmapEndColor.cpy()
        set(value) { storedTerrainHeatmapEndColor = value.cpy() }
}

/** Diagnostics for renderer culling decisions. */
class DebugCullingSettings {
    var showVisibleArea: Boolean = false
    var showObjectBounds: Boolean = false
    var showEntityBounds: Boolean = false

    private var storedVisibleAreaColor = Color(0.72f, 0.35f, 1f, 1f)
    var visibleAreaColor: Color
        get() = storedVisibleAreaColor.cpy()
        set(value) { storedVisibleAreaColor = value.cpy() }

    private var storedObjectDrawnColor = Color(0.15f, 0.85f, 1f, 1f)
    var objectDrawnColor: Color
        get() = storedObjectDrawnColor.cpy()
        set(value) { storedObjectDrawnColor = value.cpy() }

    private var storedObjectCulledColor = Color(0.08f, 0.32f, 0.42f, 0.8f)
    var objectCulledColor: Color
        get() = storedObjectCulledColor.cpy()
        set(value) { storedObjectCulledColor = value.cpy() }

    private var storedEntityDrawnColor = Color(1f, 0.68f, 0.15f, 1f)
    var entityDrawnColor: Color
        get() = storedEntityDrawnColor.cpy()
        set(value) { storedEntityDrawnColor = value.cpy() }

    private var storedEntityCulledColor = Color(0.48f, 0.28f, 0.06f, 0.8f)
    var entityCulledColor: Color
        get() = storedEntityCulledColor.cpy()
        set(value) { storedEntityCulledColor = value.cpy() }
}

/** Camera-bound visual diagnostics. */
class DebugCameraSettings {
    var showVisibleArea: Boolean = false
    var showWorldBounds: Boolean = false
    var showClampBounds: Boolean = false
}

internal val DebugPickingSettings.hasConfiguredVisuals: Boolean
    get() = showSpriteBounds || showCursorHit

internal val DebugPickingSettings.hasActiveVisuals: Boolean
    get() = enabled && hasConfiguredVisuals

internal val DebugRenderOrderSettings.hasActiveVisuals: Boolean
    get() = showLabels || showPriorityLabels || colorByPriority ||
        priorityFocusMode != RenderPriorityFocusMode.OFF || showSortVolumes ||
        showSortAnchors || showProjectedSortPositions || showTerrainIndices ||
        showTerrainHeatmap

internal val DebugCullingSettings.hasActiveVisuals: Boolean
    get() = showVisibleArea || showObjectBounds || showEntityBounds

internal val DebugCameraSettings.hasActiveVisuals: Boolean
    get() = showVisibleArea || showWorldBounds || showClampBounds
