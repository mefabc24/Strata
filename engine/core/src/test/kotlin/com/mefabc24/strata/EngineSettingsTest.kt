package com.mefabc24.strata

import com.badlogic.gdx.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EngineSettingsTest {

    @Test
    fun `default clear color is dark gray`() {
        assertEquals(
            Color(0.1f, 0.1f, 0.1f, 1f),
            EngineSettings().backgroundColor
        )
    }

    @Test
    fun `assigned clear color is copied`() {
        val color = Color(0.2f, 0.3f, 0.4f, 1f)
        val settings = EngineSettings().apply {
            backgroundColor = color
        }

        color.set(Color.RED)

        assertEquals(
            Color(0.2f, 0.3f, 0.4f, 1f),
            settings.backgroundColor
        )
    }

    @Test
    fun `clear color components must be finite`() {
        assertFailsWith<IllegalArgumentException> {
            EngineSettings().backgroundColor = Color(
                Float.NaN,
                0f,
                0f,
                1f
            )
        }

        val settings = EngineSettings()
        settings.backgroundColor.r = Float.POSITIVE_INFINITY

        assertFailsWith<IllegalArgumentException> {
            settings.backgroundColorSnapshot()
        }
    }
}
