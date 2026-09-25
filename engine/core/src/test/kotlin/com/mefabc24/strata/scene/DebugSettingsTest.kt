package com.mefabc24.strata.scene

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugSettingsTest {

    @Test
    fun `debug grid is disabled by default`() {
        assertFalse(DebugSettings().grid.enabled)
    }

    @Test
    fun `grid block configures debug grid`() {
        val settings = DebugSettings().apply {
            grid {
                enabled = true
            }
        }

        assertTrue(settings.grid.enabled)
    }
}
