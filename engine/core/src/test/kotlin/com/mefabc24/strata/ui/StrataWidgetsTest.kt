package com.mefabc24.strata.ui

import com.badlogic.gdx.Files
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StrataWidgetsTest {

    @BeforeTest
    fun installTestFiles() {
        if (Gdx.files == null) {
            Gdx.files = TestFiles
        }
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
        }
    }

    private object TestFiles : Files {
        override fun getFileHandle(
            path: String,
            type: Files.FileType
        ) = FileHandle(path)

        override fun classpath(path: String) =
            getFileHandle(path, Files.FileType.Classpath)

        override fun internal(path: String) =
            getFileHandle(path, Files.FileType.Internal)

        override fun external(path: String) =
            getFileHandle(path, Files.FileType.External)

        override fun absolute(path: String) =
            getFileHandle(path, Files.FileType.Absolute)

        override fun local(path: String) =
            getFileHandle(path, Files.FileType.Local)

        override fun getExternalStoragePath() = ""

        override fun isExternalStorageAvailable() = false

        override fun getLocalStoragePath() = ""

        override fun isLocalStorageAvailable() = false
    }
}
