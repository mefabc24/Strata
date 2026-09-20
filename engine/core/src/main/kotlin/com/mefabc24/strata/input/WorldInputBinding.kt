package com.mefabc24.strata.input

import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.world.PlacedObject

/**
 * Describes an input event that can trigger a binding.
 */
sealed interface WorldInputTrigger {

    data class MouseDown(
        val button: Int
    ) : WorldInputTrigger

    data class MouseDrag(
        val button: Int
    ) : WorldInputTrigger

    data class KeyDown(
        val key: Int
    ) : WorldInputTrigger
}

/**
 * Connects an input event to a game-defined action.
 *
 * Bindings are evaluated in their registration order.
 * Returning true consumes the input event.
 */
sealed interface WorldInputBinding {

    val trigger: WorldInputTrigger

    val enabled: () -> Boolean

    /**
     * Picks a ground tile before invoking the action.
     */
    class Tile(
        override val trigger: WorldInputTrigger,
        override val enabled: () -> Boolean = { true },
        val action: (x: Int, y: Int) -> Boolean
    ) : WorldInputBinding

    /**
     * Picks a placed object using the specified mode.
     */
    class Object(
        override val trigger: WorldInputTrigger,
        val mode: ObjectPickingMode = ObjectPickingMode.SPRITE_ALPHA,
        override val enabled: () -> Boolean = { true },
        val action: (PlacedObject) -> Boolean
    ) : WorldInputBinding

    /**
     * Invokes an action without performing any picking.
     */
    class NoPicking(
        override val trigger: WorldInputTrigger,
        override val enabled: () -> Boolean = { true },
        val action: () -> Boolean
    ) : WorldInputBinding
}