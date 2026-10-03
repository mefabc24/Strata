package com.mefabc24.strata.debug.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GranularDebugFeatureBindingTest {
    @Test
    fun `disabled feature hides latent options`() {
        var enabled = false
        var first = true
        var second = true
        val binding = binding(
            { enabled }, { enabled = it },
            disableAll = { first = false; second = false },
            anyEnabled = { first || second }
        )

        assertFalse(binding.readOption { first })
        assertFalse(binding.readOption { second })
    }

    @Test
    fun `enabling one granular option clears latent options and activates feature`() {
        var enabled = false
        var first = true
        var second = true
        val binding = binding(
            { enabled }, { enabled = it },
            disableAll = { first = false; second = false },
            anyEnabled = { first || second }
        )

        binding.writeOption(true, { second }) { second = it }

        assertTrue(enabled)
        assertFalse(first)
        assertTrue(second)
    }

    @Test
    fun `disabling the last granular option deactivates feature`() {
        var enabled = true
        var option = true
        val binding = binding(
            { enabled }, { enabled = it },
            disableAll = { option = false },
            anyEnabled = { option }
        )

        binding.writeOption(false, { option }) { option = it }

        assertFalse(enabled)
        assertFalse(option)
    }

    private fun binding(
        readEnabled: () -> Boolean,
        writeEnabled: (Boolean) -> Unit,
        disableAll: () -> Unit,
        anyEnabled: () -> Boolean
    ) = GranularDebugFeatureBinding(
        readEnabled,
        writeEnabled,
        disableAll,
        anyEnabled
    )
}
