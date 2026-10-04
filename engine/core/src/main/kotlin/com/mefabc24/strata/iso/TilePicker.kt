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

    /** Returns the logical grid position at the given screen position. */
    fun pickGrid(screenX: Float, screenY: Float): TilePosition {
        position.set(screenX, screenY, 0f)
        camera.unproject(position)

        return pickGridWorld(position.x, position.y)
    }

    /** Returns the world tile at the given screen position, or null. */
    fun pick(screenX: Float, screenY: Float): TilePosition? {
        return pickGrid(screenX, screenY).takeIf { (x, y) ->
            world.getTile(x, y) != null
        }
    }

    internal fun pickGridWorld(
        worldX: Float,
        worldY: Float
    ): TilePosition {
        val tile = projection.worldToTile(worldX, worldY)

        check(
            projection.containsTopFace(
                worldX,
                worldY,
                tile.x,
                tile.y
            )
        ) {
            "Inverse isometric projection did not resolve a top face."
        }

        return tile
    }

    internal fun pickWorld(
        worldX: Float,
        worldY: Float
    ): TilePosition? {
        return pickGridWorld(worldX, worldY).takeIf { (x, y) ->
            world.getTile(x, y) != null
        }
    }
}
