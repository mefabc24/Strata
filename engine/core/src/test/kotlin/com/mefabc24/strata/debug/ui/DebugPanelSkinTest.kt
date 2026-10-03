package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.ui.StrataButton
import com.mefabc24.strata.ui.StrataExpanderStyle
import com.mefabc24.strata.ui.StrataPanelStyle
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
        assertEquals(12f, style.contentIndent)
        assertEquals(9f, style.indicatorSize)
        assertEquals(0f, style.contentSpacing)
        assertNotNull(style.headerSeparator)
        assertEquals(1f, style.headerSeparatorThickness)
        assertNull(style.background)

        val settingRow = skin.get("debug-setting-row", StrataPanelStyle::class.java)
        assertNotNull(settingRow.background)

        skin.dispose()
    }

    @Test
    fun `interactive debug controls have distinct normal hover and disabled states`() {
        val skin = DebugPanelSkin.create()
        val action = skin.get("debug-action-card", TextButton.TextButtonStyle::class.java)
        val stepperValue = skin.get("debug-stepper-value", Label.LabelStyle::class.java)

        assertNotNull(action.up)
        assertNotEquals(action.up, action.over)
        assertNotEquals(action.up, action.disabled)

        skin.dispose()
    }

    @Test
    fun `tool rail skin exposes button states and drawable icons`() {
        val skin = DebugPanelSkin.create()
        val style = skin.get(
            "debug-tool-rail-button",
            DebugToolRailButtonStyle::class.java
        )

        assertNotEquals(style.normal, style.hovered)
        assertNotEquals(style.normal, style.selected)
        DebugToolMode.entries.forEach { mode ->
            assertNotNull(skin.getDrawable(debugToolIconName(mode)))
        }
        skin.dispose()
    }

    @Test
    fun `action card invokes its callback from the full button surface`() {
        val skin = DebugPanelSkin.create()
        var clicks = 0
        val card = StrataButton(
            "Clear history",
            skin,
            "debug-action-card"
        ) { clicks++ }

        card.listeners.toList()
            .filterIsInstance<ClickListener>()
            .forEach { it.clicked(InputEvent(), card.width / 2f, card.height / 2f) }

        assertEquals(1, clicks)
        skin.dispose()
    }
}
