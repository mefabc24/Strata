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
        lighting { /* live ambient and point lighting */ }
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

`debug {}` configures five responsibility groups: `ui`, `visuals`, `tools`, `operations`, and `presets`. Debug settings remain live through `strata.debug`; there is no global Debug enable switch.

```kotlin
debug {
    ui {
        toolRail { enabled = true }
        settingsWindow { enabled = true }
    }
    visuals {
        grid { enabled = true }
        entities { showPath = true }
    }
    operations {
        performance { terminalLoggingIntervalSeconds = 2f }
    }
    presets {
        storage("my-game.debug")
    }
}
```

The Debug Window and Tool Rail are configured independently for availability and startup visibility. Hiding or disabling their UI does not disable visual or operational Debug features. When preset storage contains a saved DEFAULT, its visual configuration may be applied after the game-defined block; see [Debugging](Debugging.md) for the precise startup lifecycle and defaults.

## Lighting

`lighting {}` operates on the scene's live `Lighting` object. Lighting is disabled by default. Ambient and point-light settings can also change later through `strata.lighting`.

```kotlin
lighting {
    enabled = true
    ambientColor = Color.WHITE
    ambientIntensity = 0.45f
    addPointLight(
        position = EntityPosition(8.5f, 6.5f),
        radius = 5f,
        intensity = 1.2f,
        color = Color(1f, 0.75f, 0.4f, 1f)
    )
}
```

Point-light positions and radii use logical tile-space units. The scene supports up to `Lighting.MAX_POINT_LIGHTS` (16) lights. See [Rendering](Rendering.md).

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
    preview {
        objects {
            boundsPolicy = PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE
            validColor = Color(0.35f, 0.75f, 0.3f, 0.7f)
            invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
        }
        entities {
            validColor = Color(0.35f, 0.75f, 0.3f, 0.7f)
            invalidColor = Color(1f, 0.25f, 0.25f, 0.7f)
        }
    }
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
| Debug | Live settings shared by Debug renderers and runtime | Visual, tool, and operational settings are mutable; UI construction/startup fields apply during scene creation |
| Lighting | Live scene object | Ambient settings and point lights remain mutable |
| World | Attached once | Tiles, overlays, objects, and entities remain mutable |
