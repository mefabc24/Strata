package com.mefabc24.strata.render

/**
 * Collects rendering statistics for the most recent frame.
 */
class RenderStats {

    var terrainChecked = 0
        internal set

    var terrainDrawn = 0
        internal set

    /** Number of ground terrain sprites drawn during the frame. */
    var groundTerrainDrawn = 0
        internal set

    /** Number of overlay terrain sprites drawn during the frame. */
    var overlayTerrainDrawn = 0
        internal set

    var objectsChecked = 0
        internal set

    var objectsDrawn = 0
        internal set

    var entitiesChecked = 0
        internal set

    var entitiesDrawn = 0
        internal set

    var previewsDrawn = 0
        internal set

    var drawCalls = 0
        internal set

    var cpuRenderMs = 0.0
        internal set

    var staticPlanMs = 0.0
        internal set

    var staticPlanUpdates = 0
        internal set

    var dynamicPlanMs = 0.0
        internal set

    var staticPlanRelationChecks = 0
        internal set

    internal fun reset() {
        terrainChecked = 0
        terrainDrawn = 0
        groundTerrainDrawn = 0
        overlayTerrainDrawn = 0
        objectsChecked = 0
        objectsDrawn = 0
        entitiesChecked = 0
        entitiesDrawn = 0
        previewsDrawn = 0
        drawCalls = 0
        cpuRenderMs = 0.0
        staticPlanMs = 0.0
        staticPlanUpdates = 0
        dynamicPlanMs = 0.0
        staticPlanRelationChecks = 0
    }
}
