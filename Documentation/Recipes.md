# Recipes

These snippets use current public APIs and assume normal imports. Names such as `GameTile`, `TerrainType`, `House`, `Wolf`, `WolfState`, `GameSound`, and `GameSoundCategory` are game-owned example types, modeled after the Sandbox. Registration snippets belong inside the corresponding `registrations` block; runtime snippets belong after `onReady()` unless stated otherwise.

## 1. Create a world

Creates a finite 40×30 ground grid.

```kotlin
val world = World(width = 40, height = 30) { x, y ->
    GameTile(
        terrain = if (y < 4) TerrainType.SAND else TerrainType.GRASS
    )
}
```

`GameTile` must implement `Tile`. Width and height must be positive.

## 2. Register terrain

Queues one static terrain sprite during scene setup.

```kotlin
registrations {
    terrain {
        register(TerrainType.GRASS, "grass.png")
    }
}
```

The file resolves relative to `terrainDirectory`. Registration does not create tiles.

## 3. Change a terrain tile

Replaces game data at an existing coordinate.

```kotlin
world.terrain.setTile(
    position = TilePosition(6, 9),
    tile = GameTile(TerrainType.WATER)
)
```

The position must be inside the world, and `terrainFor` must map the new tile to a registered ID.

## 4. Add an overlay layer

Creates a sparse terrain layer and writes one cell.

```kotlin
world.addOverlayLayer("roads")
world.setOverlayTile(
    layerId = "roads",
    x = 8,
    y = 5,
    tile = GameTile(TerrainType.ROAD)
)
```

Register `TerrainType.ROAD` first. Pass `tile = null` to clear the cell.

## 5. Register a 1×1 object

Defines logical occupancy and a constructible visual.

```kotlin
class OakTree : Placeable {
    override val footprint = Footprint.square(1)
}

registrations {
    objects {
        register<OakTree>(
            sprite = "oak.png",
            factory = ::OakTree
        ) {
            offsetY = 3f
        }
    }
}
```

The factory makes the registry entry constructible; it does not place a tree.

## 6. Register a multi-tile object

Defines a 3×2 footprint whose placement coordinate is its east corner.

```kotlin
class Workshop : Placeable {
    override val footprint = Footprint.rectangle(
        width = 3,
        height = 2,
        origin = FootprintOrigin.EAST
    )
}

registrations {
    objects {
        register<Workshop>("workshop.png", factory = ::Workshop)
    }
}
```

Use `PlacedObject(...).occupiedTiles()` when previewing the translated cells.

## 7. Place and remove an object

Mutates the world without interactive placement UI.

```kotlin
val placed = world.place(House(), TilePosition(5, 5))

if (placed != null) {
    world.remove(placed)
}
```

`place` returns `null` for an occupied or out-of-bounds footprint. `removeAt(position)` can remove the entire object found under one occupied tile.

## 8. Configure interactive placement

Creates a placement controller at world attachment and selects a factory at runtime.

```kotlin
// Scene setup
placement {
    showOutsideWorldPreviews = false
    validator { placeable, position ->
        gameRules.allowBuild(placeable, position)
    }
}

// onReady, after attachWorld
strata.placement.selectedFactory = ::House
strata.placement.enabled = true
```

Omitting `placement {}` means `strata.placement` is unavailable. The validator cannot override world bounds or occupancy.

## 9. Register an entity

Maps the game-owned type to a static bottom-centered sprite.

```kotlin
class Wolf : Entity

registrations {
    entities {
        register<Wolf>("wolf.png") {
            width = 48f
        }
    }
}
```

Registration does not spawn a wolf.

## 10. Spawn an entity

Creates engine-owned spatial state at a tile center.

```kotlin
val wolf = world.addEntity(
    entity = Wolf(),
    position = EntityPosition.centerOf(TilePosition(4, 7)),
    direction = EntityDirection.SOUTH_EAST
)
```

`addEntity` does not check world bounds and entities do not occupy tiles.

## 11. Move an entity with pathfinding

Finds a four-neighbor route and follows it at two logical tiles per second.

