package com.mefabc24.strata.scene

import com.mefabc24.strata.camera.CameraControls
import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.WorldInputBinding

/**
 * Configures an isometric world view.
 */
class IsoViewSettings {

    var tileWidth: Float = 64f
    var tileHeight: Float = 32f

    /**
     * Configures camera movement, zoom, bounds, and viewport.
     */
    val camera = CameraSettings()

    fun camera(configure: CameraSettings.() -> Unit) {
        camera.apply(configure)
    }

    /**
     * Configures camera input.
     */
    val cameraControls = CameraControls()

    fun controls(configure: CameraControls.() -> Unit) {
        cameraControls.apply(configure)
    }

    var bindings: List<WorldInputBinding> = emptyList()

    /**
     * Null enables automatic terrain sprite height calculation.
     */
    var maxTerrainSpriteHeight: Float? = null
}