package com.mefabc24.strata.input

import com.mefabc24.strata.camera.CameraControls

class ControlsSettings {

    val camera = CameraControls()

    fun camera(configure: CameraControls.() -> Unit) {
        camera.apply(configure)
    }

    val gameplay = GameplayControlsSettings()

    fun gameplay(configure: GameplayControlsSettings.() -> Unit) {
        gameplay.apply(configure)
    }
}