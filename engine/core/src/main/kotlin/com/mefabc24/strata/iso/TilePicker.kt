package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.math.ceil

class TilePicker(
    private val camera: OrthographicCamera,
    private val projection: IsoProjection,
    private val world: World
) {
    private val position = Vector3()
    private val candidateRadius = ceil(
        projection.logicalTileHeight / projection.tileHeight
    ).toInt()

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
        var picked: TilePosition? = null
        var pickedDepth = Int.MIN_VALUE
        var pickedX = Int.MIN_VALUE

        for (elevation in 0..world.maxHeight) {
            val planeTile = projection.worldToTile(
                worldX = worldX,
                worldY = worldY -
                        elevation * projection.elevationStep
            )

            for (x in planeTile.x - candidateRadius..planeTile.x) {
                for (y in planeTile.y - candidateRadius..planeTile.y) {
                    if (world.getHeight(x, y) != elevation) continue
                    if (world.getTile(x, y) == null) continue
                    if (!projection.containsLogicalTile(
                            worldX = worldX,
                            worldY = worldY,
                            x = x,
                            y = y,
                            elevation = elevation
                        )
                    ) {
                        continue
                    }

                    val depth = x + y

                    if (
                        depth > pickedDepth ||
                        (depth == pickedDepth && x > pickedX)
                    ) {
                        picked = TilePosition(x, y)
                        pickedDepth = depth
                        pickedX = x
                    }
                }
            }
        }

        return picked
    }
}
