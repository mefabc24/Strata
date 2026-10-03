package com.mefabc24.strata.debug.ui

internal object DebugWindowLayout {
    const val DEBUG_WIDTH = 372f
    const val TOOLS_MARGIN = 12f
    const val OVERLAY_GAP = 8f

    fun debugWidth(availableWidth: Float): Float {
        require(availableWidth.isFinite() && availableWidth >= 0f)
        return minOf(DEBUG_WIDTH, availableWidth)
    }

    fun overlayRightInset(debugVisible: Boolean, debugWidth: Float): Float =
        if (debugVisible) debugWidth + OVERLAY_GAP else 0f
}
