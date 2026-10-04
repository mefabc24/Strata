package com.mefabc24.strata.render.debug

import com.mefabc24.strata.iso.PickedTarget

/** Independent transient hover and persistent lock targets for world feedback. */
internal data class DebugPickingVisualTargets(
    val hover: PickedTarget?,
    val locked: PickedTarget?
)

internal fun pickingVisualTargets(
    enabled: Boolean,
    hover: PickedTarget?,
    locked: PickedTarget?
): DebugPickingVisualTargets = if (enabled) {
    DebugPickingVisualTargets(hover, locked)
} else {
    DebugPickingVisualTargets(null, null)
}
