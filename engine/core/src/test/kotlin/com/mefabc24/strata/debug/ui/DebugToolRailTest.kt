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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `active tool changes close settings without coupling right click state`() {
        val state = DebugToolRailState(
            setOf(DebugToolMode.PAINT, DebugToolMode.PATHFINDING),
            activeMode = DebugToolMode.PAINT
        )

        assertEquals(DebugToolMode.PATHFINDING, state.toggleSettings(DebugToolMode.PATHFINDING))
        assertFalse(state.syncActiveTool(DebugToolMode.PAINT))
        assertEquals(DebugToolMode.PATHFINDING, state.settingsMode)

        assertTrue(state.syncActiveTool(DebugToolMode.MOVE))
        assertNull(state.settingsMode)
    }

    @Test
    fun `availability keeps universal tools and filters content tools`() {
        assertEquals(
            listOf(
                DebugToolMode.NONE,
                DebugToolMode.INSPECT,
                DebugToolMode.MOVE,
                DebugToolMode.DELETE,
                DebugToolMode.PATHFINDING,
                DebugToolMode.FREE_CAMERA
            ),
            availableDebugToolModes(false, false, false)
        )
        assertEquals(
            listOf(
                DebugToolMode.NONE,
                DebugToolMode.INSPECT,
                DebugToolMode.MOVE,
                DebugToolMode.BUILD,
                DebugToolMode.DELETE,
                DebugToolMode.PAINT,
                DebugToolMode.SPAWN,
                DebugToolMode.PATHFINDING,
                DebugToolMode.FREE_CAMERA
            ),
            availableDebugToolModes(true, true, true)
        )
    }

    @Test
    fun `long tool names use compact rail labels`() {
        assertEquals("Camera", debugToolRailLabel(DebugToolMode.FREE_CAMERA))
        assertEquals("Inspect", debugToolRailLabel(DebugToolMode.INSPECT))
    }

    @Test
    fun `configurable tools expose concise ordered flyout sections`() {
        assertEquals(emptyList(), debugToolFlyoutSectionTitles(DebugToolMode.NONE))
        assertEquals(
            listOf("Selection", "Preview", "Status"),
            debugToolFlyoutSectionTitles(DebugToolMode.BUILD)
        )
        assertEquals(
            listOf("Brush", "Terrain", "Target", "Status"),
            debugToolFlyoutSectionTitles(DebugToolMode.PAINT)
        )
        assertEquals(
            listOf("Selection", "Preview", "Status"),
            debugToolFlyoutSectionTitles(DebugToolMode.SPAWN)
        )
        assertEquals(
            listOf("Search", "Visualization", "Diagnostic search", "Traversal", "Status"),
            debugToolFlyoutSectionTitles(DebugToolMode.PATHFINDING)
        )
        assertEquals(
            listOf("Movement", "Visualization", "Status"),
            debugToolFlyoutSectionTitles(DebugToolMode.FREE_CAMERA)
        )
    }

    @Test
    fun `every configurable flyout has unique sections ending in status`() {
        DebugToolMode.entries.filterNot { it == DebugToolMode.NONE }.forEach { mode ->
            val sections = debugToolFlyoutSectionTitles(mode)

            assertTrue(sections.isNotEmpty(), "$mode should expose settings content")
            assertEquals(sections.distinct(), sections, "$mode section names should be unique")
            assertEquals("Status", sections.last(), "$mode should finish with contextual status")
        }
    }

    @Test
    fun `supported mouse presses are consumed before world input`() {
        val skin = skin()
        val button = button(skin, {}, {}).apply { setSize(68f, 56f) }
        val listener = button.listeners.filterIsInstance<InputListener>().single()
        val leftEvent = InputEvent()

        assertTrue(listener.touchDown(leftEvent, 34f, 28f, 0, Input.Buttons.LEFT))
        assertTrue(leftEvent.isStopped)
        assertFalse(listener.touchDown(InputEvent(), 34f, 28f, 0, Input.Buttons.MIDDLE))
        skin.dispose()
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
            add("debug-tool-label", Label.LabelStyle(font, null))
        }
    }
}
