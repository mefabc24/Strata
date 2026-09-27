# Placement

Strata separates low-level world mutation from interactive placement:

- `World.place` validates a concrete `Placeable` and mutates the world.
- `PlacementController` manages a selected factory, hover or explicit previews, additional validation, and placement calls.
- Game code decides tool modes, drag policy, selection UI, sounds, and what to do after a successful placement.

## Enable placement for a scene

```kotlin
placement {
    showOutsideWorldPreviews = false
    previewStyle = PlacementPreviewStyle(
        validColor = Color(0.35f, 0.75f, 0.3f, 0.7f),
        invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
    )
    validator { placeable, position ->
        gameRules.allowBuild(placeable, position)
    }
}
```

The block is optional. It snapshots `PlacementSettings`; the controller is created only after `attachWorld`. The custom validator supplements geometric `World.canPlace` rules.

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

The scene calls `PlacementController.update(view.hoveredTile)` every frame. With an enabled controller and selected factory, it creates one preview for the hovered in-world tile. `PlacementPreview.valid` combines world occupancy/bounds with the game validator.

When `showOutsideWorldPreviews` is `false`, any footprint extending outside the finite world is hidden. When it is `true`, the invalid preview may be drawn outside the world; placement still fails because `World.canPlace` cannot be overridden.

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

`previewAt` enters explicit-preview mode, suppressing hover updates until `clearPreviewPositions()`. Duplicate origins after the first are ignored. Valid earlier previews reserve their occupied tiles in the preview calculation, so later conflicting previews are marked invalid without mutating the world.

```kotlin
val placed: List<PlacedObject> = placement.placeAt(origins)
placement.clearPreviewPositions()
```

Batch placement also ignores duplicate origins, proceeds in order, and is non-transactional: successful earlier placements remain if later positions fail.

The Sandbox's `SandboxBuildDragController` is an example of game-side policy. It chooses a rectangle, calculates footprint-aligned origins, calls `previewAt` during a drag, calls batch `placeAt` on release, and always clears explicit mode. It is not part of the engine API.

## Input policy

Interactive placement normally combines a `Tile` mouse-down binding with `Grid` drag/up bindings. `Tile` ensures the gesture begins inside the world. `Grid` lets the game finish or update a drag even when the pointer leaves it. Tool controllers should cancel explicit previews when leaving build mode.

See [Objects](Objects.md) and [Input and Picking](Input-and-Picking.md).

