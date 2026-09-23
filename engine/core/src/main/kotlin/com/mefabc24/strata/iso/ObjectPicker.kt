package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.render.IsoObjectBounds
import com.mefabc24.strata.render.ObjectVisual
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World

/**
 * Finds placed objects by their rectangular sprite bounds.
 */
class ObjectPicker(
    private val camera: OrthographicCamera,
    private val projection: IsoProjection,
    private val world: World,
    private val visualFor: (PlacedObject) -> ObjectVisual?
) {
    private val cursor = Vector3()
    private val bounds = Rectangle()

    fun pick(screenX: Float, screenY: Float): PlacedObject? {
        cursor.set(screenX, screenY, 0f)
        camera.unproject(cursor)

        val objectsByDepth = world.getObjects().groupBy { placed ->
            placed.occupiedTiles().maxOf { (x, y) -> x + y }
        }

        for (depth in objectsByDepth.keys.sortedDescending()) {
            val objects = objectsByDepth.getValue(depth)

            for (placed in objects.asReversed()) {
                val visual = visualFor(placed) ?: continue

                val elevation = world.getHeight(
                    placed.x,
                    placed.y
                ) ?: 0

                IsoObjectBounds.calculate(
                    projection = projection,
                    placed = placed,
                    visual = visual,
                    result = bounds,
                    elevation = elevation
                )

                if (!bounds.contains(cursor.x, cursor.y)) {
                    continue
                }

                val u = (cursor.x - bounds.x) / bounds.width
                val v = (cursor.y - bounds.y) / bounds.height

                val alphaMask = visual.alphaMask

                if (alphaMask == null || alphaMask.isSolid(u, v)) {
                    return placed
                }
            }
        }

        return null
    }
}