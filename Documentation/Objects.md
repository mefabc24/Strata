# Objects

Objects are stationary game values that occupy one or more world tiles. The game implements `Placeable`; the world creates a `PlacedObject` wrapper when that value is placed.

```kotlin
class House : Placeable {
    override val footprint = Footprint.square(2)
}
```

`Placeable` is the logical object. `PlacedObject` holds that value plus the chosen `(x, y)` placement origin. Its `occupiedTiles()` translates footprint offsets into world coordinates.

## Footprints and origins

A 1×1 object:

```kotlin
override val footprint = Footprint.square(1)
```

A rectangular object:

```kotlin
override val footprint = Footprint.rectangle(
    width = 3,
    height = 2,
    origin = FootprintOrigin.NORTH
)
```

An irregular L shape:

```kotlin
override val footprint = Footprint.custom(
    TileOffset(0, 0),
    TileOffset(1, 0),
    TileOffset(0, 1),
    origin = TileOffset(0, 0)
)
```

The placement `(x, y)` corresponds to `footprint.origin`, not necessarily offset `(0, 0)`. Translation is `world = placement + offset - origin`. A custom origin must be one of the occupied offsets.

For rectangles, the named corners map to offsets as follows:

| Origin | Offset |
| --- | --- |
| `NORTH` | `(0, 0)` |
| `EAST` | `(width - 1, 0)` |
| `SOUTH` | `(width - 1, height - 1)` |
| `WEST` | `(0, height - 1)` |

Call `PlacedObject(...).occupiedTiles()` when a tool needs to preview the actual translated cells instead of assuming the placement coordinate is the minimum corner.

## Register object visuals

```kotlin
registrations {
    objects {
        register<House>(
            sprite = "house.png",
            factory = ::House
        ) {
            offsetY = 3f
        }
    }
}
```

The type is the registration key. A factory is optional; supplying it makes the `ObjectEntry` constructible and exposes it through `constructibleEntries`. `entry.create()` validates that the factory returned the registered type. The factory does not place the object.

Static files, file animations, sprite sheets, static/animated atlas regions, and stateful visuals are supported. Per-type `ObjectSpriteSettings` provide `offsetX`, `offsetY`, optional world-unit `width`/`height`, and `scale` (default `1f`). See [Assets](Assets.md) and [Visuals and Animation](Visuals-and-Animation.md).

Stateful constructible objects must supply a selection visual because a menu has no runtime `PlacedObject` from which to derive state:

```kotlin
registerStateful<Workshop>(
    factory = ::Workshop,
    stateFor = { _, workshop -> workshop.visualState },
    selection = { sprite("workshop-idle.png") }
) {
    state(WorkshopState.IDLE) { sprite("workshop-idle.png") }
    state(WorkshopState.WORKING) {
        spriteSheet("workshop-working.png", 64, 96, 0.1f)
    }
}
```

`Workshop`, its state, and the rule that changes it are game code.

## Place and remove

```kotlin
val placed = world.place(
    placeable = House(),
    position = TilePosition(5, 5)
) ?: error("The footprint is outside the world or occupied.")

val occupant = world.getObjectAt(6, 5) // The same 2x2 PlacedObject.
world.remove(placed)
```

`World.canPlace` checks that every occupied tile exists and has no object. `place` repeats that check and returns `null` on failure. Objects reserve all footprint cells. `removeAt(position)` removes the entire object occupying that cell and returns it, or returns `null` for an empty cell.

`getObjects()` is a read-only live view. `objectVersion` changes after successful placement/removal. Objects have no movement state and do not share the entity system.

## Picking

Footprint picking checks the tile under the pointer and then `getObjectAt`. Sprite-alpha picking checks the current visual's rendered bounds and alpha mask. `SPRITE_OR_FOOTPRINT` tries the sprite first, then the footprint. Visual offsets and size settings therefore affect sprite picking but never logical occupancy.

See [Placement](Placement.md), [Rendering](Rendering.md), and [Input and Picking](Input-and-Picking.md).
