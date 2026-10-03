package com.mefabc24.strata.debug.ui

internal object DebugWindowLayout {
    const val DEBUG_WIDTH = 372f
    const val TOOLS_MARGIN = 12f
    const val OVERLAY_GAP = 8f
    const val OVERLAY_MARGIN = 16f

    fun debugWidth(availableWidth: Float): Float {
        require(availableWidth.isFinite() && availableWidth >= 0f)
        return minOf(DEBUG_WIDTH, availableWidth)
    }

    fun overlayTopPadding(toolsVisible: Boolean, toolsHeight: Float): Float {
        require(toolsHeight.isFinite() && toolsHeight >= 0f)
        return if (toolsVisible) {
            TOOLS_MARGIN + toolsHeight + OVERLAY_GAP
        } else {
            OVERLAY_MARGIN
        }
    }
}
