package com.mefabc24.strata.ui

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StrataWidgetsTest {

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    private enum class Option {
        FIRST,
        SECOND
    }

    @Test
    fun `selectable buttons reflect group state`() {
        val skin = createSkin()
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        val first = StrataSelectableButton(
            text = "First",
            value = Option.FIRST,
            selectionGroup = group,
            skin = skin
        )

        val second = StrataSelectableButton(
            text = "Second",
            value = Option.SECOND,
            selectionGroup = group,
            skin = skin
        )

        try {
            assertTrue(first.isChecked)
            assertFalse(second.isChecked)

            group.select(Option.SECOND)

            assertFalse(first.isChecked)
            assertTrue(second.isChecked)
        } finally {
            first.detach()
            second.detach()
            skin.dispose()
        }
    }

    @Test
    fun `required selectable button restores its checked state`() {
        val skin = createSkin()
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        val first = StrataSelectableButton(
            text = "First",
            value = Option.FIRST,
            selectionGroup = group,
            skin = skin
        )

        try {
            first.isChecked = false
            first.fire(ChangeListener.ChangeEvent())

            assertEquals(Option.FIRST, group.selected)
            assertTrue(first.isChecked)
        } finally {
            first.detach()
            skin.dispose()
        }
    }

    @Test
    fun `selectable image buttons reflect group state`() {
        val skin = createSkin()
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        val first = StrataSelectableImageButton(
            drawable = BaseDrawable(),
            value = Option.FIRST,
            selectionGroup = group,
            skin = skin
        )

        val second = StrataSelectableImageButton(
            drawable = BaseDrawable(),
            value = Option.SECOND,
            selectionGroup = group,
            skin = skin
        )

        try {
            assertTrue(first.isChecked)
            assertFalse(second.isChecked)

            second.isChecked = true
            second.fire(ChangeListener.ChangeEvent())

            assertEquals(Option.SECOND, group.selected)
            assertFalse(first.isChecked)
            assertTrue(second.isChecked)
        } finally {
            first.detach()
            second.detach()
            skin.dispose()
        }
    }

    @Test
    fun `required selectable image button cannot be unchecked`() {
        val skin = createSkin()
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        val button = StrataSelectableImageButton(
            drawable = BaseDrawable(),
            value = Option.FIRST,
            selectionGroup = group,
            skin = skin
        )

        try {
            button.isChecked = false
            button.fire(ChangeListener.ChangeEvent())

            assertEquals(Option.FIRST, group.selected)
            assertTrue(button.isChecked)
        } finally {
            button.detach()
            skin.dispose()
        }
    }

    @Test
    fun `optional selectable image button can be unchecked`() {
        val skin = createSkin()
        val group = StrataSelectionGroup(
            options = Option.entries,
            initialSelection = Option.FIRST,
            selectionRequired = false
        )

        val button = StrataSelectableImageButton(
            drawable = BaseDrawable(),
            value = Option.FIRST,
            selectionGroup = group,
            skin = skin
        )

        try {
            button.isChecked = false
            button.fire(ChangeListener.ChangeEvent())

            assertEquals(null, group.selected)
            assertFalse(button.isChecked)
        } finally {
            button.detach()
            skin.dispose()
        }
    }

    @Test
    fun `disabled selectable image button ignores clicks`() {
        val skin = createSkin()
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        val button = StrataSelectableImageButton(
            drawable = BaseDrawable(),
            value = Option.SECOND,
            selectionGroup = group,
            skin = skin
        )

        try {
            button.isDisabled = true
            button.clickListener.clicked(InputEvent(), 0f, 0f)

            assertEquals(Option.FIRST, group.selected)
            assertFalse(button.isChecked)
        } finally {
            button.detach()
            skin.dispose()
        }
    }

    @Test
    fun `detached image button stops reflecting group state`() {
        val skin = createSkin()
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        val button = StrataSelectableImageButton(
            drawable = BaseDrawable(),
            value = Option.FIRST,
            selectionGroup = group,
            skin = skin
        )

        try {
            assertTrue(button.isChecked)
            button.detach()
            group.select(Option.SECOND)

            assertTrue(button.isChecked)
        } finally {
            button.detach()
            skin.dispose()
        }
    }

    @Test
    fun `momentary button never exposes a checked state change`() {
        val skin = createSkin()
        var clicks = 0
        var changes = 0

        val button = StrataButton(
            text = "Action",
            skin = skin,
            onClick = { clicks++ }
        )

        button.addListener(
            object : ChangeListener() {
                override fun changed(
                    event: ChangeEvent,
                    actor: Actor
                ) {
                    changes++
                }
            }
        )

        button.listeners.toList()
            .filterIsInstance<ClickListener>()
            .forEach {
                it.clicked(InputEvent(), 0f, 0f)
            }

        assertEquals(1, clicks)
        assertEquals(0, changes)
        assertFalse(button.isChecked)

        skin.dispose()
    }

    @Test
    fun `panel pointer blocking can be disabled`() {
        val skin = Skin()
        val context = StrataUiContext(
            skin = skin,
            theme = StrataUiTheme()
        )

        val panel = StrataPanel(
            context = context,
            styleName = null,
            spacing = 0f,
            paddingOverride = null,
            blocksInput = true
        )

        val inputListener = panel.listeners
            .toList()
            .filterIsInstance<InputListener>()
            .single()

        assertTrue(
            inputListener.touchDown(
                InputEvent(),
                0f,
                0f,
                0,
                0
            )
        )

        assertTrue(
            inputListener.scrolled(
                InputEvent(),
                0f,
                0f,
                0f,
                1f
            )
        )

        panel.blocksInput = false

        assertFalse(
            inputListener.touchDown(
                InputEvent(),
                0f,
                0f,
                0,
                0
            )
        )

        skin.dispose()
    }

    @Test
    fun `row and column preserve their layout semantics`() {
        val skin = Skin()
        val context = StrataUiContext(
            skin = skin,
            theme = StrataUiTheme(spacing = 0f)
        )

        val row = StrataRow(
            context = context,
            spacing = 0f,
            padding = StrataInsets.NONE,
            alignment = 0
        )

        val column = StrataColumn(
            context = context,
            spacing = 0f,
            padding = StrataInsets.NONE,
            alignment = 0
        )

        row.actor(Actor())
        row.actor(Actor())
        column.actor(Actor())
        column.actor(Actor())

        row.pack()
        column.pack()

        assertEquals(1, row.rows)
        assertEquals(2, row.columns)
        assertEquals(2, column.rows)
        assertEquals(1, column.columns)

        context.dispose()
        skin.dispose()
    }

    @Test
    fun `nested layouts and stacks retain ordinary scene2d actors`() {
        val skin = Skin()
        val context = StrataUiContext(
            skin = skin,
            theme = StrataUiTheme(spacing = 0f)
        )

        val outer = StrataColumn(
            context = context,
            spacing = 0f,
            padding = StrataInsets.NONE,
            alignment = 0
        )

        lateinit var customActor: Actor
        val nested = outer.column {
            row {
                customActor = actor(Actor())
            }
        }

        val stack = outer.stack {
            actor(Actor())
            column {
                actor(Actor())
            }
        }

        assertSame(outer, nested.parent)
        assertTrue(customActor.parent is StrataRow)
        assertEquals(2, stack.children.size)
        assertSame(outer, stack.parent)

        context.dispose()
        skin.dispose()
    }

    @Test
    fun `selection controls created through the layout bind to their group`() {
        val skin = createSkin()
        val context = StrataUiContext(
            skin = skin,
            theme = StrataUiTheme()
        )

        val column = StrataColumn(
            context = context,
            spacing = 0f,
            padding = StrataInsets.NONE,
            alignment = 0
        )

        val group = StrataSelectionGroup(
            options = Option.entries
        )

        val textButton = column.selectableButton(
            text = "First",
            value = Option.FIRST,
            group = group
        )

        val imageButton = column.selectableImageButton(
            drawable = BaseDrawable(),
            value = Option.SECOND,
            group = group
        )

        assertTrue(textButton.isChecked)
        assertFalse(imageButton.isChecked)

        group.select(Option.SECOND)

        assertFalse(textButton.isChecked)
        assertTrue(imageButton.isChecked)

        context.dispose()
        skin.dispose()
    }

    @Test
    fun `layout widgets reject non-finite and unsupported dimensions`() {
        val skin = Skin()
        val context = StrataUiContext(
            skin = skin,
            theme = StrataUiTheme()
        )

        assertFailsWith<IllegalArgumentException> {
            StrataColumn(
                context = context,
                spacing = Float.NaN,
                padding = StrataInsets.NONE,
                alignment = 0
            )
        }

        assertFailsWith<IllegalArgumentException> {
            StrataSpacer(spacerWidth = Float.POSITIVE_INFINITY)
        }

        assertFailsWith<IllegalArgumentException> {
            StrataSpacer(spacerHeight = -1f)
        }

        assertFailsWith<IllegalArgumentException> {
            StrataSeparator(
                orientation = StrataSeparatorOrientation.HORIZONTAL,
                style = StrataSeparatorStyle().apply {
                    drawable = BaseDrawable()
                    thickness = Float.NEGATIVE_INFINITY
                }
            )
        }

        val panel = StrataPanel(
            context = context,
            styleName = null,
            spacing = 0f,
            paddingOverride = null,
            blocksInput = true
        )

        assertFailsWith<IllegalArgumentException> {
            panel.setStyle(
                StrataPanelStyle().apply {
                    padLeft = Float.NaN
                }
            )
        }

        context.dispose()
        skin.dispose()
    }

    private fun createSkin(): Skin {
        val font = BitmapFont(
            BitmapFont.BitmapFontData(),
            Array<TextureRegion>().apply {
                add(TextureRegion())
            },
            false
        )

        return Skin().apply {
            add("font", font)
            add(
                "default",
                TextButton.TextButtonStyle().apply {
                    this.font = font
                }
            )

            add(
                "default",
                ImageButton.ImageButtonStyle()
            )
        }
    }
}
