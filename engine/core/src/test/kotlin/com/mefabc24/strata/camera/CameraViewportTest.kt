package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CameraViewportTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `fixed height resize adapts width and preserves camera state`() {
        val camera = OrthographicCamera(1f, 1f).apply {
            position.set(30f, 40f, 0f)
            zoom = 1.5f
        }
        val viewport = CameraViewport(
            camera,
            mode = ViewportMode.FIXED_HEIGHT,
            virtualHeight = 600f
        )

        viewport.resize(1600, 900)

        assertEquals(1066.6666f, camera.viewportWidth, absoluteTolerance = 0.001f)
        assertEquals(600f, camera.viewportHeight)
        assertEquals(1.5f, camera.zoom)
        assertEquals(30f, camera.position.x)
        assertEquals(40f, camera.position.y)
    }

    @Test
    fun `pixel based resize uses actual dimensions`() {
        val camera = OrthographicCamera()
        val viewport = CameraViewport(camera, mode = ViewportMode.PIXEL_BASED)

        viewport.resize(1234, 567)

        assertEquals(1234f, camera.viewportWidth)
        assertEquals(567f, camera.viewportHeight)
    }

    @Test
    fun `invalid resize dimensions leave camera unchanged`() {
        val camera = OrthographicCamera(320f, 200f).apply {
            position.set(7f, 9f, 0f)
            zoom = 2f
        }
        val viewport = CameraViewport(camera)

        viewport.resize(0, 100)
        viewport.resize(100, -1)

        assertEquals(320f, camera.viewportWidth)
        assertEquals(200f, camera.viewportHeight)
        assertEquals(7f, camera.position.x)
        assertEquals(9f, camera.position.y)
        assertEquals(2f, camera.zoom)
    }

    @Test
    fun `resize reapplies bounds by default`() {
        val camera = OrthographicCamera(10f, 10f).apply {
            position.set(-100f, 200f, 0f)
        }
        val viewport = CameraViewport(
            camera,
            mode = ViewportMode.PIXEL_BASED,
            bounds = CameraBounds(0f, 0f, 100f, 100f)
        )

        viewport.resize(20, 20)

        assertEquals(10f, camera.position.x)
        assertEquals(90f, camera.position.y)
    }

    @Test
    fun `bounds can be disabled for debug navigation`() {
        val camera = OrthographicCamera(10f, 10f).apply {
            position.set(-100f, 200f, 0f)
        }
        val viewport = CameraViewport(
            camera,
            mode = ViewportMode.PIXEL_BASED,
            bounds = CameraBounds(0f, 0f, 100f, 100f)
        ).apply { boundsEnabled = false }

        viewport.resize(20, 20)

        assertEquals(-100f, camera.position.x)
        assertEquals(200f, camera.position.y)
    }

    @Test
    fun `virtual height must be finite and positive`() {
        for (height in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                CameraViewport(OrthographicCamera(), virtualHeight = height)
            }
        }
    }
}
