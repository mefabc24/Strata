package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

class TilePicker(
    private val camera: OrthographicCamera,
    private val projection: IsoProjection,
    private val world: World
) {
    private val position = Vector3()

    fun pick(
        screenX: Float,
        screenY: Float
    ): TilePosition? {
        position.set(screenX, screenY, 0f)
        camera.unproject(position)

        return pickWorld(
            worldX = position.x,
            worldY = position.y
        )
    }

    internal fun pickWorld(
        worldX: Float,
        worldY: Float
    ): TilePosition? {
        for (elevation in world.maxHeight downTo 0) {
            val (x, y) = projection.worldToTile(
                worldX = worldX,
                worldY = worldY -
                        elevation * projection.elevationStep
            )

            if (
                world.getTile(x, y) != null &&
                world.getHeight(x, y) == elevation
            ) {
                return TilePosition(x, y)
            }
        }

        return null
    }
}