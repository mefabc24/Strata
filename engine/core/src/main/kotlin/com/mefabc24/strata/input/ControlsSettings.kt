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

    internal fun copy(): ControlsSettings {
        return ControlsSettings().also { copy ->
            val cameraCopy = camera.copy()

            copy.camera.moveUp = cameraCopy.moveUp
            copy.camera.moveDown = cameraCopy.moveDown
            copy.camera.moveLeft = cameraCopy.moveLeft
            copy.camera.moveRight = cameraCopy.moveRight
            copy.camera.dragButton = cameraCopy.dragButton
            copy.camera.keyboardMovementEnabled =
                cameraCopy.keyboardMovementEnabled
            copy.camera.mouseDraggingEnabled =
                cameraCopy.mouseDraggingEnabled
            copy.camera.zoomEnabled = cameraCopy.zoomEnabled

            copy.gameplay.bindings = gameplay.copy().bindings
        }
    }
}
