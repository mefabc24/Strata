package com.mefabc24.strata.render

/**
 * Collects rendering statistics for the most recent frame.
 */
class RenderStats {

    var terrainChecked = 0
        internal set

    var terrainDrawn = 0
        internal set

    var objectsChecked = 0
        internal set

    var objectsDrawn = 0
        internal set

    var previewsDrawn = 0
        internal set

    var drawCalls = 0
        internal set

    var cpuRenderMs = 0.0
        internal set

    internal fun reset() {
        terrainChecked = 0
        terrainDrawn = 0
        objectsChecked = 0
        objectsDrawn = 0
        previewsDrawn = 0
        drawCalls = 0
        cpuRenderMs = 0.0
    }
}