package com.mefabc24.strata.debug.ui

import kotlin.test.Test
import kotlin.test.assertNotEquals

class DebugPanelSkinTest {
    @Test
    fun `exclusive selections and independent toggles use distinct styles`() {
        val theme = DebugPanelSkin.theme()

        assertNotEquals(theme.selectableButtonStyle, theme.toggleButtonStyle)
    }
}
