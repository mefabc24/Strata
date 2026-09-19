package com.mefabc24.strata.input

import com.mefabc24.strata.camera.CameraControls

/**
 * Configures camera controls and world input bindings.
 */
class ControlsSettings {

    val camera = CameraControls()

    fun camera(configure: CameraControls.() -> Unit) {
        camera.apply(configure)
    }

    var bindings: List<WorldInputBinding> = emptyList()
}