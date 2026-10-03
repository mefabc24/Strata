package com.mefabc24.strata.debug.ui

internal object DebugWindowLayout {
    const val DEBUG_WIDTH = 372f
    const val TOOLS_MARGIN = 12f
    const val OVERLAY_GAP = 8f

    fun overlayRightInset(debugVisible: Boolean): Float =
        if (debugVisible) DEBUG_WIDTH + OVERLAY_GAP else 0f
}
