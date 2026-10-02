package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugSettings

internal enum class DebugWindowKind { TOOLS, DEBUG }

/** Returns visible windows in configured left-to-right order. */
internal fun orderedVisibleDebugWindows(settings: DebugSettings): List<DebugWindowKind> =
    buildList {
        if (settings.toolsWindow.enabled && settings.toolsWindow.visible) {
            add(settings.toolsWindow.order to DebugWindowKind.TOOLS)
        }
        if (settings.debugWindow.enabled && settings.debugWindow.visible) {
            add(settings.debugWindow.order to DebugWindowKind.DEBUG)
        }
    }.sortedWith(compareBy<Pair<Int, DebugWindowKind>> { it.first }.thenBy { it.second.ordinal })
        .map { it.second }
