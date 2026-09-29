package com.mefabc24.strata.placement

import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldPlacementFailure

/** Engine-known reason that a placement preview is invalid. */
enum class PlacementFailureReason {
    FOOTPRINT_OUTSIDE_WORLD,
    OCCUPIED_TILE,
    RESERVED_TILE_CONFLICT,
    EXTERNAL_VALIDATOR_REJECTED
}

/** Result of the authoritative placement checks used by the controller. */
data class PlacementDiagnostic(
    val valid: Boolean,
    val reason: PlacementFailureReason? = null,
    val detail: String? = null
) {
    init {
        require(valid == (reason == null)) {
            "A valid placement cannot have a failure reason."
        }
    }
}

/**
 * Manages object placement and its previews.
 *
 * Geometric placement is validated by the world.
 * Additional game-specific rules can be supplied through
 * the placement validator.
 */
class PlacementController(
    private val world: World,
    private val style: PlacementPreviewStyle = PlacementPreviewStyle(),
    private val previewBoundsPolicy: PlacementPreviewBoundsPolicy =
        PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE,
    private val placementValidator: (
        placeable: Placeable,
        position: TilePosition
    ) -> Boolean = { _, _ -> true },
    private val previewEnabled: Boolean = true,
    entityPreviewSettings: PlacementEntityPreviewSettings =
        PlacementEntityPreviewSettings()
) {
    internal val objectPreviewSettings = PlacementObjectPreviewSettings().also {
        it.enabled = previewEnabled
        it.validColor = style.validColor
        it.invalidColor = style.invalidColor
        it.boundsPolicy = previewBoundsPolicy
    }
    internal val entityPreviewSettings = entityPreviewSettings.copy()
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

    /** Diagnostics aligned with [previews]. */
    var previewDiagnostics: List<PlacementDiagnostic> = emptyList()
        private set

    /** Diagnostic for the most recently evaluated hover or drag position. */
    var currentDiagnostic: PlacementDiagnostic? = null
        private set

    /**
     * Updates the preview for the currently hovered tile.
     */
    fun update(hoveredTile: TilePosition?) {
        if (!enabled) {
            previews = emptyList()
            previewDiagnostics = emptyList()
            currentDiagnostic = null
            return
        }

        if (explicitPreviewPositionsActive) return

        val placeable = previewPlaceable

        if (hoveredTile == null || placeable == null) {
            previews = emptyList()
            previewDiagnostics = emptyList()
            currentDiagnostic = null
            return
        }

        val placedObject = PlacedObject(
            placeable = placeable,
            x = hoveredTile.x,
            y = hoveredTile.y
        )

        val diagnostic = diagnose(placeable, hoveredTile)
        currentDiagnostic = diagnostic

        if (!previewEnabled) {
            previews = emptyList()
            previewDiagnostics = emptyList()
            return
        }

        if (!shouldShowPreview(placedObject)) {
            previews = emptyList()
            previewDiagnostics = emptyList()
            return
        }

        previews = listOf(
            PlacementPreview(
                placedObject = placedObject,
                valid = diagnostic.valid,
                style = style
            )
        )
        previewDiagnostics = listOf(diagnostic)
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
            previewDiagnostics = emptyList()
            currentDiagnostic = null
            return
        }

        val reservedTiles = mutableSetOf<TilePosition>()
        currentDiagnostic = null

        val diagnostics = mutableListOf<PlacementDiagnostic>()
        previews = buildList {
            for (position in distinctPositions(positions)) {
                val placedObject = PlacedObject(
                    placeable = placeable,
                    x = position.x,
                    y = position.y
                )

                val occupiedTiles = placedObject.occupiedTiles()

                val diagnostic = diagnose(
                    placeable = placeable,
                    position = position,
                    reservedTiles = reservedTiles
                )
                currentDiagnostic = diagnostic

                if (!shouldShowPreview(placedObject)) {
                    continue
                }
                val valid = diagnostic.valid

                if (valid) {
                    reservedTiles += occupiedTiles
                }

                if (!previewEnabled) {
                    continue
                }

                add(
                    PlacementPreview(
                        placedObject = placedObject,
                        valid = valid,
                        style = style
                    )
                )
                diagnostics += diagnostic
            }
        }
        previewDiagnostics = diagnostics
    }

    /**
     * Exits explicit preview mode. Hover previews resume on the next update.
     */
    fun clearPreviewPositions() {
        explicitPreviewPositionsActive = false
        previews = emptyList()
        previewDiagnostics = emptyList()
        currentDiagnostic = null
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

        if (!diagnose(placeable, position).valid) {
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

                if (!diagnose(placeable, position).valid) continue

                world.place(
                    placeable = placeable,
                    position = position
                )?.let(::add)
            }
        }
    }

    /** Runs the same checks used by previews and final placement. */
    fun diagnose(
        placeable: Placeable,
        position: TilePosition,
        reservedTiles: Set<TilePosition> = emptySet()
    ): PlacementDiagnostic {
        val worldFailure = world.placementFailure(placeable, position)
        if (worldFailure != null) {
            return PlacementDiagnostic(
                valid = false,
                reason = when (worldFailure) {
                    WorldPlacementFailure.FOOTPRINT_OUTSIDE_WORLD ->
                        PlacementFailureReason.FOOTPRINT_OUTSIDE_WORLD
                    WorldPlacementFailure.OCCUPIED_TILE ->
                        PlacementFailureReason.OCCUPIED_TILE
                }
            )
        }

        val occupiedTiles = PlacedObject(
            placeable = placeable,
            x = position.x,
            y = position.y
        ).occupiedTiles()
        if (occupiedTiles.any(reservedTiles::contains)) {
            return PlacementDiagnostic(
                valid = false,
                reason = PlacementFailureReason.RESERVED_TILE_CONFLICT
            )
        }
        if (!placementValidator(placeable, position)) {
            return PlacementDiagnostic(
                valid = false,
                reason = PlacementFailureReason.EXTERNAL_VALIDATOR_REJECTED
            )
        }
        return PlacementDiagnostic(valid = true)
    }

    private fun distinctPositions(
        positions: Iterable<TilePosition>
    ): List<TilePosition> {
        return positions.toCollection(linkedSetOf()).toList()
    }

    private fun shouldShowPreview(
        placedObject: PlacedObject
    ): Boolean {
        val occupiedTiles = placedObject.occupiedTiles()

        return when (previewBoundsPolicy) {
            PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE ->
                occupiedTiles.all { position ->
                    world.getTile(position) != null
                }

            PlacementPreviewBoundsPolicy.ORIGIN_INSIDE ->
                world.getTile(
                    placedObject.x,
                    placedObject.y
                ) != null

            PlacementPreviewBoundsPolicy.ANY_TILE_INSIDE ->
                occupiedTiles.any { position ->
                    world.getTile(position) != null
                }

            PlacementPreviewBoundsPolicy.ALWAYS ->
                true
        }
    }
}
