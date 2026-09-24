package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.render.IsoObjectBounds
import com.mefabc24.strata.render.IsoObjectOrdering
import com.mefabc24.strata.render.ObjectRenderingSettings
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
    private val visualFor: (PlacedObject) -> ObjectVisual?,
    objectSettings: ObjectRenderingSettings = ObjectRenderingSettings()
) {
    private val objectSettings = objectSettings.copy().also {
        it.validate()
    }

    private val cursor = Vector3()
    private val bounds = Rectangle()

    fun pick(screenX: Float, screenY: Float): PlacedObject? {
        cursor.set(screenX, screenY, 0f)
        camera.unproject(cursor)

        return pickWorld(cursor.x, cursor.y)
    }

    internal fun pickWorld(
        worldX: Float,
        worldY: Float
    ): PlacedObject? {
        val orderedObjects = IsoObjectOrdering.backToFront(
            objects = world.getObjects(),
            projection = projection,
            elevationFor = { placed ->
                world.getHeight(placed.x, placed.y) ?: 0
            }
        )

        for (placed in orderedObjects.asReversed()) {
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
                elevation = elevation,
                objectSettings = objectSettings
            )

            if (!bounds.contains(worldX, worldY)) {
                continue
            }

            val u = (worldX - bounds.x) / bounds.width
            val v = (worldY - bounds.y) / bounds.height

            val alphaMask = visual.alphaMask

            if (alphaMask == null || alphaMask.isSolid(u, v)) {
                return placed
            }
        }

        return null
    }
}
