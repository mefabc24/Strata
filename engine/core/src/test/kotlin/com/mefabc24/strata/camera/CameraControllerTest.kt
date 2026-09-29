package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CameraControllerTest {
    @Test
    fun `unrestricted mode bypasses pan bounds`() {
        val camera = camera()
        val controller = controller(camera)

        controller.panBy(100f, 0f)
        assertEquals(95f, camera.position.x)

        controller.unrestricted = true
        controller.panBy(100f, 0f)

        assertEquals(195f, camera.position.x)
    }

    @Test
    fun `unrestricted mode bypasses configured zoom limits`() {
        val camera = camera()
        val controller = controller(camera)
        controller.unrestricted = true

        controller.zoom(-2f)
        assertTrue(camera.zoom < controller.minZoom)

        controller.zoom(10f)
        assertTrue(camera.zoom > controller.maxZoom)
    }

    @Test
    fun `leaving unrestricted mode restores zoom and pan constraints`() {
        val camera = camera()
        val controller = controller(camera)
        controller.unrestricted = true
        controller.panBy(500f, 500f)
        controller.zoom(10f)

        controller.unrestricted = false

        assertEquals(controller.maxZoom, camera.zoom)
        assertEquals(90f, camera.position.x)
        assertEquals(90f, camera.position.y)
    }

    private fun camera() = OrthographicCamera(10f, 10f).apply {
        position.set(50f, 50f, 0f)
        zoom = 1f
        update()
    }

    private fun controller(camera: OrthographicCamera): CameraController {
        val bounds = CameraBounds(0f, 0f, 100f, 100f)
        return CameraController(
            camera = camera,
            settings = CameraSettings(
                zoomSpeed = 1f,
                minZoom = 0.5f,
                maxZoom = 2f,
                zoomMode = ZoomMode.FIXED,
                zoomAnchor = ZoomAnchor.CENTER
            ),
            bounds = bounds,
            zoomBounds = bounds
        )
    }
}
