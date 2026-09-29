package com.mefabc24.strata.debug

import com.mefabc24.strata.iso.PickedSpriteTarget

enum class DebugPickingTargetMode { NONE, HOVER, LOCKED }

/** Keeps a click-locked picking target separate from transient hover state. */
internal class DebugPickingSelection {
    var lockedTarget: PickedSpriteTarget? = null
        private set

    val isLocked: Boolean
        get() = lockedTarget != null

    fun selectFromClick(target: PickedSpriteTarget?): Boolean {
        if (target == null) return false
        lockedTarget = target
        return true
    }

    fun refresh(transform: (PickedSpriteTarget) -> PickedSpriteTarget) {
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

    fun displayedTarget(hoverTarget: PickedSpriteTarget?): PickedSpriteTarget? =
        lockedTarget ?: hoverTarget

    fun mode(hoverTarget: PickedSpriteTarget?): DebugPickingTargetMode = when {
        lockedTarget != null -> DebugPickingTargetMode.LOCKED
        hoverTarget != null -> DebugPickingTargetMode.HOVER
        else -> DebugPickingTargetMode.NONE
    }
}
