package com.mefabc24.strata.debug

import com.badlogic.gdx.graphics.Color

enum class DebugGridRenderLayer { BELOW_OBJECTS, ABOVE_OBJECTS }
enum class DebugGridExtent { WORLD, VISIBLE }

/** Information drawn directly over visible world tiles. */
class DebugWorldInfoSettings {
    var showTileCoordinates: Boolean = false
    var showTerrainIds: Boolean = false
    var showOverlayInfo: Boolean = false
    var showOccupancy: Boolean = false
    var showMissingTerrainVisuals: Boolean = false
    var showOrigin: Boolean = false

    /** Labels are omitted beyond this camera zoom to keep them readable. */
    var maximumLabelZoom: Float = 1.5f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "World information maximum label zoom must be finite and positive."
            }
            field = value
        }

    /** Upper bound used to sample labels across large visible tile ranges. */
    var maximumVisibleLabels: Int = 256
        set(value) {
            require(value in 1..4096) {
                "Maximum visible world labels must be between 1 and 4096."
            }
            field = value
        }

    private var storedLabelColor = Color.WHITE.cpy()
    var labelColor: Color
        get() = storedLabelColor.cpy()
        set(value) { storedLabelColor = value.cpy() }

    private var storedOccupancyColor = Color(1f, 0.35f, 0.15f, 0.28f)
    var occupancyColor: Color
        get() = storedOccupancyColor.cpy()
        set(value) { storedOccupancyColor = value.cpy() }

    private var storedMissingVisualColor = Color(1f, 0.1f, 0.65f, 0.42f)
    var missingVisualColor: Color
        get() = storedMissingVisualColor.cpy()
        set(value) { storedMissingVisualColor = value.cpy() }

    private var storedOriginColor = Color(0.2f, 1f, 0.85f, 1f)
    var originColor: Color
        get() = storedOriginColor.cpy()
        set(value) { storedOriginColor = value.cpy() }
}

/** Debug-only category visibility. These flags never alter world contents. */
class DebugWorldVisibilitySettings {
    var groundTerrainVisible: Boolean = true
    var terrainOverlaysVisible: Boolean = true
    var placedObjectsVisible: Boolean = true
    var entitiesVisible: Boolean = true

    private val hiddenOverlayLayers = mutableSetOf<String>()

    fun isOverlayLayerVisible(layerId: String): Boolean = layerId !in hiddenOverlayLayers

    fun setOverlayLayerVisible(layerId: String, visible: Boolean) {
        require(layerId.isNotBlank()) { "Overlay layer ID must not be blank." }
        if (visible) hiddenOverlayLayers.remove(layerId) else hiddenOverlayLayers.add(layerId)
    }

    internal fun hiddenOverlayLayerIds(): Set<String> = hiddenOverlayLayers.toSet()

    internal fun restoreHiddenOverlayLayers(layerIds: Set<String>) {
        hiddenOverlayLayers.clear()
        hiddenOverlayLayers.addAll(layerIds)
    }

    /** Restores the normal renderer view for every category and overlay layer. */
    fun showAll() {
        groundTerrainVisible = true
        terrainOverlaysVisible = true
        placedObjectsVisible = true
        entitiesVisible = true
        hiddenOverlayLayers.clear()
    }
}

/** Runtime configuration for the isometric world-grid overlay. */
class DebugGridSettings : DebugFeatureSettings() {
    var renderLayer: DebugGridRenderLayer = DebugGridRenderLayer.BELOW_OBJECTS
    var extent: DebugGridExtent = DebugGridExtent.WORLD

    private var storedColor = Color(0.4f, 0.8f, 0.5f, 1f)
    var color: Color
        get() = storedColor.cpy()
        set(value) { storedColor = value.cpy() }

    private var storedHoverColor = Color(1f, 0.85f, 0.2f, 1f)
    var hoverColor: Color
        get() = storedHoverColor.cpy()
        set(value) { storedHoverColor = value.cpy() }

    var lineWidth: Float = 1f
        set(value) {
            require(value.isFinite() && value > 0f) {
                "Debug grid line width must be finite and positive."
            }
            field = value
        }

    /** Whether regular grid tiles receive a background fill. */
    var showBackground: Boolean = false

    /** Whether the hovered tile receives a background fill. */
    var showHoverBackground: Boolean = false

    private var storedBackgroundColor = Color(1f, 1f, 1f, 0.2f)

    var backgroundColor: Color
        get() = storedBackgroundColor.cpy()
        set(value) { storedBackgroundColor = value.cpy() }

    private var storedHoverBackgroundColor = Color(1f, 0f, 0f, 0.5f)

    var hoverBackgroundColor: Color
        get() = storedHoverBackgroundColor.cpy()
        set(value) { storedHoverBackgroundColor = value.cpy() }

    internal fun copy(): DebugGridSettings = DebugGridSettings().also {
        it.enabled = enabled
        it.renderLayer = renderLayer
        it.extent = extent
        it.color = color
        it.hoverColor = hoverColor
        it.lineWidth = lineWidth
        it.showBackground = showBackground
        it.showHoverBackground = showHoverBackground
        it.backgroundColor = backgroundColor
        it.hoverBackgroundColor = hoverBackgroundColor
    }
}

internal val DebugWorldInfoSettings.hasActiveVisuals: Boolean
    get() = showTileCoordinates || showTerrainIds || showOverlayInfo ||
        showOccupancy || showMissingTerrainVisuals || showOrigin
