package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.render.`object`.IsoObjectBounds
import com.mefabc24.strata.render.`object`.IsoObjectOrdering
import com.mefabc24.strata.render.`object`.ObjectRenderingSettings
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.render.`object`.ResolvedObjectVisual
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
    private val animationTime: () -> Float = { 0f },
    private val orderedObjects: (() -> List<PlacedObject>)? = null,
    objectSettings: ObjectRenderingSettings = ObjectRenderingSettings(),
    private val resolvedVisualFor: (
        (PlacedObject, Float) -> ResolvedObjectVisual?
    )? = null
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

    fun diagnose(screenX: Float, screenY: Float): SpritePickDiagnostic<PlacedObject> {
        cursor.set(screenX, screenY, 0f)
        camera.unproject(cursor)
        var tested: PlacedObject? = null
        var testedBounds: Rectangle? = null
        var alphaAccepted: Boolean? = null
        var pickedBounds: Rectangle? = null
        val picked = pickWorld(cursor.x, cursor.y) { item, itemBounds, accepted ->
            if (tested == null) {
                tested = item
                testedBounds = Rectangle(itemBounds)
                alphaAccepted = accepted
            }
            if (accepted != false) pickedBounds = Rectangle(itemBounds)
        }
        return SpritePickDiagnostic(
            picked, tested, testedBounds, alphaAccepted, pickedBounds
        )
    }

    internal fun pickWorld(
        worldX: Float,
        worldY: Float,
        onSpriteTest: ((PlacedObject, Rectangle, Boolean?) -> Unit)? = null
    ): PlacedObject? {
        val currentAnimationTime = animationTime()
        val objects = orderedObjects?.invoke()
            ?: IsoObjectOrdering.backToFront(
                objects = world.getObjects(),
                projection = projection
            )

        return pickFrontmost(
            items = objects,
            worldX = worldX,
            worldY = worldY,
            bounds = bounds,
            visualFor = { placed ->
                resolvedVisualFor?.invoke(placed, currentAnimationTime)
                    ?: visualFor(placed)?.let {
                        ResolvedObjectVisual(it, currentAnimationTime, null)
                    }
            },
            calculateBounds = { placed, visual, result ->
                IsoObjectBounds.calculate(
                    projection = projection,
                    placed = placed,
                    visual = visual,
                    result = result,
                    objectSettings = objectSettings
                )
            },
            alphaMaskFor = { visual: ResolvedObjectVisual ->
                visual.frame.alphaMask
            },
            onSpriteTest = onSpriteTest
        )
    }
}
