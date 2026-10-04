package com.mefabc24.strata.lighting

import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.world.EntityPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LightingTest {

    @Test
    fun `default lighting leaves rendering unchanged`() {
        val lighting = Lighting()

        assertFalse(lighting.enabled)
        assertEquals(1f, lighting.ambientIntensity)
        assertEquals(Color.WHITE, lighting.ambientColor)
        assertTrue(lighting.pointLights.isEmpty())
    }

    @Test
    fun `point light radius must be finite and positive`() {
        for (radius in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                light(radius = radius)
            }
        }

        val light = light()
        assertFailsWith<IllegalArgumentException> {
            light.radius = Float.NEGATIVE_INFINITY
        }
        assertEquals(2f, light.radius)
    }

    @Test
    fun `non-finite coordinates intensities and colors are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            EntityPosition(Float.NaN, 1f)
        }
        assertFailsWith<IllegalArgumentException> {
            EntityPosition(1f, Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            light(intensity = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            light(intensity = Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            light(intensity = -0.1f)
        }
        assertFailsWith<IllegalArgumentException> {
            light(color = Color(Float.NaN, 1f, 1f, 1f))
        }

        val lighting = Lighting()
        assertFailsWith<IllegalArgumentException> {
            lighting.ambientIntensity = Float.NaN
        }
        assertFailsWith<IllegalArgumentException> {
            lighting.ambientIntensity = -0.1f
        }
        assertFailsWith<IllegalArgumentException> {
            lighting.ambientColor = Color.WHITE.cpy().apply {
                g = Float.NEGATIVE_INFINITY
            }
        }
    }

    @Test
    fun `point lights can be added removed and cleared`() {
        val lighting = Lighting()
        val first = lighting.addPointLight(
            EntityPosition(1.5f, 2.5f),
            radius = 2f,
            intensity = 1f,
            color = Color.RED
        )
        val second = lighting.addPointLight(
            EntityPosition(3.5f, 4.5f),
            radius = 3f,
            intensity = 0.5f,
            color = Color.BLUE
        )

        assertEquals(2, lighting.pointLights.size)
        assertSame(first, lighting.pointLights[0])
        assertSame(second, lighting.pointLights[1])
        assertTrue(lighting.remove(first))
        assertFalse(lighting.remove(first))
        assertEquals(listOf(second), lighting.pointLights)

        lighting.clearPointLights()
        assertTrue(lighting.pointLights.isEmpty())
    }

    @Test
    fun `ambient and point light colors are defensively copied`() {
        val ambient = Color(0.2f, 0.3f, 0.4f, 1f)
        val point = Color(0.8f, 0.6f, 0.2f, 1f)
        val lighting = Lighting().apply {
            ambientIntensity = 0.35f
            ambientColor = ambient
        }
        val light = lighting.addPointLight(
            EntityPosition(2.5f, 3.5f),
            radius = 2f,
            intensity = 0.75f,
            color = point
        )

        ambient.set(Color.RED)
        point.set(Color.BLUE)
        lighting.ambientColor.set(Color.GREEN)
        light.color.set(Color.GREEN)

        assertEquals(0.35f, lighting.ambientIntensity)
        assertEquals(Color(0.2f, 0.3f, 0.4f, 1f), lighting.ambientColor)
        assertEquals(Color(0.8f, 0.6f, 0.2f, 1f), light.color)
    }

    @Test
    fun `point lights support validated runtime changes`() {
        val light = light()

        light.enabled = false
        light.position = EntityPosition(7.25f, 8.75f)
        light.radius = 4f
        light.intensity = 1.5f
        light.color = Color.CYAN

        assertFalse(light.enabled)
        assertEquals(EntityPosition(7.25f, 8.75f), light.position)
        assertEquals(4f, light.radius)
        assertEquals(1.5f, light.intensity)
        assertEquals(Color.CYAN, light.color)
    }

    @Test
    fun `point light limit is rejected clearly`() {
        val lighting = Lighting()

        repeat(Lighting.MAX_POINT_LIGHTS) { index ->
            lighting.addPointLight(
                EntityPosition(index.toFloat(), 0f),
                radius = 1f,
                intensity = 1f,
                color = Color.WHITE
            )
        }

        val failure = assertFailsWith<IllegalStateException> {
            lighting.addPointLight(
                EntityPosition(100f, 100f),
                radius = 1f,
                intensity = 1f,
                color = Color.WHITE
            )
        }

        assertEquals(
            "Lighting supports at most ${Lighting.MAX_POINT_LIGHTS} point lights.",
            failure.message
        )
        assertEquals(Lighting.MAX_POINT_LIGHTS, lighting.pointLights.size)
    }

    private fun light(
        radius: Float = 2f,
        intensity: Float = 1f,
        color: Color = Color.WHITE
    ): PointLight {
        return Lighting().addPointLight(
            position = EntityPosition(1.5f, 1.5f),
            radius = radius,
            intensity = intensity,
            color = color
        )
    }
}
