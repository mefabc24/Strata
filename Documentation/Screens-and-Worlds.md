# Screens and multiple worlds

One `Strata` runtime can register multiple game-owned worlds and multiple logical screens. Assets, content registries, audio, events, lighting, debug settings, and simulation timing remain shared by the runtime.

## Register worlds

Register worlds after the runtime is created, normally in `StrataGame.onReady()`:

```kotlin
private val surfaceId = WorldId("surface")
private val undergroundId = WorldId("underground")

override fun onReady() {
    strata.worlds.register(surfaceId, createSurfaceWorld()) { tile ->
        (tile as GameTile).terrain
    }
    strata.worlds.register(undergroundId, createUndergroundWorld()) { tile ->
        (tile as GameTile).terrain
    }
}
```

The game owns each logical `World`. Strata creates and owns one view and optional placement controller for every registration. Those presentation objects are retained while inactive, which preserves camera state and avoids rebuilding render resources on a switch.

Use `activate` to display a world directly:

```kotlin
strata.worlds.activate(undergroundId)
val activeWorld = strata.worlds.activeWorld
```

`deactivate()` hides the current world without removing it. `remove(id)` disposes the engine-owned view and returns the still game-owned `World`. Runtime disposal releases all remaining views but never disposes logical worlds.

Only the active world advances entity movement by default. Set `strata.worlds.inactiveWorldPolicy = InactiveWorldPolicy.UPDATE` when background worlds should also simulate. Inactive views never process input, render, or update their cameras.

## Register screens

Screens are logical presentation states. Register them after their referenced worlds:

```kotlin
private val menuId = ScreenId("main-menu")
private val gameplayId = ScreenId("gameplay")
private val settingsId = ScreenId("settings")

strata.screens {
    register(menuId) {
        ui {
            column(alignment = StrataAlignment.CENTER) {
                label("My Game")
                button("Play") { navigate(gameplayId) }
                button("Settings") { navigate(settingsId) }
            }
        }
    }

    register(gameplayId) {
        world(surfaceId)
        ui {
            button("Pause") { showOverlay("pause") }
        }
    }

    register(settingsId) {
        ui {
            button("Back") { back() }
        }
    }
}

strata.screens.navigate(menuId)
```

A screen may define UI, a world, both, or neither. Its UI and `onCreate` callback are created lazily on first activation. The UI remains allocated across navigation and is disposed when the screen is removed or the runtime ends.

Lifecycle callbacks run in this order:

1. UI construction, then `onCreate`, on first use only.
2. `onActivate` whenever the screen enters the visible stack.
3. Real-time UI updates and rendering while visible.
4. `onDeactivate` whenever the screen leaves the visible stack.
5. `onDispose` before its retained UI is disposed.

`navigate` replaces the visible stack and records the previous base screen in history. `back` dismisses the top overlay first, then returns through history. Repeated navigation to the current screen with the same parameters is a no-op. Activation parameters are available through `ScreenContext.parameters`.

## Overlays and input

Show a registered screen above the current presentation:

```kotlin
strata.screens.showOverlay("pause")
strata.screens.dismissOverlay()
```

Visible layers render in this order: active world, base UI, overlay UIs from bottom to top, then the debug UI. Input travels in the reverse UI order before debug world tools and active-world input.

An overlay blocks all input below it by default, including input over empty UI space. Pass `blocksInput = false` for a non-modal overlay. UI-only overlays preserve the currently active world, including a world selected directly through `strata.worlds.activate(...)`.

Underlying screens remain visible and receive real-time UI updates while covered. Hidden screens receive no update, render, or input calls. Only the selected world view receives camera, hover, placement, render, and world-input work.

## UI resources and escape hatches

The parameterless `ui {}` screen API uses an engine-owned default skin. Labels, text buttons, panels, layouts, resource-path images, `StrataAlignment`, and navigation callbacks require no libGDX imports:

```kotlin
ui {
    image("ui/logo.png")
    panel {
        button("Continue") { navigate(gameplayId) }
    }
}
```

`StrataUiTheme` maps semantic style roles. Advanced users may call `ui(skin, theme) { ... }`; the game then retains ownership of the supplied Scene2D `Skin`. `StrataUi.skin`, `stage`, `actor(...)`, `Drawable` overloads, raw alignment overloads, and `cell {}` remain available as lower-level escape hatches.

The older `strata.attachWorld(...)` and `strata.createUi(...)` calls remain available for simple single-world applications. They use a reserved `WorldId("default")` and a compatibility UI layer outside screen navigation.
