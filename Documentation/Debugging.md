# Debugging

`DebugSettings` is the live, scene-wide entry point for developer UI, visual diagnostics, interactive tools, and operational Debug services. Configure initial values in `debug {}` and change them later through `strata.debug`.

The public API is grouped by responsibility:

| Group | Purpose |
| --- | --- |
| `ui` | Debug Window and Tool Rail availability, startup visibility, and shortcuts |
| `visuals` | Grid, world, object, entity, picking, render-order, culling, and camera diagnostics |
| `tools` | Inspect, Paint, Delete, and Pathfinding tool settings and game callbacks |
| `operations` | Performance, simulation, world statistics, Event Bus, notifications, and camera overrides |
| `presets` | Saved DEFAULT storage and startup behavior |

This grouping is part of the consumer API. Internal Debug UI classes and source-file organization are not.

## Scene configuration

```kotlin
debug {
    ui {
        toolRail {
            enabled = true
            toggleKey = Input.Keys.F2
        }
        settingsWindow {
            enabled = true
            toggleKey = Input.Keys.F3
        }
    }

    visuals {
        grid { enabled = true }
        objects { showOccupiedTiles = true }
        entities { showPath = true }
    }

    tools {
        paint { brushSize = 3 }
        delete { dragEnabled = true }
        pathfinding { movementMode = PathMovementMode.EIGHT_WAY }
    }

    operations {
        performance { terminalLoggingIntervalSeconds = 2f }
        eventBus {
            visible = true
            captureEnabled = true
        }
    }

    presets {
        storage("my-game.debug")
        applySavedDefaultOnStartup = true
    }
}
```

The Debug Window is configured through `ui.settingsWindow`; the API name distinguishes it from the Tool Rail. Both UI components are disabled and hidden by default. Their default shortcuts are F3 and F2 respectively, and enabled components must use different keys.

`enabled` controls whether a component exists in the scene. `visibleOnStartup` controls its initial visibility. Runtime visibility is exposed as read-only `isVisible` and changes through the shortcut or the Debug UI itself.

## UI and Debug functionality are independent

Debug Window visibility, Tool Rail visibility, visual diagnostics, tools, and operational services are separate concerns. Hiding either interface does not disable diagnostics, terminal logging, event capture, notifications, or other Debug behavior. A scene may use the runtime API with both interfaces disabled:

```kotlin
strata.debug.visuals.entities.showPath = true
strata.debug.operations.performance.terminalLoggingEnabled = true
```

The Tool Rail selects world-editing and inspection tools and opens their contextual flyouts. The Debug Window edits shared diagnostic and operational settings. Neither is a master Debug switch.

## DEFAULT and startup precedence

Debug startup applies configuration in this order:

1. Engine defaults construct `DebugSettings`.
2. The game's `debug { ... }` block overrides them.
3. A persisted DEFAULT visual configuration overrides the game's visual configuration when storage is configured, saved data exists, and `applySavedDefaultOnStartup` is `true`.

Configure persistence with a game-owned libGDX preferences namespace:

```kotlin
debug {
    presets {
        storage("my-game.debug")
        applySavedDefaultOnStartup = true
    }
}
```

`applySavedDefaultOnStartup` defaults to `true`, but has no effect until `storage(...)` configures a namespace. Strata stores the versioned value under `default-visual-configuration` in that namespace. Missing data leaves the game-defined visuals active. Invalid or unsupported data is reported through the libGDX error log and also falls back to the game-defined visuals.

The loaded DEFAULT is retained even when automatic startup application is disabled. This permits manual application later:

```kotlin
strata.debug.applyDefaultVisualConfiguration()
```

`saveDefaultVisualConfiguration()` captures the current visual configuration, updates the in-memory DEFAULT, and persists it when storage is configured. Without storage it still updates the in-memory DEFAULT for the current run.

DEFAULT captures visual presentation, including the live Performance overlay, Performance History overlay and its ordered graph metrics, their shared refresh interval, world statistics overlay, Event Monitor presentation, and notification position. It deliberately excludes UI availability/visibility, tool settings and selection, terminal logging, performance history recording and capacity, simulation runtime state, Event Bus visibility/capture state, notification activation and queue contents, camera restriction overrides, entity freeze state, and other operational state.

The old `defaultPresetStorage(...)` helper is not part of the current API. Use `presets { storage(...) }`.

## Built-in presets

`applyPreset` applies one of the engine presets immediately:

