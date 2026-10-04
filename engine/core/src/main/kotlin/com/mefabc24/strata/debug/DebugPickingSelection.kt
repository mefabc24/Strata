package com.mefabc24.strata.debug

import com.mefabc24.strata.iso.PickedTarget

enum class DebugPickingTargetMode { HOVER, LOCKED }

/** Keeps a click-locked picking target separate from transient hover state. */
internal class DebugPickingSelection {
    var lockedTarget: PickedTarget? = null
        private set

    val isLocked: Boolean
        get() = lockedTarget != null

    fun selectFromClick(target: PickedTarget?): Boolean {
        if (target == null) return false
        lockedTarget = target
        return true
    }

    fun refresh(transform: (PickedTarget) -> PickedTarget) {
        lockedTarget = lockedTarget?.let(transform)
    }

    fun clear(): Boolean {
        if (lockedTarget == null) return false
        lockedTarget = null
        return true
    }

    fun syncEnabled(enabled: Boolean) {
        if (!enabled) clear()
    }

    fun displayedTarget(hoverTarget: PickedTarget?): PickedTarget? =
        lockedTarget ?: hoverTarget

    fun mode(): DebugPickingTargetMode = when {
        lockedTarget != null -> DebugPickingTargetMode.LOCKED
        else -> DebugPickingTargetMode.HOVER
    }
}
