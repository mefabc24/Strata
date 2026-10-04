package com.mefabc24.strata.render.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WorldInformationDebugTest {
    @Test
    fun `label stride bounds dense visible ranges`() {
        assertEquals(1, worldDebugLabelStride(0, 256))
        assertEquals(1, worldDebugLabelStride(256, 256))
        assertEquals(2, worldDebugLabelStride(257, 256))
        assertEquals(4, worldDebugLabelStride(4096, 256))
    }

    @Test
    fun `world labels stop at the configured zoom`() {
        assertTrue(worldDebugLabelsVisible(1.5f, 1.5f))
        assertFalse(worldDebugLabelsVisible(1.51f, 1.5f))
        assertFalse(worldDebugLabelsVisible(Float.NaN, 1.5f))
    }

    @Test
    fun `label stride rejects invalid bounds`() {
        assertFailsWith<IllegalArgumentException> { worldDebugLabelStride(-1, 10) }
        assertFailsWith<IllegalArgumentException> { worldDebugLabelStride(10, 0) }
    }
}
