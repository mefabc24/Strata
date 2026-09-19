package com.mefabc24.strata.scene

import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.ControlsSettings

/**
 * Configures an isometric world view.
 */
class IsoViewSettings {

    var tileWidth: Float = 64f
    var tileHeight: Float = 32f

    val camera = CameraSettings()

    fun camera(configure: CameraSettings.() -> Unit) {
        camera.apply(configure)
    }

    val controls = ControlsSettings()

    fun controls(configure: ControlsSettings.() -> Unit) {
        controls.apply(configure)
    }

    /**
     * Null enables automatic terrain sprite height calculation.
     */
    var maxTerrainSpriteHeight: Float? = null
}