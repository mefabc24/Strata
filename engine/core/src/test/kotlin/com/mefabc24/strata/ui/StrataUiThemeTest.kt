package com.mefabc24.strata.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StrataUiThemeTest {

    @Test
    fun `default theme uses standard scene2d styles`() {
        val theme = StrataUiTheme()

        assertEquals("default", theme.labelStyle)
        assertEquals("default", theme.buttonStyle)
        assertEquals("default", theme.toggleButtonStyle)
        assertNull(theme.panelStyle)
        assertNull(theme.separatorStyle)
        assertEquals(8f, theme.spacing)
    }

    @Test
    fun `theme rejects invalid style names and spacing`() {
        assertFailsWith<IllegalArgumentException> {
            StrataUiTheme(labelStyle = " ")
        }

        assertFailsWith<IllegalArgumentException> {
            StrataUiTheme(panelStyle = "")
        }

        assertFailsWith<IllegalArgumentException> {
            StrataUiTheme(spacing = -1f)
        }
    }

    @Test
    fun `insets create predictable symmetric values`() {
        assertEquals(
            StrataInsets(3f, 3f, 3f, 3f),
            StrataInsets.all(3f)
        )

        assertEquals(
            StrataInsets(4f, 2f, 4f, 2f),
            StrataInsets.symmetric(
                horizontal = 2f,
                vertical = 4f
            )
        )

        assertFailsWith<IllegalArgumentException> {
            StrataInsets(
                top = -1f,
                left = 0f,
                bottom = 0f,
                right = 0f
            )
        }
    }

    @Test
    fun `panel style copy has independent padding values`() {
        val original = StrataPanelStyle(
            background = null,
            padding = StrataInsets.all(4f)
        )

        val copy = StrataPanelStyle(original)
        copy.padLeft = 9f

        assertEquals(4f, original.padLeft)
        assertEquals(9f, copy.padLeft)
    }

    @Test
    fun `separator style requires positive thickness`() {
        assertFailsWith<IllegalArgumentException> {
            StrataSeparatorStyle(
                drawable = TestDrawable,
                thickness = 0f
            )
        }
    }

    private object TestDrawable :
        com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable()
}
