package com.mefabc24.strata.placement

import com.mefabc24.strata.render.PlacementPreview
import com.mefabc24.strata.render.PlacementPreviewStyle
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World

/**
 * Manages geometric object placement and its preview.
 */
class PlacementController(
    private val world: World,
    private val style: PlacementPreviewStyle = PlacementPreviewStyle.DEFAULT
) {
    var selectedPlaceable: Placeable? = null

    var preview: PlacementPreview? = null
        private set

    /**
     * Updates the preview for the currently hovered tile.
     */
    fun update(hoveredTile: Pair<Int, Int>?) {
        val placeable = selectedPlaceable

        preview = if (hoveredTile != null && placeable != null) {
            val (x, y) = hoveredTile
            val placedObject = PlacedObject(placeable, x, y)

            PlacementPreview(
                placedObject = placedObject,
                valid = world.canPlaceObject(placedObject),
                style = style
            )
        } else {
            null
        }
    }

    /**
     * Places the selected object if its footprint is available.
     *
     * Returns the placed object on success, or null otherwise.
     */
    fun placeAt(x: Int, y: Int): PlacedObject? {
        val placeable = selectedPlaceable ?: return null
        val placedObject = PlacedObject(placeable, x, y)

        return if (world.placeObject(placedObject)) {
            placedObject
        } else {
            null
        }
    }
}