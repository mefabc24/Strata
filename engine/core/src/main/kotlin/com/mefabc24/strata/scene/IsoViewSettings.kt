package com.mefabc24.strata.scene

import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.ControlsSettings
import com.mefabc24.strata.render.RenderingSettings

/**
 * Configures an isometric world view.
 */
class IsoViewSettings {

    val rendering = RenderingSettings()

    fun rendering(configure: RenderingSettings.() -> Unit) {
        rendering.apply(configure)
    }

    val camera = CameraSettings()

    fun camera(configure: CameraSettings.() -> Unit) {
        camera.apply(configure)
    }

    val controls = ControlsSettings()

    fun controls(configure: ControlsSettings.() -> Unit) {
        controls.apply(configure)
    }
}