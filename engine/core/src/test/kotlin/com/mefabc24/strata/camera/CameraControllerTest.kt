package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CameraControllerTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

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

    @Test
    fun `drag gesture accepts only configured button pointer and release`() {
        val controller = controller(camera())
        controller.controls.dragButton = Input.Buttons.MIDDLE

        assertFalse(controller.inputProcessor.touchDown(10, 10, 0, Input.Buttons.LEFT))
        assertTrue(controller.inputProcessor.touchDown(10, 10, 2, Input.Buttons.MIDDLE))
        assertFalse(controller.inputProcessor.touchDown(10, 10, 3, Input.Buttons.MIDDLE))
        assertFalse(controller.inputProcessor.touchDragged(20, 20, 1))
        assertTrue(controller.inputProcessor.touchDragged(20, 20, 2))
        assertFalse(controller.inputProcessor.touchUp(20, 20, 1, Input.Buttons.MIDDLE))
        assertFalse(controller.inputProcessor.touchUp(20, 20, 2, Input.Buttons.LEFT))
        assertTrue(controller.inputProcessor.touchUp(20, 20, 2, Input.Buttons.MIDDLE))
        assertFalse(controller.inputProcessor.touchUp(20, 20, 2, Input.Buttons.MIDDLE))
    }

    @Test
    fun `disabling controller cancels active drag and rejects input`() {
        val controller = controller(camera())
        val button = controller.controls.dragButton
        assertTrue(controller.inputProcessor.touchDown(10, 10, 0, button))

        controller.enabled = false

        assertFalse(controller.inputProcessor.touchDragged(20, 20, 0))
        assertFalse(controller.inputProcessor.touchUp(20, 20, 0, button))
        assertFalse(controller.inputProcessor.scrolled(0f, 1f))
    }

    @Test
    fun `disabling mouse dragging during a gesture cancels it`() {
        val controller = controller(camera())
        val button = controller.controls.dragButton
        assertTrue(controller.inputProcessor.touchDown(10, 10, 0, button))

        controller.controls.mouseDraggingEnabled = false
        controller.update(0.1f)
        controller.controls.mouseDraggingEnabled = true

        assertFalse(controller.inputProcessor.touchDragged(20, 20, 0))
        assertFalse(controller.inputProcessor.touchUp(20, 20, 0, button))
    }

    @Test
    fun `scroll respects zoom controls and clamps fixed limits`() {
        val camera = camera()
        val controller = controller(camera)

        assertTrue(controller.inputProcessor.scrolled(0f, 100f))
        assertEquals(2f, camera.zoom)
        assertTrue(controller.inputProcessor.scrolled(0f, -100f))
        assertEquals(0.5f, camera.zoom)

        controller.controls.zoomEnabled = false
        assertFalse(controller.inputProcessor.scrolled(0f, 1f))
        assertEquals(0.5f, camera.zoom)
    }

    @Test
    fun `fit world requires world based mode`() {
        assertFailsWith<IllegalArgumentException> {
            controller(camera()).fitWorld()
        }
    }

    @Test
    fun `world based fit centers world and honors fill fraction`() {
        val camera = OrthographicCamera(100f, 50f)
        val world = Rectangle(-20f, 10f, 200f, 100f)
        val controller = CameraController(
            camera,
            CameraSettings(
                minZoom = 0.25f,
                zoomMode = ZoomMode.WORLD_BASED,
                zoomAnchor = ZoomAnchor.CENTER,
                worldFill = 0.5f
            ),
            worldZoomBounds = world
        )

        controller.fitWorld()

        assertEquals(4f, camera.zoom)
        assertEquals(80f, camera.position.x)
        assertEquals(60f, camera.position.y)
    }

    @Test
    fun `world based mode requires positive world bounds`() {
        val settings = CameraSettings(zoomMode = ZoomMode.WORLD_BASED)
        assertFailsWith<IllegalArgumentException> {
            CameraController(OrthographicCamera(), settings)
        }
        assertFailsWith<IllegalArgumentException> {
            CameraController(
                OrthographicCamera(),
                settings,
                worldZoomBounds = Rectangle(0f, 0f, 0f, 10f)
            )
        }
    }

    @Test
    fun `unrestricted zoom retains a small positive floor`() {
        val camera = camera()
        val controller = controller(camera)
        controller.unrestricted = true

        controller.zoom(Float.NEGATIVE_INFINITY)

        assertEquals(0.0001f, camera.zoom)
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
