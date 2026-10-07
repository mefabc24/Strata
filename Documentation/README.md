# Strata engine guide

Strata is a Kotlin/libGDX engine layer for finite isometric worlds. It provides multi-world management, screen navigation, world rendering, object placement, movable entities, pathfinding, input and picking, camera control, audio, debugging, and a high-level Kotlin UI layer. A game supplies its own worlds, content, rules, and controllers.

This guide is for developers building a game on the current `dev` implementation. It explains workflows and boundaries; KDoc remains the exact API reference. The Sandbox module is the working example used throughout.

## Start here

The screen and world lifecycle is covered in [Screens and multiple worlds](Screens-and-Worlds.md).

1. [Getting Started](Getting-Started.md) — dependencies, resources, a minimal world, and desktop launch
2. [Architecture](Architecture.md) — engine/game responsibilities, registration, lifecycle, and ownership
3. [Configuration](Configuration.md) — the `Strata.configure` DSL and which settings are snapshots
4. [Assets](Assets.md) — classpath paths, files, sheets, atlases, loading, and alpha masks

## Build the world

- [World and Coordinates](World-and-Coordinates.md) — `World`, `TilePosition`, `EntityPosition`, bounds, and projection
- [Terrain](Terrain.md) — game tile data, terrain registrations, changes, and overlays
- [Objects](Objects.md) — `Placeable`, footprints, `PlacedObject`, registration, and world mutation
- [Placement](Placement.md) — interactive previews, direct multi-waypoint paths, validation, and placement policy
- [Entities](Entities.md) — game entities and engine-owned `WorldEntity` state
- [Movement and Pathfinding](Movement-and-Pathfinding.md) — path queries, waypoints, movement, and game AI

## Interaction and presentation

- [Visuals and Animation](Visuals-and-Animation.md) — static, animated, stateful, and directional visuals
- [Input and Picking](Input-and-Picking.md) — bindings, event consumption, grid interaction, and pick modes
- [Camera](Camera.md) — movement, dragging, zoom, bounds, viewports, and resize behavior
- [Rendering](Rendering.md) — tile geometry, anchoring, priorities, ordering, and lighting
- [Audio](Audio.md) — sound registration, playback, categories, music, and volume
- [Debugging](Debugging.md) — Debug UI, presets, diagnostics, tools, and operational services
- [UI](UI.md) — UI lifecycle, themes, layouts, controls, selection, and input priority

## Apply and diagnose

- [Recipes](Recipes.md) — short examples for common tasks
- [Troubleshooting](Troubleshooting.md) — symptoms, likely causes, and fixes

The fastest route for a first project is **Getting Started → Configuration → World and Coordinates → Terrain**, followed by Objects or Entities depending on the game. Read Architecture before adding game rules so engine-owned spatial state stays separate from game-owned behavior.
