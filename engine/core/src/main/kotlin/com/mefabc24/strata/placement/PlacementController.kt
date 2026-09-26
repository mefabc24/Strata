package com.mefabc24.strata.placement

import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
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
    /**
     * Controls preview generation and placement operations.
     *
     * Disabling placement clears the preview while retaining
     * the selected factory.
     */
    var enabled: Boolean = true
        set(value) {
            field = value

            if (!value) {
                preview = null
            }
        }

    private var previewPlaceable: Placeable? = null

    /**
     * Creates a new placeable for each placement operation.
     *
     * Assigning a factory also creates a separate instance used
     * exclusively for placement previews.
     */
    var selectedFactory: (() -> Placeable)? = null
        set(value) {
            field = value
            previewPlaceable = value?.invoke()
            preview = null
        }

    /**
     * The placeable currently used for preview and selection inspection.
     *
     * This instance is never placed into the world.
     */
    val selectedPlaceable: Placeable?
        get() = previewPlaceable

    var preview: PlacementPreview? = null
        private set

    /**
     * Updates the preview for the currently hovered tile.
     */
    fun update(hoveredTile: TilePosition?) {
        if (!enabled) {
            preview = null
            return
        }

        val placeable = previewPlaceable

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
     * Creates and places a new object at the given position.
     *
     * Returns the placed object on success, or null otherwise.
     */
    fun placeAt(
        position: TilePosition
    ): PlacedObject? {
        if (!enabled) return null

        val create =
            selectedFactory ?: return null

        val placeable = create()

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
     * Creates and places a new object at the given coordinates.
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