package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.ui.StrataExpanderStyle
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class DebugPanelSkinTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `exclusive selections and independent toggles use distinct styles`() {
        val theme = DebugPanelSkin.theme()

        assertNotEquals(theme.selectableButtonStyle, theme.toggleButtonStyle)
    }

    @Test
    fun `debug expander style provides integrated state visuals`() {
        val skin = DebugPanelSkin.create()
        val style = skin.get("debug-expander", StrataExpanderStyle::class.java)

        assertEquals("debug-expander-header", style.headerButtonStyle)
        assertNotNull(style.headerBackground)
        assertNotNull(style.expandedHeaderBackground)
        assertNotNull(style.collapsedIndicator)
        assertNotNull(style.expandedIndicator)

        skin.dispose()
    }
}
