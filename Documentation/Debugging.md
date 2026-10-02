# Debugging

`DebugSettings` contains four independent, runtime-mutable groups. Configure initial values in `debug {}` and change them later through `strata.debug`. The Sandbox UI is an example of wiring these live settings to controls.

## Performance output

```kotlin
strata.debug.performance.overlayEnabled = true
strata.debug.performance.terminalLoggingEnabled = true
strata.debug.performance.terminalLoggingIntervalSeconds = 2f
```

The overlay and terminal logger are independent and disabled by default. The Debug Panel's Performance toggle changes only `overlayEnabled`. Terminal logging records the same completed-world-render metrics and periodically writes a `StrataPerf` libGDX log entry. It reports:

- current FPS and frame time average, p95, and maximum;
- world CPU render average and maximum;
- static-plan maximum time, update count, and relation checks;
- dynamic-plan average and maximum;
- terrain, object, and entity drawn/checked counts;
- preview count and draw calls.

Changing `terminalLoggingEnabled` resets the current terminal sample window. `terminalLoggingIntervalSeconds` must be finite and positive. The most recent raw `RenderStats` is also readable from `strata.view.renderStats`.

## Grid overlay

```kotlin
strata.debug.grid.apply {
    enabled = true
    extent = DebugGridExtent.WORLD
    renderLayer = DebugGridRenderLayer.BELOW_OBJECTS
    color = Color(1f, 1f, 1f, 0.4f)
    hoverColor = Color.RED
    lineWidth = 1f
    backgroundColor = Color(1f, 1f, 1f, 0.2f)
    hoverBackgroundColor = Color(1f, 0f, 0f, 0.5f)
}
```

`WORLD` draws the finite world's grid and highlights only an in-world hover. `VISIBLE` draws the logical grid across the visible camera area and can highlight an off-world logical coordinate.

`BELOW_OBJECTS` redraws objects, entities, and previews after the grid so they remain above it. `ABOVE_OBJECTS` leaves the grid above normal world content. Nullable background colors fill regular and hovered top faces; `null` disables that fill. Line width must be finite and positive.

Defaults are disabled, `WORLD`, `BELOW_OBJECTS`, green grid lines, yellow hover lines, width `1f`, and no backgrounds.

## Object diagnostics

```kotlin
strata.debug.objects.apply {
    enabled = true
    showOccupiedTiles = true
    showOriginTile = true
    showSpriteBounds = true
    lineWidth = 1f
    occupiedTileColor = Color.CYAN
    occupiedTileFillColor = Color(0f, 1f, 1f, 0.2f)
    originTileColor = Color.ORANGE
    spriteBoundsColor = Color.MAGENTA
}
```

Occupied tiles come from `PlacedObject.occupiedTiles()`. The origin marker is the object's stored placement `(x, y)`, which may differ from the minimum footprint corner. Sprite bounds use the active state/frame and include global and per-type object offsets and sizing. Set `occupiedTileFillColor = null` to disable fills.

The three display flags default to occupied tiles `true`, origin `true`, sprite bounds `false`.

## Entity diagnostics

```kotlin
strata.debug.entities.apply {
    enabled = true
    showCurrentTile = true
    showPosition = true
    showPath = true
    showDirection = true
    showSpriteBounds = true
    lineWidth = 1f
}
```

Available colors are `currentTileColor`, nullable `currentTileFillColor`, `positionColor`, `pathColor`, `directionColor`, and `spriteBoundsColor`. The exact position marks the continuous ground anchor. The path uses the entity's already-stored remaining waypoints; enabling debug does not run pathfinding. Direction draws a ground-plane line. Bounds use the active state, direction, and animation frame.

Defaults are current tile `true`, position `true`, path `true`, direction `false`, and sprite bounds `false`.

## Runtime behavior

All four setting objects are shared with registered renderers rather than copied, so every property described above can change at runtime. Debug shapes render after the normal world and grid. Settings may be configured before worlds are registered; they take effect on the active view.

Debug output diagnoses engine spatial/render state. It does not display game-specific AI, economy, or semantic visual state unless the game builds that UI/logging itself.

See [Rendering](Rendering.md), [Objects](Objects.md), and [Entities](Entities.md).