```kotlin
strata.debug.applyPreset(DebugPreset.RENDERING)
```

| Preset | Main effect |
| --- | --- |
| `OFF` | Turns off preset-managed overlays and visual diagnostics and restores normal world-category visibility |
| `MINIMAL` | Enables the performance overlay and grid |
| `PLACEMENT` | Enables the grid and object occupied-tile/origin diagnostics, including the object fill |
| `ENTITIES` | Enables current-tile, position, path, direction, sprite-bound, and current-tile-fill diagnostics |
| `RENDERING` | Enables performance, sprite-bound, render-order, culling, and camera diagnostics |
| `EVERYTHING` | Enables the broad built-in diagnostic set, world statistics, and simulation controls |

The Debug Window also exposes `DEFAULT` beside these built-ins. DEFAULT applies the last loaded or saved configuration; it is not a `DebugPreset` enum value.

Built-in presets reset the preset-managed visual flags before applying their selection. `OFF` does not disable the Debug Window, Tool Rail, terminal logging, performance history recording, event capture, notifications, or tool configuration. Color choices and other inactive visual parameters remain available for later use.

`resetVisualConfiguration()` restores every captured visual preference to engine defaults. `resetVisualCategory(category)` resets one `DebugVisualCategory`. Both preserve operational and tool state.

## Performance output

```kotlin
strata.debug.operations.performance.apply {
    overlayEnabled = true
    historyOverlayEnabled = true
    overlayRefreshIntervalSeconds = 0.25f
    terminalLoggingEnabled = true
    terminalLoggingIntervalSeconds = 2f
    historyRecording = true
    historyLength = 240
    historyGraphMetrics = listOf(
        DebugPerformanceMetric.FRAME_TIME,
        DebugPerformanceMetric.ENTITIES_DRAWN
    )
}
```

The live **Performance** overlay shows only current renderer statistics with a fixed set of rows. The separate **Performance History** panel is self-contained: its header shows the Recording/Stopped state with **Start**, **Stop**, and **Clear**, the retained sample count, a list of graphs, **Add graph**, and a history **Length** control. Each graph card selects its metric from the dropdown in its upper-right corner, can be removed while another graph remains, and shows current, average, minimum, and maximum values in the metric's unit. Up to six graphs can be shown; the list scrolls when it is taller than the available space. Its default position is on the left, above the Simulation rail, with room reserved for the Tool Rail, tool flyout, Debug Window, and live stats stack. The panel is placed on whole pixels so its bitmap text stays crisp, and it blocks pointer input to world tools underneath.

The Debug Window only toggles the panel through **Overlays → Performance history overlay**. **Overlays** also holds the shared **Overlay refresh** interval, and **Terminal logging** holds the terminal output settings.

Both overlays, terminal logging, and history recording are independently disabled by default. Showing the panel does not start recording. Recording continues with either overlay hidden; stopping or hiding retains captured samples, and clearing does not change the recording state. `startHistoryRecording()`, `stopHistoryRecording()`, and `clearHistory()` remain available alongside the panel's buttons.

Every recorded frame captures all `DebugPerformanceMetric` values on one shared, bounded timeline, so all graphs belong to the same recording session and switching a graph's metric shows that metric's own samples without mixing in the previous metric. Each metric has a `displayName`, a `DebugPerformanceUnit` (`MILLISECONDS`, `FRAMES_PER_SECOND`, or `COUNT`), a `label` such as `Frame time (ms)` or `Entities drawn (count)`, and `format(value)`. All values come from the existing `RenderStats` and frame time:

| Metric | Unit | Source |
|---|---|---|
| `FRAME_TIME` | ms | Frame delta |
| `FRAMES_PER_SECOND` | FPS | Instantaneous `1 / frame delta`; zero for a zero-length frame |
| `RENDER_TIME`, `STATIC_PLAN_TIME`, `DYNAMIC_PLAN_TIME` | ms | World CPU render and render-plan timings |
| `DRAW_CALLS` | count | World batch render calls |
| `GROUND_DRAWN`, `GROUND_TOTAL` | count | Ground terrain sprites drawn / ground tiles in the world |
| `OVERLAYS_DRAWN`, `OVERLAYS_TOTAL` | count | Overlay terrain sprites drawn / non-empty overlay tiles |
| `TERRAIN_CHECKED` | count | Ground and overlay terrain candidates visited by culling |
| `OBJECTS_DRAWN`, `OBJECTS_CHECKED`, `OBJECTS_TOTAL` | count | Placed objects drawn / culling candidates / in the world |
| `ENTITIES_DRAWN`, `ENTITIES_CHECKED`, `ENTITIES_TOTAL` | count | Entities drawn / culling candidates / in the world |
| `PREVIEWS_DRAWN` | count | Placement previews drawn |
| `STATIC_PLAN_UPDATES`, `STATIC_PLAN_CHECKS` | count | Static render-plan updates and relation checks |

