package com.mefabc24.strata.input

import com.badlogic.gdx.InputAdapter
import com.mefabc24.strata.iso.TilePicker

/**
 * Converts mouse clicks into tile click callbacks.
 */
class TileInputProcessor(
    private val tilePicker: TilePicker,
    private val onTileClick: (x: Int, y: Int, button: Int) -> Boolean
) : InputAdapter() {

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        val tile = tilePicker.pick(
            screenX.toFloat(),
            screenY.toFloat()
        ) ?: return false

        return onTileClick(tile.first, tile.second, button)
    }
}