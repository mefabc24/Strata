# Placement

Strata separates low-level world mutation from interactive placement:

- `World.place` validates a concrete `Placeable` and mutates the world.
- `PlacementController` manages a selected factory, hover or explicit previews, additional validation, and placement calls.
- `TilePathResolver` and `TilePathSelection` select logical tile chains independently of objects and worlds.
- Game code decides tool modes, drag policy, selection UI, sounds, and what to do after a successful placement.

## Enable placement for a scene

```kotlin
placement {
    preview {
        objects {
            enabled = true
            boundsPolicy = PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE
            validColor = Color(0.35f, 0.75f, 0.3f, 0.7f)
            invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
        }
        entities {
            enabled = true
            validColor = Color(0.35f, 0.75f, 0.3f, 0.7f)
            invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
        }
    }
    validator { placeable, position ->
        gameRules.allowBuild(placeable, position)
    }
}
```

The block is optional. It snapshots `PlacementSettings`; a controller is created for each registered world. Object preview settings configure the placement controller and the Debug Move tool. Entity preview settings configure Debug Spawn and Move previews. Both preview types default to enabled with green valid and red invalid tints. The custom validator supplements geometric `World.canPlace` rules.

## Select and place

Set a factory after creation. A fresh object is required for every placement:

```kotlin
strata.placement.selectedFactory = ::House

val placed = strata.placement.placeAt(TilePosition(5, 5))
if (placed != null) {
    strata.audio.playSound(BuildingSound.PLACE)
}
```

Assigning `selectedFactory` immediately creates a separate preview-only instance and clears current previews. `selectedPlaceable` exposes that preview instance; it is never added to the world. Setting the factory to `null` clears selection and previews.

`enabled = false` prevents placement and clears previews while retaining the factory. Re-enabling restores hover previews on a subsequent scene update.

## Hover previews

The scene calls `PlacementController.update(view.hoveredTile)` every frame. With an enabled controller and selected factory, it creates one preview for the hovered logical tile when the configured bounds policy allows it. `PlacementPreview.valid` combines world occupancy/bounds with the game validator.

`PlacementPreviewBoundsPolicy` affects visibility only; final placement still requires every footprint tile to be inside the world:

| Policy | Preview visibility |
| --- | --- |
| `ALL_TILES_INSIDE` | Every footprint tile must be in-world; this is the default |
| `ORIGIN_INSIDE` | The placement origin must be in-world |
| `ANY_TILE_INSIDE` | At least one footprint tile must be in-world |
| `ALWAYS` | The preview may be fully outside the world |

Disabling object previews suppresses drawing but does not disable validation or placement.

## Explicit and drag previews

```kotlin
placement.previewAt(
    listOf(
        TilePosition(2, 2),
        TilePosition(3, 2),
        TilePosition(4, 2)
    )
)
```

`previewAt` enters explicit-preview mode, suppressing hover updates until `clearPreviewPositions()`. Duplicate origins after the first are ignored. Valid earlier previews reserve their occupied tiles in the preview calculation, so later conflicting previews are marked invalid without mutating the world. `previewDiagnostics` is aligned with the previews that are actually shown; `currentDiagnostic` reports the most recently evaluated origin. `diagnose(...)` exposes the same authoritative checks for game UI.

```kotlin
val placed: List<PlacedObject> = placement.placeAt(origins)
placement.clearPreviewPositions()
```

Batch placement also ignores duplicate origins, proceeds in order, and is non-transactional: successful earlier placements remain if later positions fail.

A game-side drag controller can choose a rectangle, calculate footprint-aligned origins, call `previewAt` during a drag, call batch `placeAt` on release, and always clear explicit mode. The built-in Debug Build tool follows the same placement-controller boundary.

## Direct tile paths

`DirectTilePathResolver.resolve(start, end)` selects an eight-connected grid line
with both endpoints included. It uses Bresenham rasterization with canonical
tie-breaking: reversing endpoints reverses the same chain. It does not route
around obstacles or clip to world bounds. Unlike `World.findPath`, this API
answers which tiles the user is drawing, independently of traversal rules.

```kotlin
val selection = TilePathSelection() // optionally supply a TilePathResolver
selection.start(TilePosition(2, 2))
selection.addWaypoint(TilePosition(6, 4))
selection.addWaypoint(TilePosition(8, 8))
selection.previewTo(TilePosition(10, 9))
val livePath: List<TilePosition> = selection.positions
val finishedPath: List<TilePosition> = selection.finish()
```

Confirmed segments are cached; cursor changes resolve only the segment from the
latest waypoint. Shared junctions occur once. Nonconsecutive revisits remain in
the logical chain; the placement pipeline deduplicates origins before creating
objects. Lists are read-only snapshots. `cancel()` and `clear()` discard all
selection state; `finish()` returns the combined path and also returns to idle.
`previewTo(null)` removes only the unfinished segment. There is no waypoint limit.
Custom resolvers must return ordered endpoints and an eight-connected chain
without consecutive duplicates. Alternative routing remains separate from selection.

## Path placement and previews

Each placement controller owns `path: PathPlacementController`. It reuses the
same explicit preview calculation and batch placement as rectangular dragging:

```kotlin
placement.selectedFactory = ::Marker // a game-owned Placeable
val path = placement.path
path.begin(TilePosition(2, 2))
path.addWaypoint(TilePosition(6, 4))
path.addWaypoint(TilePosition(8, 8))
path.update(cursorGridPosition)
val logicalPath = path.positions
val placed = path.finish() // uses the same cached logical path as the preview
```

`update` revalidates the complete preview, including confirmed segments, while
only resolving the unfinished segment. Bounds visibility, footprint reservations,
occupancy, colors, preview enablement, and external validation remain the normal
placement rules. Invalid origins are skipped in order, exactly as for batch
placement; there is no all-or-nothing policy. Large footprints can conflict with
neighboring origins and are validated rather than changing the selected path.

`finish(end)` can update the last endpoint before placement. `finish()`, `cancel()`,
and `clear()` clear path state and explicit previews. Changing the selected factory,
disabling placement, calling `clearPreviewPositions()`, starting explicit previews
or another placement operation, or deactivating the world cancels the active path.
Games should also cancel when leaving their path tool. Assign `path.resolver` to
use another resolver; this cancels any current selection.

Preview and placement share logical origins, but placement always rechecks the
current world and validator. Selected factories should produce consistent
footprints and validators should avoid side effects so previews can predict results.

## Input policy

Interactive placement normally combines a `Tile` mouse-down binding with `Grid` drag/up bindings. `Tile` ensures the gesture begins inside the world. `Grid` lets the game finish or update a drag even when the pointer leaves it. Tool controllers should cancel explicit previews when leaving build mode.

See [Objects](Objects.md) and [Input and Picking](Input-and-Picking.md).
