package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.render.entity.EntityVisual
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

    fun diagnose(screenX: Float, screenY: Float): SpritePickDiagnostic<WorldEntity> {
        cursor.set(screenX, screenY, 0f)
        camera.unproject(cursor)
        var tested: WorldEntity? = null
        var testedBounds: Rectangle? = null
        var alphaAccepted: Boolean? = null
        val picked = pickWorld(cursor.x, cursor.y) { item, itemBounds, accepted ->
            if (tested == null) {
                tested = item
                testedBounds = Rectangle(itemBounds)
                alphaAccepted = accepted
            }
        }
        return SpritePickDiagnostic(picked, tested, testedBounds, alphaAccepted)
    }

    internal fun pickWorld(
        worldX: Float,
        worldY: Float,
        onSpriteTest: ((WorldEntity, Rectangle, Boolean?) -> Unit)? = null
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
            alphaMaskFor = { visual: ResolvedEntityVisual ->
                visual.frame.alphaMask
            },
            onSpriteTest = onSpriteTest
        )
    }
}
