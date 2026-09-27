package com.mefabc24.strata.scene

import com.badlogic.gdx.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugSettingsTest {

    @Test
    fun `debug grid is disabled by default`() {
        assertFalse(DebugSettings().grid.enabled)
        assertEquals(DebugGridExtent.WORLD, DebugSettings().grid.extent)
    }

    @Test
    fun `grid block configures debug grid`() {
        val settings = DebugSettings().apply {
            grid {
                enabled = true
                extent = DebugGridExtent.VISIBLE
            }
        }

        assertTrue(settings.grid.enabled)
        assertEquals(DebugGridExtent.VISIBLE, settings.grid.extent)
        assertEquals(DebugGridExtent.VISIBLE, settings.grid.copy().extent)
    }

    @Test
    fun `grid colors are copied on assignment and access`() {
        val supplied = Color(1f, 0.5f, 0.25f, 0.75f)
        val settings = DebugGridSettings().apply {
            color = supplied
            backgroundColor = supplied
        }

        supplied.a = 0.1f
        settings.color.a = 0.2f
        settings.backgroundColor?.a = 0.3f

        assertEquals(0.75f, settings.color.a)
        assertEquals(0.75f, settings.backgroundColor?.a)
    }

    @Test
    fun `grid line width rejects invalid values`() {
        val settings = DebugGridSettings()

        for (invalid in listOf(
            0f,
            -1f,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        )) {
            assertFailsWith<IllegalArgumentException> {
                settings.lineWidth = invalid
            }
        }
    }
}
