package com.mefabc24.strata.camera

import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CameraBoundsTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `clamp keeps viewport inside bounds`() {
        val camera = camera(viewportWidth = 20f, viewportHeight = 10f)
        val bounds = CameraBounds(0f, 0f, 100f, 80f)

        camera.position.set(-50f, 200f, 0f)
        bounds.clamp(camera)

        assertEquals(10f, camera.position.x)
        assertEquals(75f, camera.position.y)
    }

    @Test
    fun `clamp centers axes covered by the viewport`() {
        val camera = camera(viewportWidth = 200f, viewportHeight = 10f)
        val bounds = CameraBounds(10f, 20f, 90f, 80f)
        camera.position.set(-100f, -100f, 0f)

        bounds.clamp(camera)

        assertEquals(50f, camera.position.x)
        assertEquals(25f, camera.position.y)
    }

    @Test
    fun `edge allowance exposes a controlled part of the viewport`() {
        val camera = camera(viewportWidth = 20f, viewportHeight = 20f)
        val bounds = CameraBounds(0f, 0f, 100f, 100f, edgeAllowance = 0.5f)
        camera.position.set(0f, 100f, 0f)

        bounds.clamp(camera)

        assertEquals(5f, camera.position.x)
        assertEquals(95f, camera.position.y)
    }

    @Test
    fun `move clamps requested movement at viewport aware edges`() {
        val camera = camera(viewportWidth = 20f, viewportHeight = 20f)
        val bounds = CameraBounds(0f, 0f, 100f, 100f)
        camera.position.set(50f, 50f, 0f)

        bounds.move(camera, -100f, 100f)

        assertEquals(10f, camera.position.x)
        assertEquals(90f, camera.position.y)
    }

    @Test
    fun `move does not snap an existing violation or allow it farther out`() {
        val camera = camera(viewportWidth = 20f, viewportHeight = 20f)
        val bounds = CameraBounds(0f, 0f, 100f, 100f)
        camera.position.set(-20f, 120f, 0f)

        bounds.move(camera, -10f, 10f)
        assertEquals(-20f, camera.position.x)
        assertEquals(120f, camera.position.y)

        bounds.move(camera, 5f, -5f)
        assertEquals(-15f, camera.position.x)
        assertEquals(115f, camera.position.y)
    }

    @Test
    fun `move leaves axes unchanged when viewport covers the bounds`() {
        val camera = camera(viewportWidth = 200f, viewportHeight = 200f)
        val bounds = CameraBounds(0f, 0f, 100f, 100f)
        camera.position.set(12f, 34f, 0f)

        bounds.move(camera, 50f, -50f)

        assertEquals(12f, camera.position.x)
        assertEquals(34f, camera.position.y)
    }

    @Test
    fun `constructor rejects inverted bounds and invalid edge allowance`() {
        assertFailsWith<IllegalArgumentException> { CameraBounds(1f, 0f, 0f, 1f) }
        assertFailsWith<IllegalArgumentException> { CameraBounds(0f, 1f, 1f, 0f) }
        for (allowance in listOf(-0.1f, 1.1f, Float.NaN)) {
            assertFailsWith<IllegalArgumentException> {
                CameraBounds(0f, 0f, 1f, 1f, allowance)
            }
        }
    }

    private fun camera(
        viewportWidth: Float,
        viewportHeight: Float
    ) = OrthographicCamera(viewportWidth, viewportHeight).apply {
        zoom = 1f
        update()
    }
}
