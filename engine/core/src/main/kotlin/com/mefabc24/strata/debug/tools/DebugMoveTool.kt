package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.debug.DebugWorldState
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity
import com.mefabc24.strata.world.WorldPlacementFailure

internal enum class DebugMoveTargetType { OBJECT, ENTITY }

internal data class DebugMovePreview(
    val type: DebugMoveTargetType,
    val source: TilePosition,
    val target: TilePosition,
    val valid: Boolean,
    val rejection: WorldPlacementFailure? = null,
    val objectPreview: PlacementPreview? = null,
    val occupiedTiles: Set<TilePosition> = emptySet()
)

internal data class DebugMoveOutcome(
    val type: DebugMoveTargetType,
    val target: TilePosition,
    val success: Boolean,
    val rejection: WorldPlacementFailure? = null
)

/** Drag state for relocating existing objects and entities. */
class DebugMoveTool internal constructor(
    private val world: World,
    private val state: DebugWorldState,
    private val previewStyle: PlacementPreviewStyle = PlacementPreviewStyle.DEFAULT
) {
    private var subject: Subject? = null

    internal val active: Boolean get() = subject != null
    internal val preview: DebugMovePreview? get() = state.movePreview

    internal fun begin(target: PickedTarget?, position: TilePosition): Boolean {
        subject = when (target) {
            is PickedTarget.Object -> Subject.Object(target.placedObject)
            is PickedTarget.Entity -> Subject.Entity(target.worldEntity)
            else -> null
        }
        if (subject == null) return false
        updatePreview(position)
        return true
    }

    internal fun dragTo(position: TilePosition): Boolean {
        if (subject == null) return false
        updatePreview(position)
        return true
    }

    internal fun finish(position: TilePosition): DebugMoveOutcome? {
        val moving = subject ?: return null
        updatePreview(position)
        val current = state.movePreview ?: return null
        val success = if (!current.valid) false else when (moving) {
            is Subject.Object -> world.relocate(moving.value, position)
            is Subject.Entity -> world.teleportEntity(
                moving.value,
                EntityPosition.centerOf(position)
            )
        }
        return DebugMoveOutcome(
            type = current.type,
            target = position,
            success = success,
            rejection = current.rejection
        ).also { cancel() }
    }

    internal fun cancel(): Boolean {
        if (subject == null && state.movePreview == null) return false
        subject = null
        state.movePreview = null
        return true
    }

    private fun updatePreview(position: TilePosition) {
        state.movePreview = when (val moving = checkNotNull(subject)) {
            is Subject.Object -> objectPreview(moving.value, position)
            is Subject.Entity -> {
                val valid = world.getTile(position) != null
                DebugMovePreview(
                    type = DebugMoveTargetType.ENTITY,
                    source = moving.value.currentTile,
                    target = position,
                    valid = valid,
                    rejection = if (valid) null else WorldPlacementFailure.FOOTPRINT_OUTSIDE_WORLD,
                    occupiedTiles = setOf(position)
                )
            }
        }
    }

    private fun objectPreview(
        placedObject: PlacedObject,
        position: TilePosition
    ): DebugMovePreview {
        val previewObject = PlacedObject(placedObject.placeable, position.x, position.y)
        val rejection = world.relocationFailure(placedObject, position)
        return DebugMovePreview(
            type = DebugMoveTargetType.OBJECT,
            source = TilePosition(placedObject.x, placedObject.y),
            target = position,
            valid = rejection == null,
            rejection = rejection,
            objectPreview = PlacementPreview(previewObject, rejection == null, previewStyle),
            occupiedTiles = previewObject.occupiedTiles()
        )
    }

    private sealed interface Subject {
        data class Object(val value: PlacedObject) : Subject
        data class Entity(val value: WorldEntity) : Subject
    }
}
