package com.mefabc24.strata.input

/**
 * Configures input bindings for game world actions.
 */
class GameplayControlsSettings {
    var bindings: List<WorldInputBinding> = emptyList()
        set(value) {
            field = value.toList()
        }

    internal fun copy(): GameplayControlsSettings {
        return GameplayControlsSettings().also {
            it.bindings = bindings.toList()
        }
    }
}
