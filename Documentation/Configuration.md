# Configuration

`Strata.configure` is a one-shot Kotlin DSL. The current Sandbox shows its complete shape:

```kotlin
override val strata = Strata().configure {
    engine {
        backgroundColor = Color(0.53f, 0.81f, 0.92f, 1f)
    }

    scene(
        terrainDirectory = "tiles",
        objectDirectory = "objects",
        entityDirectory = "entities"
    ) {
        registrations {
            terrain { registerSandboxTerrain() }
            objects { registerSandboxObjects() }
            entities { registerSandboxEntities() }
            sounds { registerSandboxSounds() }
        }

        audio { /* initial volumes */ }
        debug { /* diagnostic settings */ }
        camera { /* camera snapshot */ }
        rendering { /* rendering snapshot */ }
        placement { /* optional controller snapshot */ }
        controls { /* camera and gameplay bindings */ }
    }
}
```

The `registerSandbox...` functions above are Sandbox helpers. The blocks they extend—`TerrainRegistry`, `ObjectRegistry`, `EntityRegistry`, and `SoundRegistry`—are Strata API.

## Engine

`engine {}` configures `EngineSettings`. Its current setting is `backgroundColor`, defaulting to `(0.1, 0.1, 0.1, 1)`. The value is copied and captured when `StrataEngine` is constructed, before the libGDX render loop.

```kotlin
engine {
    backgroundColor = Color.BLACK
}
```

## Scene and registrations

Exactly one scene must be defined. `terrainDirectory` and `objectDirectory` are required; `entityDirectory` defaults to `objectDirectory`. These directories prefix file sprite paths. Atlas paths are not prefixed.

`registrations {}` groups content declarations. Registrations queue assets during setup, become immutable when setup ends, and remain readable at runtime through `strata.terrain`, `strata.objects`, `strata.entities`, and `strata.sounds`. Duplicate IDs/classes fail. Registration describes content; runtime instances still come from `World` mutations.

See [Terrain](Terrain.md), [Objects](Objects.md), [Entities](Entities.md), [Visuals and Animation](Visuals-and-Animation.md), and [Audio](Audio.md).

## Audio

`audio {}` operates on the scene's `StrataAudio`, so its values remain mutable later through `strata.audio`:

```kotlin
audio {
    masterVolume = 1f
    soundVolume = 0.8f
    musicVolume = 0.6f
    setCategoryVolume(SoundCategory.UI, 0.7f)
}
```

All volume values must be within `0f..1f`. See [Audio](Audio.md).

## Debug

`debug {}` configures four live groups: `performance`, `grid`, `objects`, and `entities`. All remain runtime mutable through `strata.debug`. They default to disabled.

```kotlin
debug {
    performance {
        enabled = false
        intervalSeconds = 2f
    }
    grid {
        enabled = false
        extent = DebugGridExtent.WORLD
        renderLayer = DebugGridRenderLayer.BELOW_OBJECTS
    }
}
```

See [Debugging](Debugging.md) for every setting.

## Camera

`camera {}` configures a snapshot used when the world view is created. Defaults include movement speed `500f`, zoom step `0.1f`, zoom range `0.25f..3f`, cursor-anchored world-based zoom, a fixed-height viewport of `720f`, camera padding `100f`, no zoom edge allowance, and `worldFill = 0.85f`.

```kotlin
camera {
    zoomAnchor = ZoomAnchor.CURSOR
    zoomEdgeAllowance = 0.3f
    viewportMode = ViewportMode.FIXED_HEIGHT
}
```

See [Camera](Camera.md).

## Rendering

`rendering {}` configures a snapshot. `TileGeometry` defaults to `width = 64f`, `height = 64f`; its diamond top face is always half the width. `maxTerrainSpriteHeight = null` asks scene attachment to derive the maximum from registered terrain. Global object offsets default to zero.

```kotlin
rendering {
    tileGeometry {
        width = 32f
        height = 24f
    }
    objects {
        offsetY = 1f
    }
}
```

With width `32f`, the diamond top face is `16f` high, so the full logical height `24f` is valid. See [Rendering](Rendering.md).

## Placement

Including `placement {}` creates a scene-owned `PlacementController` for every registered world. Omitting the block means `strata.placement` is unavailable.

```kotlin
placement {
    showOutsideWorldPreviews = false
    previewStyle = PlacementPreviewStyle(
        validColor = Color(0.35f, 0.75f, 0.3f, 0.7f),
        invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
    )
    validator { placeable, position ->
        canBuildHere(placeable, position)
    }
}
```

The validator supplements `World.canPlace`; it cannot allow an out-of-bounds or occupied footprint. Settings are copied for attachment, while controller selection and enablement are runtime state. See [Placement](Placement.md).

## Controls

`controls {}` is copied when setup ends. `camera {}` changes keys and enables/disables keyboard movement, middle-button dragging, or wheel zoom. `gameplay {}` supplies an ordered snapshot of `WorldInputBinding` values.

```kotlin
controls {
    camera {
        moveUp = Input.Keys.UP
        moveDown = Input.Keys.DOWN
        dragButton = Input.Buttons.RIGHT
    }
    gameplay {
        bindings = gameInputBindings()
    }
}
```

Binding lambdas run later, so the Sandbox uses provider lambdas such as `{ tools }` for `lateinit` game controllers created in `onReady()`. Do not evaluate those controllers during configuration. See [Input and Picking](Input-and-Picking.md).

## What can change later

| Area | Setup behavior | Runtime access |
| --- | --- | --- |
| Engine background | Captured by `StrataEngine` | No facade mutation affects the captured clear color |
| Registrations | Frozen and prepared during scene creation | Read entries and resolve visuals; no new registration |
| Camera/rendering/controls | Copied after the scene block | View/controller state can change, not the original settings snapshot |
| Placement settings | Copied; controller created on attachment | `enabled`, `selectedFactory`, previews, and placement calls |
| Audio | Live object | Volumes and playback remain mutable |
| Debug | Live objects passed to debug renderers | All settings remain mutable |
| World | Attached once | Tiles, overlays, objects, and entities remain mutable |