`historyGraphMetrics` accepts one to six metrics, top to bottom; changing it never clears captured history. Graphs use a zero baseline and rounded upper bounds with headroom; small changes preserve the scale, spikes expand it immediately, and much smaller values let it shrink. Changing a graph's metric restarts its scale. Frame time and frame rate include subtle 60/30 FPS budget lines when they fit the visible range, and large counts use compact `k`/`M` axis labels. Pixel-width buckets retain chronological minima/maxima and endpoints, preserving spikes without changing recorded history. Empty history shows **No captured samples** at the same graph size.

`overlayRefreshIntervalSeconds` defaults to `0.25f` and controls both overlays; history capture still runs every recorded frame. Clear, graph, metric, recording-state, history-capacity, and graph-size changes refresh a visible chart immediately. `terminalLoggingIntervalSeconds` defaults to `2f`. Both intervals must be finite and positive. `historyLength` defaults to 240, accepts 1–16,384 samples per metric, and retains the newest samples when resized.

Terminal output records completed-world-render metrics and periodically writes a `StrataPerf` libGDX log entry. It reports frame time and FPS, world CPU render time, static and dynamic render-plan work, terrain/object/entity drawn and checked counts, previews, and draw calls. The most recent raw `RenderStats` is readable from `strata.view.renderStats`.

## Visual diagnostics

The following runtime example uses the current nested API:

```kotlin
strata.debug.visuals.apply {
    filter = DebugVisualizationFilter.ALL

    grid.apply {
        enabled = true
        extent = DebugGridExtent.WORLD
        renderLayer = DebugGridRenderLayer.BELOW_OBJECTS
        showBackground = true
    }

    objects.apply {
        showOccupiedTiles = true
        showOriginTile = true
        showOccupiedTileFill = true
    }

    entities.apply {
        showCurrentTile = true
        showPosition = true
        showPath = true
        showCurrentTileFill = true
    }
}
```

Most visual groups with several independent rows do not have generic master switches. In particular, `objects.enabled` and `entities.enabled` do not exist. Enable the object or entity diagnostics you need through their individual `show...` properties. The same granular rule applies to world information, render-order, culling, and camera diagnostics.

Picking has an explicit `enabled` state because its diagnostics collect hover and selection snapshots. `showSpriteBounds` and `showCursorHit` remain configured when Picking is disabled, but they do not collect or render Picking diagnostics until it is re-enabled. Disabling Picking clears its locked diagnostic selection. Inspect, Build, Move, and other Debug tools continue to use the engine's generic picking independently.

`DebugVisualizationFilter.ALL` is the default shared target filter. Other filter values limit supported object, entity, culling, and render-order diagnostics without changing their individual settings.

### Grid

`WORLD` draws the finite world's grid and highlights only an in-world hover. `VISIBLE` draws the logical grid across the visible camera area and can highlight an off-world coordinate. `BELOW_OBJECTS` redraws objects, entities, and previews after the grid; `ABOVE_OBJECTS` leaves the grid above normal world content.

The grid defaults to disabled, `WORLD`, `BELOW_OBJECTS`, line width `1f`, green lines, yellow hover lines, and both fills hidden.

Grid background visibility is controlled by `showBackground` and `showHoverBackground`. `backgroundColor` and `hoverBackgroundColor` are non-null `Color` values. Changing either visibility flag does not discard its color or opacity.

### Object and entity diagnostics

Object diagnostics derive occupied cells from `PlacedObject.occupiedTiles()`. The origin is the stored placement coordinate and may differ from the minimum footprint corner. Sprite bounds use the resolved state/frame and final rendering offsets and size.

All object `show...` flags default to `false` and line width defaults to `1f`. `showOccupiedTileFill` is independent from the non-null `occupiedTileFillColor`, whose default is `(0.2, 0.65, 1, 0.18)`.

