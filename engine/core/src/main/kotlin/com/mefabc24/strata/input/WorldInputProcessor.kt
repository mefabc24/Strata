package com.mefabc24.strata.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.world.PlacedObject

/**
 * Resolves input bindings and dispatches picked targets to the game.
 */
class WorldInputProcessor(
    private val bindings: List<WorldInputBinding>,
    private val pickTile: (Float, Float) -> Pair<Int, Int>?,
    private val pickObject: (
        Float,
        Float,
        ObjectPickingMode
    ) -> PlacedObject?
) : InputAdapter() {

    var enabled: Boolean = true

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        if (!enabled) return false

        return dispatch(
            trigger = WorldInputTrigger.MouseDown(button),
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
                        binding.action(tile.first, tile.second)
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