# Strata

Strata is a Kotlin/libGDX engine layer for finite isometric worlds. It provides multi-world registration, screen navigation and overlays, terrain and layered world rendering, multi-tile object placement, movable entities and pathfinding, input and pixel-aware picking, camera controls, audio, debugging, and a high-level Kotlin UI API. Games retain ownership of their worlds, content types, rules, AI, and semantic states.

The repository currently targets JDK 21, Kotlin 2.4.0, and libGDX 1.14.2.

## Modules

- `engine:core` — platform-independent engine and public runtime API
- `engine:desktop` — LWJGL3 launcher
- `engine:tools` — Strata-compatible texture-atlas packing tools
- `sandbox` — runnable example game and primary usage reference

Run the Sandbox with:

```text
./gradlew :sandbox:run
```

## Documentation

Start with the [documentation guide](Documentation/README.md), or go directly to [Getting Started](Documentation/Getting-Started.md) to configure dependencies, resources, a minimal `StrataGame`, a visible `World`, and the desktop launcher.

The guide covers architecture and lifecycle, configuration, assets, coordinates, terrain, objects, placement, entities, movement, animation, input and picking, camera, rendering, audio, debugging, UI, practical recipes, and troubleshooting. It documents the current `dev` implementation; KDoc remains the low-level API reference.

Atlas generation is documented separately in the [texture-atlas tools README](engine/tools/README.md).

## Icons

Icons by <a target="_blank" href="https://icons8.com">Icons8</a>