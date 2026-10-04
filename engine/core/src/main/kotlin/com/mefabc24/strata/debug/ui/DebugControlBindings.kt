package com.mefabc24.strata.debug.ui

/** Runs lightweight view bindings against authoritative runtime state. */
internal class DebugControlBindings {
    private val bindings = mutableListOf<() -> Unit>()

    operator fun plusAssign(binding: () -> Unit) {
        bindings += binding
    }

    fun sync() {
        bindings.forEach { it() }
    }
}
