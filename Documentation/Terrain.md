# Terrain

Terrain has two parts: game-owned tile data and a scene-owned visual registration. `Tile` is only a marker interface. The game decides which fields a tile contains and how they map to `TerrainId`.

```kotlin
enum class TerrainType : TerrainId {
    GRASS,
    WATER
}

data class GameTile(
    val terrain: TerrainType
) : Tile
```

## Register visuals

Register every terrain ID that `terrainFor` can return:

```kotlin
registrations {
    terrain {
        register(TerrainType.GRASS, "grass.png")
        registerAnimated(
            type = TerrainType.WATER,
            frames = listOf("water-0.png", "water-1.png"),
            frameDuration = 0.2f
        )
    }
}
```

The Sandbox uses atlas equivalents:

```kotlin
registerAtlas(
    TerrainType.GRASS,
    atlas = "atlas/tiles.atlas",
    region = "grass"
)

registerAnimatedAtlas(
    TerrainType.BUSH_ANIMATED,
    atlas = "atlas/tiles.atlas",
    region = "bush-animated",
    frameDuration = 0.2f
)
```

Terrain also supports `registerVisual` for the common source DSL and `registerStateful`. Terrain animations and states use the shared scene animation clock by default, so tiles in the same state remain synchronized.

```kotlin
enum class WaterState : VisualStateId { CALM, ROUGH }

registerStateful(
    type = TerrainType.WATER,
    stateFor = { tile -> (tile as WaterTile).visualState }
) {
    state(WaterState.CALM) { sprite("water-calm.png") }
    state(WaterState.ROUGH) {
        animated(
            listOf("water-rough-0.png", "water-rough-1.png"),
            frameDuration = 0.12f
        )
    }
}
```

The state identifiers and transition policy belong to the game. The resolver must return a registered state.

## Register the game's tile model

Registration does not change world data. `terrainFor` bridges a runtime tile to its visual ID:

```kotlin
strata.worlds.register("surface", world) { tile ->
    (tile as GameTile).terrain
}
```

The same function resolves ground and overlay tiles. Returning an unregistered ID fails during rendering.

## Change ground terrain

Use `world.terrain`, which validates the whole target area before mutation:

```kotlin
world.terrain.setTile(
    TilePosition(8, 5),
    GameTile(TerrainType.WATER)
)

world.terrain.fill(10..14, 3..6) { x, y ->
    GameTile(if ((x + y) % 2 == 0) TerrainType.GRASS else TerrainType.WATER)
}
```

`setTile` requires an in-world position. `fill` requires non-empty ranges fully inside the world; it creates all replacements before changing any cell.

## Overlay layers

Overlays are optional `Tile?` grids with the same dimensions as the world. Add layers before using them:

```kotlin
world.addOverlayLayer("roads")
world.setOverlayTile(
    layerId = "roads",
    position = TilePosition(4, 6),
    tile = GameTile(TerrainType.GRASS)
)

world.setOverlayTile("roads", 4, 6, null) // Clear the cell.
```

Layer IDs must be non-blank and unique. Layers render in insertion order after the ground sprite for each cell. There is currently no public remove/reorder operation. Unknown layer IDs and out-of-world writes fail; out-of-world reads return `null` after the layer ID is validated.

`overlayLayerIds` is a snapshot in render order. `forEachOverlayTile` reports every cell, including empty cells as `null`.

## Runtime registry access

`strata.terrain.entries` returns entries in registration order. `TerrainEntry.texture` and `sprite` are valid only for a single-visual registration; stateful terrain must be resolved with a runtime tile. Registrations are already prepared by `onReady()` and cannot be added after scene setup.

See [Assets](Assets.md), [World and Coordinates](World-and-Coordinates.md), and [Visuals and Animation](Visuals-and-Animation.md).
