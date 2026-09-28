package com.mefabc24.strata.debug.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugControlBindingsTest {
    @Test
    fun `sync reads authoritative state without rebuilding controls`() {
        var setting = false
        var displayed = true
        val bindings = DebugControlBindings()
        bindings += { displayed = setting }

        bindings.sync()
        assertFalse(displayed)
        setting = true
        bindings.sync()
        assertTrue(displayed)
    }
}
