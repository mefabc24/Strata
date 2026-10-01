# UI

Strata's UI layer is a Kotlin construction API over libGDX Scene2D. It supplies lifecycle integration, common layouts and controls, theme mapping, selection helpers, and input priority. Screen UI uses engine-owned default resources, so ordinary game UI requires no libGDX imports. Scene2D remains available as an advanced escape hatch.

## Lifecycle and ownership

Define UI as part of a registered screen, normally in `onReady()`:

```kotlin
override fun onReady() {
    strata.screens.register("menu") {
        ui {
            panel {
                label("My Game")
                button("Play") { navigate("gameplay") }
            }
        }
    }
}
```

Each screen may own one UI. Strata creates and disposes its `Stage`, updates it while the screen is visible, draws visible screen UIs in stack order, and resizes its `ScreenViewport`. UI is created on first activation and retained across navigation.

The default screen UI skin and resource-path image textures are engine-owned. Advanced code may call `ui(customSkin, theme) { ... }`; the caller then owns that skin. The compatibility `strata.createUi(customSkin) { ... }` API remains available for a single persistent UI outside screen navigation.

## Root and layouts

`StrataUi.root` is a top-left-aligned `StrataColumn` that fills the stage. It does not consume input in empty areas. Builders exist on both `StrataUi` and nested `StrataLayout` values:

```kotlin
column(padding = StrataInsets.all(12f)) {
    label("Selected object")
    row {
        button("Place") { placeSelected() }
        button("Cancel") { cancelTool() }
    }
    grid(columns = 3) {
        // Controls fill rows left-to-right.
    }
    expander("Advanced", expanded = false) {
        toggleButton("Show grid") { enabled ->
            strata.debug.grid.enabled = enabled
        }
    }
}
```

Use `StrataAlignment` to align layouts without libGDX constants. `image("ui/icon.png")` and the resource-path `imageButton` overload load classpath images whose textures are disposed with the UI. Raw Scene2D alignment integers, `Drawable` overloads, `actor`, `cell`, `skin`, and `stage` remain available for advanced customization.

Available containers are vertical `column`, horizontal `row`, fixed-column `grid`, overlaid `stack`, collapsible `expander`, and vertical `panel`. `actor(customActor)` adds any Scene2D actor.

Layouts are regular Scene2D `Table` actors. Call `.cell { ... }` immediately after adding an actor to configure its `Cell`. Helpers `hugX`, `hugY`, `hug`, `fillAvailableX`, `fillAvailableY`, `fillAvailable`, `fixedWidth`, `fixedHeight`, and `fixedSize` cover common sizing choices.

## Panels and input

Panels use an optional `StrataPanelStyle` from the skin and block pointer down/scroll over their full bounds by default, including padding:

```kotlin
panel(
    styleName = "toolbar",
    padding = StrataInsets.symmetric(horizontal = 12f, vertical = 8f),
    blocksInput = true
) {
    label("World tools")
}
```

Set `blocksInput = false` for a decorative overlay. The UI stage is registered before world input, so consumed panel/control events do not click through to world bindings. Empty root space does not block the world.

## Theme and skin styles

`StrataUiTheme` stores style names and default spacing; it owns no resources. It maps label, button, toggle button, image button, selectable image button, panel, and separator roles.

Text controls expect normal Scene2D `Label.LabelStyle` and `TextButton.TextButtonStyle`. Image controls expect `ImageButton.ImageButtonStyle`. Add Strata-specific styles with their explicit types:

```kotlin
skin.add(
    "toolbar",
    StrataPanelStyle(
        background = panelDrawable,
        padding = StrataInsets.all(12f)
    ),
    StrataPanelStyle::class.java
)

skin.add(
    "toolbar",
    StrataSeparatorStyle(separatorDrawable, thickness = 1f),
    StrataSeparatorStyle::class.java
)
```

Calling `separator()` requires `theme.separatorStyle`; otherwise pass a concrete style or style name. Panel and separator drawables remain skin-owned.

## Controls and selection

Builders provide labels, momentary text/image buttons, toggle buttons, separators, spacers, and value-bound selectable text/image buttons.

```kotlin
val terrainGroup = selectionGroup(
    options = TerrainType.entries,
    initialSelection = TerrainType.GRASS
) { selected ->
    painter.terrain = selected
}

row {
    for (terrain in TerrainType.entries) {
        selectableButton(
            text = terrain.name,
            value = terrain,
            group = terrainGroup
        )
    }
}
```

A required `selectionGroup` always selects one option, defaulting to the first when no initial value is supplied. `optionalSelectionGroup` may start and remain `null`; `clearSelection()` works only for optional groups. Options are fixed, non-null, and unique.

Game code may call `group.select(value)` directly. Callbacks fire only on subsequent changes, not at registration time. Builder-created selection controls and callbacks are detached when the UI is disposed. When constructing `StrataSelectableButton` or `StrataSelectableImageButton` directly, call `detach()` if its lifetime is shorter than the group.

## Sandbox boundary

`SandboxUi` is application code. It uses engine primitives to build tool tabs, catalogs from terrain/object registry entries, and live debug controls. Its concrete toolbar, modes, labels, and sync policy are examples, not reusable Strata API.

See [Input and Picking](Input-and-Picking.md) and [Debugging](Debugging.md).
