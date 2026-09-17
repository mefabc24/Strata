package com.mefabc24.strata.iso

import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.world.World
import kotlin.math.floor

class IsoProjection(
    val tileWidth: Float = 64f,
    val tileHeight: Float = 32f,
) {
    init {
        require(tileWidth > 0f && tileWidth.isFinite())
        require(tileHeight > 0f && tileHeight.isFinite())
    }

    fun tileToWorld(x: Int, y: Int): Vector2 {
        return Vector2(
            (x - y) * tileWidth / 2f,
            -(x + y) * tileHeight / 2f
        )
    }

    fun worldToTile(worldX: Float, worldY: Float): Pair<Int, Int> {
        val x = worldX / tileWidth - worldY / tileHeight
        val y = -worldX / tileWidth - worldY / tileHeight

        return floor(x).toInt() to floor(y).toInt()
    }
}