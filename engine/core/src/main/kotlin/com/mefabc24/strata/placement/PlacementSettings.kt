package com.mefabc24.strata.placement

import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

typealias PlacementValidator = (
    placeable: Placeable,
    position: TilePosition
) -> Boolean

enum class PlacementPreviewBoundsPolicy {

    /**
     * Shows a preview only when every occupied footprint tile is inside
     * the world.
     */
    ALL_TILES_INSIDE,

    /**
     * Shows a preview when the placement origin is inside the world.
     */
    ORIGIN_INSIDE,

    /**
     * Shows a preview when at least one occupied footprint tile is inside
     * the world.
     */
    ANY_TILE_INSIDE,

    /**
     * Always shows the preview, even when the complete footprint is outside
     * the world.
     */
    ALWAYS
}

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
     * Controls when placement previews are visible near or outside
     * the world bounds.
     */
    var previewBoundsPolicy: PlacementPreviewBoundsPolicy =
        PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE

    private var placementValidator: PlacementValidator = { _, _ -> true }

    fun validator(validate: PlacementValidator) {
        placementValidator = validate
    }

    internal fun createController(world: World): PlacementController {
        return PlacementController(
            world = world,
            style = previewStyle.snapshot(),
            previewBoundsPolicy = previewBoundsPolicy,
            placementValidator = placementValidator
        )
    }

    internal fun copy(): PlacementSettings {
        return PlacementSettings().also { copy ->
            copy.previewStyle = previewStyle
            copy.previewBoundsPolicy = previewBoundsPolicy
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
