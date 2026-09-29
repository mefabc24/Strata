package com.mefabc24.strata.debug

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.iso.PickedTarget
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

/** One internally consistent view of either the cursor or the locked picking target. */
internal data class DebugPickingDiagnostics(
    val mode: DebugPickingTargetMode,
    val screen: Vector2,
    val world: Vector2,
    val grid: TilePosition,
    val tile: TilePosition?,
    val placedObject: PlacedObject?,
    val entities: List<WorldEntity>,
    val alphaAccepted: Boolean?,
    val bounds: Rectangle?
)

internal fun resolvePickingDiagnostics(
    selection: DebugPickingSelection,
    hoverTarget: PickedTarget?,
    cursorScreen: Vector2,
    cursorWorld: Vector2,
    cursorGrid: TilePosition,
    cursorTile: TilePosition?,
    world: World,
    tileCenterWorld: (TilePosition) -> Vector2,
    objectOriginWorld: (PlacedObject) -> Vector2,
    entityWorld: (WorldEntity) -> Vector2,
    worldToScreen: (Vector2) -> Vector2
): DebugPickingDiagnostics {
    val locked = selection.lockedTarget
    if (locked == null) {
        return cursorDiagnostics(
            hoverTarget = hoverTarget,
            cursorScreen = cursorScreen,
            cursorWorld = cursorWorld,
            cursorGrid = cursorGrid,
            cursorTile = cursorTile,
            world = world
        )
    }

    val tile = when (locked) {
        is PickedTarget.Object -> TilePosition(locked.placedObject.x, locked.placedObject.y)
        is PickedTarget.Entity -> locked.worldEntity.currentTile
        is PickedTarget.Tile -> locked.position
    }
    val worldPosition = when (locked) {
        is PickedTarget.Object -> objectOriginWorld(locked.placedObject)
        is PickedTarget.Entity -> entityWorld(locked.worldEntity)
        is PickedTarget.Tile -> tileCenterWorld(locked.position)
    }
    val placedObject = when (locked) {
        is PickedTarget.Object -> locked.placedObject
        else -> world.getObjectAt(tile)
    }
    val entities = when (locked) {
        is PickedTarget.Entity -> listOf(locked.worldEntity)
        else -> world.getEntities().filter { it.currentTile == tile }
    }
    return DebugPickingDiagnostics(
        mode = DebugPickingTargetMode.LOCKED,
        screen = worldToScreen(worldPosition).cpy(),
        world = worldPosition.cpy(),
        grid = tile,
        tile = tile,
        placedObject = placedObject,
        entities = entities,
        alphaAccepted = locked.alphaAccepted,
        bounds = locked.bounds?.let(::Rectangle)
    )
}

private fun cursorDiagnostics(
    hoverTarget: PickedTarget?,
    cursorScreen: Vector2,
    cursorWorld: Vector2,
    cursorGrid: TilePosition,
    cursorTile: TilePosition?,
    world: World
): DebugPickingDiagnostics {
    val placedObject = when (hoverTarget) {
        is PickedTarget.Object -> hoverTarget.placedObject
        else -> cursorTile?.let(world::getObjectAt)
    }
    val entities = when (hoverTarget) {
        is PickedTarget.Entity -> listOf(hoverTarget.worldEntity)
        else -> cursorTile?.let { tile ->
            world.getEntities().filter { it.currentTile == tile }
        }.orEmpty()
    }
    return DebugPickingDiagnostics(
        mode = DebugPickingTargetMode.HOVER,
        screen = cursorScreen.cpy(),
        world = cursorWorld.cpy(),
        grid = cursorGrid,
        tile = cursorTile,
        placedObject = placedObject,
        entities = entities,
        alphaAccepted = hoverTarget?.alphaAccepted,
        bounds = hoverTarget?.bounds?.let(::Rectangle)
    )
}
