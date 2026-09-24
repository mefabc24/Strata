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
        assertEquals("default", theme.imageButtonStyle)
        assertEquals("default", theme.selectableImageButtonStyle)
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
            StrataUiTheme(imageButtonStyle = " ")
        }

        for (
            invalid in listOf(
                -1f,
                Float.NaN,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                StrataUiTheme(spacing = invalid)
            }
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

        assertFailsWith<IllegalArgumentException> {
            StrataInsets.all(Float.NaN)
        }

        assertFailsWith<IllegalArgumentException> {
            StrataInsets.symmetric(
                horizontal = Float.POSITIVE_INFINITY,
                vertical = 0f
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
        for (
            invalid in listOf(
                0f,
                -1f,
                Float.NaN,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                StrataSeparatorStyle(
                    drawable = TestDrawable,
                    thickness = invalid
                )
            }
        }
    }

    private object TestDrawable :
        com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable()
}
