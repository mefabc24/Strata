package com.mefabc24.strata.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition

/**
 * Resolves input bindings and dispatches picked targets to the game.
 */
class WorldInputProcessor(
    private val bindings: List<WorldInputBinding>,
    private val pickTile: (Float, Float) -> TilePosition?,
    private val pickObject: (
        Float,
        Float,
        ObjectPickingMode
    ) -> PlacedObject?
) : InputAdapter() {

    private val pressedButtons = mutableMapOf<Int, MutableSet<Int>>()

    var enabled: Boolean = true
        set(value) {
            field = value

            if (!value) {
                pressedButtons.clear()
            }
        }

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        if (!enabled) return false

        pressedButtons
            .getOrPut(pointer) { mutableSetOf() }
            .add(button)

        return dispatch(
            trigger = WorldInputTrigger.MouseDown(button),
            screenX = screenX.toFloat(),
            screenY = screenY.toFloat()
        )
    }

    override fun touchDragged(
        screenX: Int,
        screenY: Int,
        pointer: Int
    ): Boolean {
        if (!enabled) return false

        val buttons = pressedButtons[pointer] ?: return false

        for (button in buttons.toList()) {
            val handled = dispatch(
                trigger = WorldInputTrigger.MouseDrag(button),
                screenX = screenX.toFloat(),
                screenY = screenY.toFloat()
            )

            if (handled) return true
        }

        return false
    }

    override fun touchUp(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        val wasPressed = pressedButtons[pointer]?.remove(button) == true

        if (pressedButtons[pointer]?.isEmpty() == true) {
            pressedButtons.remove(pointer)
        }

        if (!enabled || !wasPressed) {
            return false
        }

        return dispatch(
            trigger = WorldInputTrigger.MouseUp(button),
            screenX = screenX.toFloat(),
            screenY = screenY.toFloat()
        )
    }

    override fun keyDown(keycode: Int): Boolean {
        if (!enabled) return false

        return dispatch(
            trigger = WorldInputTrigger.KeyDown(keycode),
            screenX = Gdx.input.x.toFloat(),
            screenY = Gdx.input.y.toFloat()
        )
    }

    private fun dispatch(
        trigger: WorldInputTrigger,
        screenX: Float,
        screenY: Float
    ): Boolean {
        for (binding in bindings) {
            if (binding.trigger != trigger) continue
            if (!binding.enabled()) continue

            val handled = when (binding) {
                is WorldInputBinding.Tile -> {
                    val tile = pickTile(screenX, screenY)

                    if (tile != null) {
                        binding.action(tile.x, tile.y)
                    } else {
                        false
                    }
                }

                is WorldInputBinding.Object -> {
                    val placed = pickObject(
                        screenX,
                        screenY,
                        binding.mode
                    )

                    if (placed != null) {
                        binding.action(placed)
                    } else {
                        false
                    }
                }

                is WorldInputBinding.NoPicking -> {
                    binding.action()
                }
            }

            if (handled) return true
        }

        return false
    }
}