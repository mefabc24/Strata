package com.mefabc24.strata.placement

import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

/**
 * Manages object placement and its previews.
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
     * Disabling placement clears previews while retaining
     * the selected factory.
     */
    var enabled: Boolean = true
        set(value) {
            field = value

            if (!value) {
                clearPreviewPositions()
            }
        }

    private var previewPlaceable: Placeable? = null
    private var explicitPreviewPositionsActive = false

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
            clearPreviewPositions()
        }

    /**
     * The placeable currently used for preview and selection inspection.
     *
     * This instance is never placed into the world.
     */
    val selectedPlaceable: Placeable?
        get() = previewPlaceable

    var previews: List<PlacementPreview> = emptyList()
        private set

    /**
     * Updates the preview for the currently hovered tile.
     */
    fun update(hoveredTile: TilePosition?) {
        if (!enabled) {
            previews = emptyList()
            return
        }

        if (explicitPreviewPositionsActive) return

        val placeable = previewPlaceable

        previews = if (
            hoveredTile != null &&
            placeable != null
        ) {
            listOf(
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
                ),
            )
        } else {
            emptyList()
        }
    }

    /**
     * Shows ordered previews at arbitrary placement origins.
     *
     * Duplicate positions are ignored after their first occurrence. Valid
     * previews reserve their occupied tiles so later previews can report
     * conflicts without modifying the world.
     */
    fun previewAt(positions: Iterable<TilePosition>) {
        if (!enabled) {
            clearPreviewPositions()
            return
        }

        explicitPreviewPositionsActive = true

        val placeable = previewPlaceable
        if (placeable == null) {
            previews = emptyList()
            return
        }

        val reservedTiles = mutableSetOf<TilePosition>()

        previews = distinctPositions(positions).map { position ->
            val placedObject = PlacedObject(
                placeable = placeable,
                x = position.x,
                y = position.y
            )
            val occupiedTiles = placedObject.occupiedTiles()
            val valid = canPlace(placeable, position) &&
                occupiedTiles.none(reservedTiles::contains)

            if (valid) {
                reservedTiles += occupiedTiles
            }

            PlacementPreview(
                placedObject = placedObject,
                valid = valid,
                style = style
            )
        }
    }

    /**
     * Exits explicit preview mode. Hover previews resume on the next update.
     */
    fun clearPreviewPositions() {
        explicitPreviewPositionsActive = false
        previews = emptyList()
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

    /**
     * Places fresh objects at arbitrary origins in requested order.
     *
     * Duplicate positions are ignored after their first occurrence. The
     * operation is intentionally sequential and non-transactional.
     */
    fun placeAt(
        positions: Iterable<TilePosition>
    ): List<PlacedObject> {
        if (!enabled) return emptyList()

        val create = selectedFactory ?: return emptyList()

        return buildList {
            for (position in distinctPositions(positions)) {
                val placeable = create()

                if (!canPlace(placeable, position)) continue

                world.place(
                    placeable = placeable,
                    position = position
                )?.let(::add)
            }
        }
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

    private fun distinctPositions(
        positions: Iterable<TilePosition>
    ): List<TilePosition> {
        return positions.toCollection(linkedSetOf()).toList()
    }
}
