package com.mefabc24.strata.camera

/**
 * Configures a camera and its viewport.
 *
 * Settings are applied when the world view is created.
 */
data class CameraSettings(
    var moveSpeed: Float = 500f,
    var zoomSpeed: Float = 0.1f,

    var minZoom: Float = 0.25f,
    var maxZoom: Float = 3f,

    var zoomMode: ZoomMode = ZoomMode.WORLD_BASED,
    var zoomAnchor: ZoomAnchor = ZoomAnchor.CURSOR,

    var viewportMode: ViewportMode = ViewportMode.FIXED_HEIGHT,
    var virtualHeight: Float = 720f,

    var cameraPadding: Float = 100f,
    var zoomEdgeAllowance: Float = 0f,
    var worldFill: Float = 0.85f
) {

    internal fun validate() {
        require(moveSpeed.isFinite() && moveSpeed >= 0f) {
            "Camera movement speed must be finite and non-negative."
        }

        require(zoomSpeed.isFinite() && zoomSpeed >= 0f) {
            "Camera zoom speed must be finite and non-negative."
        }

        require(minZoom.isFinite() && minZoom > 0f) {
            "Minimum zoom must be finite and positive."
        }

        require(maxZoom.isFinite() && maxZoom >= minZoom) {
            "Maximum zoom must be finite and >= minimum zoom."
        }

        require(virtualHeight.isFinite() && virtualHeight > 0f) {
            "Virtual viewport height must be finite and positive."
        }

        require(cameraPadding.isFinite() && cameraPadding >= 0f) {
            "Camera padding must be finite and non-negative."
        }

        require(
            zoomEdgeAllowance.isFinite() &&
                    zoomEdgeAllowance in 0f..1f
        ) {
            "Zoom edge allowance must be between 0 and 1."
        }

        require(worldFill.isFinite() && worldFill > 0f && worldFill <= 1f) {
            "World fill must be between 0 (exclusive) and 1."
        }
    }
}