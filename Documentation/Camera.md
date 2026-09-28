# Camera

Strata creates an orthographic camera and controller when a world is attached. Scene configuration establishes a snapshot; the attached view exposes runtime control through `strata.view.camera`, `strata.view.cameraController`, and `cameraController.controls`.

## Scene configuration

```kotlin
camera {
    moveSpeed = 500f
    zoomSpeed = 0.1f
    minZoom = 0.25f
    maxZoom = 3f
    zoomMode = ZoomMode.WORLD_BASED
    zoomAnchor = ZoomAnchor.CURSOR
    viewportMode = ViewportMode.FIXED_HEIGHT
    virtualHeight = 720f
    cameraPadding = 100f
    zoomEdgeAllowance = 0.3f
    worldFill = 0.85f
}

controls {
    camera {
        moveUp = Input.Keys.W
        moveDown = Input.Keys.S
        moveLeft = Input.Keys.A
        moveRight = Input.Keys.D
        dragButton = Input.Buttons.MIDDLE
        keyboardMovementEnabled = true
        mouseDraggingEnabled = true
        zoomEnabled = true
    }
}
```

These values are the current defaults except `zoomEdgeAllowance`, whose default is `0f`.

## Movement and dragging

Keyboard movement is measured in world units per second and multiplied by current zoom, so its apparent screen speed remains useful across zoom levels. The default WASD directions move in screen/world axes, not logical tile axes.

Middle-button dragging pans by the cursor's world-space delta. Camera bounds are derived from the projected world, `cameraPadding`, and terrain visual height. When the visible viewport is larger than an allowed axis, that axis is centered instead of pannable.

At runtime:

```kotlin
strata.view.cameraController.enabled = false
strata.view.cameraController.controls.mouseDraggingEnabled = false
strata.view.cameraController.moveSpeed = 350f
```

Disabling the controller cancels an active drag. Gameplay input and camera input are separate; disable the relevant processor for the desired effect.

## Zoom

Scrolling changes `camera.zoom` by `amountY * zoomSpeed`. Smaller zoom values show a closer view.

`ZoomAnchor.CURSOR` adjusts the camera so the world point under the cursor remains fixed. `CENTER` keeps the camera center fixed.

`ZoomMode.FIXED` clamps to `minZoom..maxZoom`. `ZoomMode.WORLD_BASED` calculates the maximum zoom-out from projected world bounds, viewport size, and `worldFill`; it uses at least `minZoom` and centers the world at the fitted limit. In world-based mode, the configured `maxZoom` is not the zoom-out limit.

`zoomEdgeAllowance` is a fraction from `0f` to `1f` used when clamping the camera after zoom/resize. It allows that fraction of a half-viewport to extend beyond the zoom bounds. Regular panning still uses the camera bounds and padding.

`cameraController.fitWorld()` is valid only in `WORLD_BASED` mode. `refreshZoomBounds()` reapplies limits after viewport changes and is called by the view on resize.

## Viewport modes and resizing

`FIXED_HEIGHT` preserves `virtualHeight` world units and derives width from aspect ratio. At constant zoom, changing window size without changing aspect ratio preserves relative on-screen scale.

`PIXEL_BASED` uses actual window dimensions as viewport dimensions. A larger window shows more world while sprite size stays roughly constant in pixels at the same zoom.

`StrataGame.resize` forwards to the view automatically. Non-positive sizes are ignored.

See [World and Coordinates](World-and-Coordinates.md) and [Rendering](Rendering.md).
