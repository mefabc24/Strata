package com.mefabc24.strata.camera

import com.badlogic.gdx.Input

/**
 * Configures the input bindings used to control the camera.
 */
class CameraControls {

    var moveUp: Int = Input.Keys.W
    var moveDown: Int = Input.Keys.S
    var moveLeft: Int = Input.Keys.A
    var moveRight: Int = Input.Keys.D

    var dragButton: Int = Input.Buttons.MIDDLE

    var keyboardMovementEnabled: Boolean = true
    var mouseDraggingEnabled: Boolean = true
    var zoomEnabled: Boolean = true
}