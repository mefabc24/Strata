package com.mefabc24.strata.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.mefabc24.strata.iso.TilePicker

/**
 * Converts mouse clicks into tile click callbacks.
 */
class TileInputProcessor(
    private val tilePicker: TilePicker,
    private val onLeftClick: ((x: Int, y: Int) -> Boolean)? = null,
    private val onRightClick: ((x: Int, y: Int) -> Boolean)? = null
) : InputAdapter() {

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        val callback = when (button) {
            Input.Buttons.LEFT -> onLeftClick
            Input.Buttons.RIGHT -> onRightClick
            else -> null
        } ?: return false

        val tile = tilePicker.pick(
            screenX.toFloat(),
            screenY.toFloat()
        ) ?: return false

        return callback(tile.first, tile.second)
    }
}