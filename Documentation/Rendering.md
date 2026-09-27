# Rendering

Strata renders authored terrain, placed objects, entities, placement previews, optional debug overlays, and UI. Game code configures geometry and visual metadata; it does not need to call renderer internals.

## Tile geometry

```kotlin
rendering {
    tileGeometry {
        width = 64f
        height = 64f
    }
}
```

`TileGeometry.width` is the world-unit width of the diamond top face. Strata uses a fixed 2:1 face, so its height is `width / 2`. `TileGeometry.height` is the full logical terrain height and must be at least the face height.

Terrain textures scale uniformly so their pixel width equals the configured tile width. Their bottom aligns with the logical terrain rectangle. Pixels taller than the logical height extend upward without moving the top face or the ground-contact anchor.

`maxTerrainSpriteHeight` defaults to `null`; scene attachment calculates it from every prepared terrain frame at the configured tile width. Set it explicitly only when the automatic bound is unsuitable. It must be positive. The value affects culling and world/camera bounds, not sprite scaling.

## Object anchoring and sizing

Objects derive a rectangular span from the minimum/maximum coordinates in `occupiedTiles()`. Their sprite is centered horizontally over that span and bottom-aligned to its front ground anchor.

With no per-type `width`, object width defaults to the projected footprint span. With no `height`, height preserves the active frame's aspect ratio from the resolved width. Explicit `width` and `height` are world units; `scale` multiplies the result. Type-specific offsets are then added to scene-wide object offsets:

```kotlin
registrations {
    objects {
        register<Villa>("villa.png", factory = ::Villa) {
            offsetX = -6f
            offsetY = -16f
            scale = 1f
        }
    }
}

rendering {
    objects {
        offsetX = 0f
        offsetY = 1f
    }
}
```

Offsets and visual size affect drawing, sprite bounds debugging, and sprite-alpha picking. They do not change occupied tiles.

## Entity anchoring and sizing

Entity sprites are bottom-center anchored at the projection of their continuous `EntityPosition`. Without an explicit width, the texture's pixel width is used as world units; absent height preserves aspect ratio from that width. `width`, `height`, `scale`, `offsetX`, and `offsetY` are configured per entity type.

Because object and entity defaults differ, set explicit world-unit dimensions when art pixel sizes do not already match the desired scale.

## Layers and order

For a terrain cell, Strata draws ground first and overlay layers in `World.overlayLayerIds` order. Terrain cells, placed objects, entities, and previews participate in an isometric back-to-front plan based on logical spatial bounds. This lets tall sprites overlap nearby content consistently without game code assigning manual depth values.

Entities use continuous point-like sort positions; placed objects use their footprint extents. Stable fallback keys make ambiguous relationships deterministic. These ordering helpers are internal and are not a game API.

Placement previews use the selected object's active visual and tint it with the valid/invalid `PlacementPreviewStyle` color. Animation frame resolution occurs during the render using scene time, game state, and entity direction.

The debug grid may appear above objects or be followed by a redraw of objects/entities/previews for `BELOW_OBJECTS`. Object/entity diagnostics render after the normal world and grid. UI always renders after the world.

## Authoring implications

- Terrain picking uses the logical diamond, independent of transparent texture pixels or tall art.
- Object/entity sprite picking uses the final rendered rectangle and active-frame alpha mask.
- Atlas regions for world visuals must be unrotated and untrimmed.
- Irregular footprints still use their bounding coordinate span for default object sprite width and anchor calculations.
- Object visual settings never alter collision/occupancy; entity visuals never create a footprint.

See [Assets](Assets.md), [Visuals and Animation](Visuals-and-Animation.md), and [Debugging](Debugging.md).

