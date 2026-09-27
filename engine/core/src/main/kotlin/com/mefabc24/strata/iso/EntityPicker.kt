package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.entity.alphaMask
import com.mefabc24.strata.render.entity.ResolvedEntityVisual
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
    private val animationTime: () -> Float = { 0f },
    private val orderedEntities: (() -> List<WorldEntity>)? = null,
    private val resolvedVisualFor: (
        (WorldEntity, Float) -> ResolvedEntityVisual?
    )? = null
) {
    private val cursor = Vector3()
    private val bounds = Rectangle()

    fun pick(screenX: Float, screenY: Float): WorldEntity? {
        cursor.set(screenX, screenY, 0f)
        camera.unproject(cursor)
        return pickWorld(cursor.x, cursor.y)
    }

    internal fun pickWorld(
        worldX: Float,
        worldY: Float
    ): WorldEntity? {
        val currentAnimationTime = animationTime()
        val entities = orderedEntities?.invoke()
            ?: WorldRenderPlan.create(
                world = world,
                projection = projection
            ).mapNotNull { primitive ->
                (primitive as? WorldEntityPrimitive)?.worldEntity
            }

        return pickFrontmost(
            items = entities,
            worldX = worldX,
            worldY = worldY,
            bounds = bounds,
            visualFor = { entity ->
                resolvedVisualFor?.invoke(entity, currentAnimationTime)
                    ?: visualFor(entity)?.let {
                        ResolvedEntityVisual(it, currentAnimationTime, null)
                    }
            },
            calculateBounds = { entity, visual, result ->
                IsoEntityBounds.calculate(
                    projection = projection,
                    entity = entity,
                    visual = visual,
                    result = result
                )
            },
            alphaMaskFor = { visual ->
                visual.frame.alphaMask
            }
        )
    }
}
