package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.entity.IsoEntityBounds
import com.mefabc24.strata.render.order.WorldEntityPrimitive
import com.mefabc24.strata.render.order.WorldRenderPlan
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

/** Finds the frontmost entity through its current sprite-frame alpha mask. */
class EntityPicker(
    private val camera: OrthographicCamera,
    private val projection: IsoProjection,
    private val world: World,
    private val visualFor: (WorldEntity) -> EntityVisual?,
    private val animationTime: () -> Float = { 0f }
) {
    private val cursor = Vector3()
    private val bounds = Rectangle()

    fun pick(screenX: Float, screenY: Float): WorldEntity? {
        cursor.set(screenX, screenY, 0f)
        camera.unproject(cursor)
        return pickWorld(cursor.x, cursor.y)
    }

    internal fun pickWorld(worldX: Float, worldY: Float): WorldEntity? {
        val plan = WorldRenderPlan.create(world, projection)

        for (primitive in plan.asReversed()) {
            if (primitive !is WorldEntityPrimitive) continue

            val entity = primitive.worldEntity
            val visual = visualFor(entity) ?: continue
            IsoEntityBounds.calculate(
                projection = projection,
                entity = entity,
                visual = visual,
                result = bounds
            )
            if (!bounds.contains(worldX, worldY)) continue

            val u = (worldX - bounds.x) / bounds.width
            val v = (worldY - bounds.y) / bounds.height
            val mask = visual.frameAt(animationTime()).alphaMask

            if (mask == null || mask.isSolid(u, v)) return entity
        }

        return null
    }
}
