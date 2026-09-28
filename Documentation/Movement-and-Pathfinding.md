# Movement and pathfinding

Strata provides a four-neighbor shortest-path query and route following. The game defines traversability and decides when an entity should move.

## Find and follow a path

```kotlin
import com.mefabc24.strata.pathfinding.findPath

val entity = world.addEntity(
    entity = Wolf(),
    position = EntityPosition.centerOf(TilePosition(2, 3))
)

val path = world.findPath(
    start = entity.currentTile,
    goal = TilePosition(12, 9),
    canEnter = { position ->
        val tile = world.getTile(position) as GameTile
        tile.terrain != TerrainType.WATER &&
            world.getObjectAt(position) == null
    }
)

if (path != null) {
    entity.followPath(path, speed = 2f)
}
```

`speed` is Euclidean distance in logical tiles per second and must be finite and positive. The scene advances routes every update.

## Path query behavior

`findPath` requires in-world start and goal positions. It uses edge-connected neighbors only, uniform cost `1`, and a Manhattan heuristic. The returned shortest path includes both start and goal. If start equals goal, the result is a one-element list, provided `canEnter` accepts it. No route returns `null`.

`canEnter` is called for start and goal and as candidates are explored. Pathfinding does not automatically inspect terrain, objects, entities, slopes, or diagonal movement. Encode every game rule in this predicate. Entity positions and reservations are also game concerns.

## Route following

`followPath` converts every supplied tile to its center and follows those waypoints in order. It replaces any active route. An empty list clears movement. The method does not validate adjacency, world bounds, or whether the list came from `findPath`; it can follow any ordered tile-center waypoints.

If the entity is not already at the first tile center, it moves there first. When a path from `findPath(entity.currentTile, goal)` includes the start tile, a centered entity skips that already-reached waypoint. A non-centered entity first centers itself in the start tile.

Large frame deltas can cross multiple waypoints. The final position is the exact final center, `isMoving` becomes false, `movementSpeed` becomes `null`, and remaining lists become empty.

```kotlin
entity.cancelMovement() // Stop at the current continuous position.
entity.teleport(EntityPosition(4.5f, 6.5f)) // Relocate and cancel.
```

`remainingWaypoints` returns continuous centers. `remainingPath` maps those remaining centers back to tiles. Both are snapshots.

## Direction changes

Movement selects the dominant logical axis:

| Motion | Direction |
| --- | --- |
| positive `x` | `SOUTH_EAST` |
| negative `x` | `NORTH_WEST` |
| positive `y` | `SOUTH_WEST` |
| negative `y` | `NORTH_EAST` |

When absolute `x` and `y` deltas tie, the `x` direction wins. After route completion, the last direction remains. `face` can change it while stationary.

## What game AI owns

Strata moves along a provided route. It does not choose goals, retry failed paths, avoid moving entities, reserve destinations, idle, roam, or react to world changes. The Sandbox's `SandboxWolfController` holds those policies: it waits for a random idle duration, chooses bounded destinations, calls `findPath`, and starts `followPath` at `2f` tiles per second.

If world occupancy changes while an entity is already following a route, Strata does not revalidate it. A game that needs dynamic avoidance should cancel or replace routes.

See [Entities](Entities.md) and [Debugging](Debugging.md).