```kotlin
val path = world.findPath(
    start = wolf.currentTile,
    goal = TilePosition(15, 10),
    canEnter = { position ->
        world.getObjectAt(position) == null
    }
)

if (path != null) {
    wolf.followPath(path, speed = 2f)
}
```

Import `com.mefabc24.strata.pathfinding.findPath`. Pathfinding applies only the rules in `canEnter`.

## 12. Create a stateful animated entity

Chooses a game state from runtime movement.

```kotlin
enum class WolfState : VisualStateId { IDLE, WALK }

registerStateful<Wolf>(
    stateFor = { entity ->
        if (entity.isMoving) WolfState.WALK else WolfState.IDLE
    }
) {
    state(WolfState.IDLE) { sprite("wolf-idle.png") }
    state(WolfState.WALK) {
        animated(
            frames = listOf("wolf-walk-0.png", "wolf-walk-1.png"),
            frameDuration = 0.1f
        )
    }
}
```

Entity states default to local playback, so `WALK` restarts when the runtime entity enters it.

## 13. Create directional sprite-sheet animation

Uses one row per engine direction.

```kotlin
registerDirectional<Wolf> {
    directionalSpriteSheet(
        path = "wolf/wolf-run.png",
        frameWidth = 64,
        frameHeight = 64,
        frameDuration = 0.1f,
        directionRows = mapOf(
            EntityDirection.SOUTH_WEST to 0,
            EntityDirection.SOUTH_EAST to 1,
            EntityDirection.NORTH_WEST to 2,
            EntityDirection.NORTH_EAST to 3
        ),
        framesPerDirection = 8
    )
}
```

Every direction is required. `framesPerDirection` must match the number of sheet columns.

## 14. Handle tile input

Runs only when a click lands on an in-world tile.

```kotlin
WorldInputBinding.Tile(
    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
    enabled = { mode == ToolMode.PAINT }
) { x, y ->
    painter.begin(TilePosition(x, y))
}
```

Return `true` from `painter.begin` when it handles the event.

## 15. Handle grid drag input

Keeps receiving logical coordinates outside the finite world.

```kotlin
WorldInputBinding.Grid(
    trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
    enabled = { drag.active }
) { x, y ->
    drag.update(TilePosition(x, y))
    true
}
```

Pair this with a `Grid` mouse-up or `NoPicking` cleanup binding so the gesture cannot remain active outside the world.

## 16. Pick an object

Removes the frontmost solid sprite, falling back to its footprint.

```kotlin
WorldInputBinding.Object(
    trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
    mode = ObjectPickingMode.SPRITE_OR_FOOTPRINT
) { placed ->
    world.remove(placed)
}
```

The Boolean returned by `world.remove` becomes the event-consumption result.

## 17. Pick an entity

Selects an entity through the current frame's alpha mask.

```kotlin
WorldInputBinding.Entity(
    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
) { entity ->
    selectedEntity = entity
    true
}
```

The default mode is `EntityPickingMode.SPRITE_ALPHA`; entities without a registered visual cannot be picked this way.

## 18. Play a registered sound

Registers during setup and plays after creation.

```kotlin
registrations {
    sounds {
        register(
            id = GameSound.PLACE,
            path = "audio/place.wav",
            category = GameSoundCategory.BUILDING
        )
    }
}

val handle = strata.audio.playSound(GameSound.PLACE)
```

The nullable handle can later be passed to `stopSound`.

## 19. Enable grid debugging

Shows the logical grid above the normal world.

```kotlin
strata.debug.grid.apply {
    enabled = true
    extent = DebugGridExtent.VISIBLE
    renderLayer = DebugGridRenderLayer.ABOVE_OBJECTS
    lineWidth = 1f
}
```

`VISIBLE` includes off-world logical grid positions in the camera view.

## 20. Enable object and entity debugging

Shows footprint/origin diagnostics and entity route state.

```kotlin
strata.debug.objects.apply {
    enabled = true
    showOccupiedTiles = true
    showOriginTile = true
    showSpriteBounds = true
}

strata.debug.entities.apply {
    enabled = true
    showCurrentTile = true
    showPosition = true
    showPath = true
    showDirection = true
    showSpriteBounds = true
}
```

These settings are live; no world reattachment is required.
