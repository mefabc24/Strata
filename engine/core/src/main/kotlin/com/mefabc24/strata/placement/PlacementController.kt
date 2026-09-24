package com.mefabc24.strata.placement

import com.mefabc24.strata.render.PlacementPreview
import com.mefabc24.strata.render.PlacementPreviewStyle
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

/**
 * Manages object placement and its preview.
 *
 * Geometric placement is validated by the world.
 * Additional game-specific rules can be supplied through
 * the placement validator.
 */
class PlacementController(
    private val world: World,
    private val style: PlacementPreviewStyle = PlacementPreviewStyle.DEFAULT,
    private val placementValidator: (
        placeable: Placeable,
        position: TilePosition
    ) -> Boolean = { _, _ -> true }
) {
    var selectedPlaceable: Placeable? = null

    var preview: PlacementPreview? = null
        private set

    /**
     * Updates the preview for the currently hovered tile.
     */
    fun update(hoveredTile: TilePosition?) {
        val placeable = selectedPlaceable

        preview = if (
            hoveredTile != null &&
            placeable != null
        ) {
            PlacementPreview(
                placedObject = PlacedObject(
                    placeable = placeable,
                    x = hoveredTile.x,
                    y = hoveredTile.y
                ),
                valid = canPlace(
                    placeable = placeable,
                    position = hoveredTile
                ),
                style = style
            )
        } else {
            null
        }
    }

    /**
     * Places the selected object at the given position.
     *
     * Returns the placed object on success, or null otherwise.
     */
    fun placeAt(
        position: TilePosition
    ): PlacedObject? {
        val placeable =
            selectedPlaceable ?: return null

        if (
            !canPlace(
                placeable = placeable,
                position = position
            )
        ) {
            return null
        }

        return world.place(
            placeable = placeable,
            position = position
        )
    }

    /**
     * Places the selected object at the given coordinates.
     */
    fun placeAt(
        x: Int,
        y: Int
    ): PlacedObject? {
        return placeAt(
            TilePosition(
                x = x,
                y = y
            )
        )
    }

    private fun canPlace(
        placeable: Placeable,
        position: TilePosition
    ): Boolean {
        return world.canPlace(
            placeable = placeable,
            position = position
        ) && placementValidator(
            placeable,
            position
        )
    }
}