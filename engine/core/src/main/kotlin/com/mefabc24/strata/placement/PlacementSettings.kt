package com.mefabc24.strata.placement

import com.mefabc24.strata.render.PlacementPreviewStyle
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

    private var placementValidator: PlacementValidator = { _, _ -> true }

    fun validator(validate: PlacementValidator) {
        placementValidator = validate
    }

    internal fun createController(world: World): PlacementController {
        val styleSnapshot = PlacementPreviewStyle(
            validColor = previewStyle.validColor.cpy(),
            invalidColor = previewStyle.invalidColor.cpy()
        )

        return PlacementController(
            world = world,
            style = styleSnapshot,
            placementValidator = placementValidator
        )
    }
}
