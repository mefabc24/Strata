
# Debugging

`DebugSettings` provides runtime-mutable configuration for debug tools, visual diagnostics, performance monitoring, and developer UI.

Configure initial values in `debug {}` and change them later through `strata.debug`. Visual settings can also be modified through the Debug Window and restored using presets.

## Performance output

```kotlin
strata.debug.performance.overlayEnabled = true
strata.debug.performance.terminalLoggingEnabled = true
strata.debug.performance.terminalLoggingIntervalSeconds = 2f
```

The overlay and terminal logger are independent and disabled by default. The Debug Window's Performance toggle changes only `overlayEnabled`. Terminal logging records the same completed-world-render metrics and periodically writes a `StrataPerf` libGDX log entry. It reports:

- current FPS and frame time average, p95, and maximum;
- world CPU render average and maximum;
- static-plan maximum time, update count, and relation checks;
- dynamic-plan average and maximum;
- terrain, object, and entity drawn/checked counts;
- preview count and draw calls.

Changing `terminalLoggingEnabled` resets the current terminal sample window. `terminalLoggingIntervalSeconds` must be finite and positive. The most recent raw `RenderStats` is also readable from `strata.view.renderStats`.

## Paint and Delete brushes

```kotlin
strata.debug.paint.apply {
    brushSize = 3
    showBrushPreview = true
    showTileBorders = false
}

strata.debug.delete.apply {
    brushSize = 5
    dragEnabled = true
    showBrushPreview = true
    showTileBorders = false
}
```

Brush sizes are odd square side lengths from `1` through `9`. Paint and Delete use the same clipped tile geometry and fill gaps between sampled drag positions. Paint always supports continuous strokes; Delete dragging is optional. Each affected tile is processed once per stroke.

Brush previews follow the cursor, show only in-world affected tiles, and do not modify the world. `showTileBorders` controls whether individual affected tiles receive an outline. It is disabled by default, leaving only the translucent area fill.

## Grid overlay

```kotlin
strata.debug.grid.apply {
    enabled = true
    extent = DebugGridExtent.WORLD
    renderLayer = DebugGridRenderLayer.BELOW_OBJECTS
    color = Color(1f, 1f, 1f, 0.4f)
    hoverColor = Color.RED
    lineWidth = 1f
    showBackground = true
    showHoverBackground = true
    backgroundColor = Color(1f, 1f, 1f, 0.2f)
    hoverBackgroundColor = Color(1f, 0f, 0f, 0.5f)
}
```

`WORLD` draws the finite world's grid and highlights only an in-world hover. `VISIBLE` draws the logical grid across the visible camera area and can highlight an off-world logical coordinate.

`BELOW_OBJECTS` redraws objects, entities, and previews after the grid so they remain above it. `ABOVE_OBJECTS` leaves the grid above normal world content.

`enabled` controls the overall grid visualization. `showBackground` and `showHoverBackground` independently control regular and hovered tile fills.

Background colors remain configurable even when their corresponding fill is disabled. Disabling a fill does not discard its configured color or opacity.

Line width must be finite and positive.

Defaults are disabled, `WORLD`, `BELOW_OBJECTS`, green grid lines, yellow hover lines, width `1f`, and both background fills disabled.

## Object diagnostics

```kotlin
strata.debug.objects.apply {
    showOccupiedTiles = true
    showOriginTile = true
    showSpriteBounds = true
    showOccupiedTileFill = true
    lineWidth = 1f
    occupiedTileColor = Color.CYAN
    occupiedTileFillColor = Color(0f, 1f, 1f, 0.2f)
    originTileColor = Color.ORANGE
    spriteBoundsColor = Color.MAGENTA
}
```

Object diagnostics are controlled through individual visualization properties. No additional global enable flag is required.

Occupied tiles come from `PlacedObject.occupiedTiles()`. The origin marker is the object's stored placement `(x, y)`, which may differ from the minimum footprint corner.

Sprite bounds use the active state/frame and include global and per-type object offsets and sizing.

`showOccupiedTileFill` independently controls the translucent occupied-tile fill. Disabling it does not discard the configured `occupiedTileFillColor`, allowing its color and opacity to be adjusted while the visualization is hidden.

All object visualization flags, including `showOccupiedTileFill`, are disabled by default.

## Entity diagnostics

```kotlin
strata.debug.entities.apply {
    showCurrentTile = true
    showPosition = true
    showPath = true
    showDirection = true
    showSpriteBounds = true
    showCurrentTileFill = true
    currentTileFillColor = Color(0.3f, 1f, 0.3f, 0.16f)
    lineWidth = 1f
}
```

Entity diagnostics are controlled through individual visualization properties. No additional global enable flag is required.

Available colors are `currentTileColor`, `currentTileFillColor`, `positionColor`, `pathColor`, `directionColor`, and `spriteBoundsColor`.

`showCurrentTileFill` controls the translucent fill of the entity's current tile independently of `currentTileFillColor`. Disabling the fill preserves its configured color and opacity.

The exact position marks the continuous ground anchor. The path uses the entity's already-stored remaining waypoints; enabling path visualization does not run pathfinding.

Direction draws a ground-plane line. Sprite bounds use the active state, direction, and animation frame.

All entity visualization flags, including `showCurrentTileFill`, are disabled by default.

## Runtime behavior

Debug settings remain runtime-mutable. Registered renderers access their corresponding live configuration, so changes take effect without recreating the world or renderer.

Visual diagnostic settings are independent of the Debug Window and Tool Rail visibility. Both interfaces can be hidden without disabling the underlying debug functionality.

Optional fills use separate visibility flags and color properties. Colors can be modified while their visualization is disabled and are retained when the visualization is enabled again.

Debug shapes render after normal world content, respecting the configured grid render layer where applicable.

Debug output diagnoses engine spatial/render state. It does not display game-specific AI, economy, or semantic visual state unless the game builds that UI/logging itself.

See [Rendering](Rendering.md), [Objects](Objects.md), and [Entities](Entities.md).
