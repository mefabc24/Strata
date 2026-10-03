package com.mefabc24.strata.debug.ui

internal object DebugWindowLayout {
    const val DEBUG_WIDTH = 372f
    const val TOOL_RAIL_WIDTH = 68f
    const val TOOL_BUTTON_HEIGHT = 56f
    const val TOOL_FLYOUT_WIDTH = 332f
    const val OVERLAY_GAP = 8f
    const val OVERLAY_MARGIN = 16f

    fun debugWidth(availableWidth: Float): Float {
        require(availableWidth.isFinite() && availableWidth >= 0f)
        return minOf(DEBUG_WIDTH, availableWidth)
    }

    fun overlayLeftPadding(
        toolsVisible: Boolean,
        flyoutVisible: Boolean,
        flyoutWidth: Float = TOOL_FLYOUT_WIDTH
    ): Float {
        require(flyoutWidth.isFinite() && flyoutWidth >= 0f)
        if (!toolsVisible) return OVERLAY_MARGIN
        return TOOL_RAIL_WIDTH + OVERLAY_GAP +
            if (flyoutVisible) flyoutWidth + OVERLAY_GAP else 0f
    }
}
