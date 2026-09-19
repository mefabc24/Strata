package com.mefabc24.strata.input

import com.mefabc24.strata.camera.CameraControls

class ControlsSettings {

    val camera = CameraControls()

    fun camera(configure: CameraControls.() -> Unit) {
        camera.apply(configure)
    }

    val worldInput = WorldInputSettings()

    fun worldInput(configure: WorldInputSettings.() -> Unit) {
        worldInput.apply(configure)
    }
}