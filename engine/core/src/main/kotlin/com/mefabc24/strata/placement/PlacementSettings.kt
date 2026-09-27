package com.mefabc24.strata.placement

import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

typealias PlacementValidator = (
    placeable: Placeable,
    position: TilePosition
) -> Boolean

/**
 * Configures the optional placement controller owned by a scene.
 *
 * Settings are captured when the world is attached. The validator supplements
 * the world's geometric placement rules and cannot override them.
 */
class PlacementSettings {
    var previewStyle: PlacementPreviewStyle = PlacementPreviewStyle.DEFAULT
        set(value) {
            field = value.snapshot()
        }

    /**
     * Whether placement previews may be shown outside the world bounds.
     */
    var showOutsideWorldPreviews: Boolean = false

    private var placementValidator: PlacementValidator = { _, _ -> true }

    fun validator(validate: PlacementValidator) {
        placementValidator = validate
    }

    internal fun createController(world: World): PlacementController {
        return PlacementController(
            world = world,
            style = previewStyle.snapshot(),
            showOutsideWorldPreviews = showOutsideWorldPreviews,
            placementValidator = placementValidator
        )
    }

    internal fun copy(): PlacementSettings {
        return PlacementSettings().also { copy ->
            copy.previewStyle = previewStyle
            copy.showOutsideWorldPreviews = showOutsideWorldPreviews
            copy.placementValidator = placementValidator
        }
    }
}

private fun PlacementPreviewStyle.snapshot(): PlacementPreviewStyle {
    return PlacementPreviewStyle(
        validColor = validColor.cpy(),
        invalidColor = invalidColor.cpy()
    )
}
