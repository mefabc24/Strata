package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.utils.Array
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DebugToolRailTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `left and right clicks remain independent`() {
        val selected = mutableListOf<DebugToolMode>()
        val settings = mutableListOf<DebugToolMode>()
        val skin = skin()
        val button = button(skin, selected::add, settings::add).apply {
            setSize(68f, 56f)
        }
        val listener = button.listeners.filterIsInstance<InputListener>().single()

        click(listener, button, Input.Buttons.RIGHT)
        assertEquals(emptyList(), selected)
        assertEquals(listOf(DebugToolMode.PAINT), settings)

        click(listener, button, Input.Buttons.LEFT)
        assertEquals(listOf(DebugToolMode.PAINT), selected)
        assertEquals(listOf(DebugToolMode.PAINT), settings)
        skin.dispose()
    }

    @Test
    fun `settings state opens switches closes and ignores empty tools`() {
        val state = DebugToolRailState(
            setOf(DebugToolMode.PAINT, DebugToolMode.PATHFINDING)
        )

        assertEquals(DebugToolMode.PAINT, state.toggleSettings(DebugToolMode.PAINT))
        assertEquals(
            DebugToolMode.PATHFINDING,
            state.toggleSettings(DebugToolMode.PATHFINDING)
        )
        assertNull(state.toggleSettings(DebugToolMode.PATHFINDING))
        assertNull(state.toggleSettings(DebugToolMode.NONE))
    }

    @Test
    fun `flyout aligns to its button and clamps within the viewport`() {
        val aligned = debugToolFlyoutBounds(
            viewportWidth = 1280f,
            viewportHeight = 720f,
            railRight = 68f,
            anchorTop = 600f,
            preferredWidth = 320f,
            preferredHeight = 240f
        )
        assertEquals(DebugToolFlyoutBounds(76f, 360f, 320f, 240f), aligned)

        val clamped = debugToolFlyoutBounds(
            viewportWidth = 640f,
            viewportHeight = 300f,
            railRight = 68f,
            anchorTop = 40f,
            preferredWidth = 320f,
            preferredHeight = 500f,
            rightInset = 260f
        )
        assertEquals(76f, clamped.x)
        assertEquals(296f, clamped.width)
        assertEquals(284f, clamped.height)
        assertEquals(8f, clamped.y)
    }

    private fun click(
        listener: InputListener,
        button: DebugToolRailButton,
        mouseButton: Int
    ) {
        val event = InputEvent()
        listener.touchDown(event, 34f, 28f, 0, mouseButton)
        listener.touchUp(event, 34f, 28f, 0, mouseButton)
    }

    private fun button(
        skin: Skin,
        onSelected: (DebugToolMode) -> Unit,
        onSettings: (DebugToolMode) -> Unit
    ): DebugToolRailButton {
        val drawable = BaseDrawable()
        return DebugToolRailButton(
            DebugToolMode.PAINT,
            "Paint",
            drawable,
            skin,
            DebugToolRailButtonStyle(
                drawable,
                BaseDrawable(),
                BaseDrawable(),
                BaseDrawable(),
                BaseDrawable()
            ),
            onSelected,
            onSettings
        )
    }

    private fun skin(): Skin {
        val font = BitmapFont(
            BitmapFont.BitmapFontData(),
            Array<TextureRegion>().apply { add(TextureRegion()) },
            false
        )
        return Skin().apply {
            add("default", Label.LabelStyle(font, null))
        }
    }
}
