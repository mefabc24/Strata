package com.mefabc24.strata.debug.ui

/**
 * Presents a master-gated feature as independent effective options.
 *
 * Enabling the first option clears latent options left behind while the feature
 * was disabled, so the user's chosen row is the only visualization activated.
 */
internal class GranularDebugFeatureBinding(
    private val readEnabled: () -> Boolean,
    private val writeEnabled: (Boolean) -> Unit,
    private val disableAllOptions: () -> Unit,
    private val anyOptionEnabled: () -> Boolean
) {
    fun readOption(read: () -> Boolean): Boolean = readEnabled() && read()

    fun writeOption(
        value: Boolean,
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ) {
        if (value && !readEnabled()) disableAllOptions()
        if (read() != value) write(value)
        writeEnabled(anyOptionEnabled())
    }
}