Entity diagnostics can show the current tile, exact continuous position, remaining path, direction, sprite bounds, movement trail and vector, next waypoint, speed, and within-tile offset. Enabling the path view reads existing route state and does not run pathfinding. Movement trails can be cleared with `clearMovementTrails()`.

All entity `show...` flags default to `false` and line width defaults to `1f`. `showCurrentTileFill` is independent from the non-null `currentTileFillColor`, whose default is `(0.3, 1, 0.3, 0.16)`. Trail defaults are 120 positions, five seconds, minimum distance `0.02f`, and opacity `0.65f`; movement-vector scale defaults to `0.5f` seconds.

### Other visual groups

`worldInfo` provides tile coordinates, terrain IDs, overlay information, occupancy, missing-visual markers, and the world origin. All are disabled by default; label zoom and count limits default to `1.5f` and 256.

`worldVisibility` hides renderer categories for diagnosis without changing world contents. Ground, overlays, placed objects, entities, and every overlay layer are visible by default. `showAll()` restores normal visibility.

`picking`, `renderOrder`, `culling`, and `camera` diagnostics default to no active visuals. Picking itself and both Picking child visuals default to disabled. Render-order mode defaults to `CALCULATED`, priority focus to `OFF`, selected priority to `0`, heatmap steps to `PER_TILE`, priority alpha to `0.2f`, and geometry line width to `2f`.

## Tools

Build defaults to its existing rectangular left-drag/release gesture (a click
places one object). Right-click Build in the Tool Rail to open settings; **Gesture**
offers **Rectangle** and **Path**. Path uses left-click to start/add waypoints,
cursor movement for live previews, Enter to place, and Escape or **Cancel path**
to cancel. Right-click world-object removal is available in both shapes.
Selection and shape changes, leaving Build, hiding the Tool Rail, or disabling
world picking cancel an unfinished path. Escape cancels a path before acting as
a configured Debug Window shortcut. The separate rail **Path** tool is A*
pathfinding, not path placement. Build uses the normal placement validator and
ordered batch policy, skipping invalid origins. See the [Sandbox demo](../sandbox/README.md).

Paint and Delete share odd square brush sizes from 1 through 9. Both default to size 1, affected-area previews on, and tile borders off. Delete dragging defaults to enabled.

```kotlin
strata.debug.tools.apply {
    paint.apply {
        brushSize = 3
        showTileBorders = true
    }
    delete.apply {
        brushSize = 5
        dragEnabled = false
    }
}
```

The Inspect tool's visual options and entity-animation freeze default to enabled. The Pathfinding tool is available by default in `FOUR_WAY` mode; explored nodes and final path are shown, advanced cost/set diagnostics are hidden, automatic search advances one iteration per update, reached waypoints are consumed, and the entity speed multiplier is `1f`. Label zoom and count limits default to `1.5f` and 128, with up to 2048 rejected transitions retained.

Game rules can be supplied to the Debug Pathfinding tool without coupling them to the UI:

```kotlin
debug {
    tools {
        pathTraversal { world, position -> world.getObjectAt(position) == null }
        pathCost { world, _, to -> terrainCost(world.getTile(to)) }
    }
}
```

`onEntitySpawned` and `onObjectsPlaced` connect Debug tool mutations to game-owned controllers, events, or sound effects.

## Operational services

Simulation controls, world statistics, and Event Bus monitor visibility are disabled by default. Simulation's `freezeVisualAnimations` also defaults to `false`. Notifications are enabled by default, retain up to four messages for three seconds each, and appear at `TOP_CENTER`. `disableCameraRestrictions` defaults to `false`.

Event Bus capture defaults to enabled. `visible` controls only the monitor panel, while `captureEnabled` controls whether published events are appended to the bounded history. Capture can run while the panel is hidden, and changing either setting preserves existing history; the Debug Window's **Clear history** action explicitly removes it. Presentation defaults to eight visible records in newest-first order. Visibility and capture are not restored by DEFAULT.

Debug settings are runtime mutable. Renderers and the Debug runtime read their live settings, so most changes take effect without reattaching a world. UI `enabled` and `visibleOnStartup` values describe scene construction/startup and should be configured in `debug {}`; use the UI shortcuts for runtime visibility.

Debug output diagnoses engine spatial and rendering state. It does not supply game-specific AI, economy, or semantic diagnostics unless the game publishes events or builds those diagnostics itself.

See [Rendering](Rendering.md), [Objects](Objects.md), [Entities](Entities.md), and [Movement and Pathfinding](Movement-and-Pathfinding.md).
