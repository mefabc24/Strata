package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.mefabc24.strata.ui.StrataInsets
import com.mefabc24.strata.ui.StrataExpanderStyle
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataSeparatorStyle
import com.mefabc24.strata.ui.StrataUiTheme

/** Small self-contained skin owned by the built-in debug panel. */
internal object DebugPanelSkin {
    fun theme() = StrataUiTheme(
        labelStyle = "default",
        buttonStyle = "default",
        toggleButtonStyle = "debug-toggle",
        selectableButtonStyle = "debug-selection",
        imageButtonStyle = "default",
        selectableImageButtonStyle = "default",
        panelStyle = "debug-panel",
        separatorStyle = "debug-separator",
        spacing = 6f
    )

    fun create(): Skin {
        val skin = Skin()
        val font = BitmapFont()
        skin.add("default-font", font)
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }
        val texture = Texture(pixmap)
        pixmap.dispose()
        skin.add("debug-white", texture)
        val drawable = TextureRegionDrawable(TextureRegion(texture))
        skin.add("default", Label.LabelStyle(font, Color.WHITE))
        skin.add("title", Label.LabelStyle(font, Color(0.55f, 0.82f, 1f, 1f)))
        skin.add("debug-key", Label.LabelStyle(font, Color(0.55f, 0.82f, 1f, 1f)))
        skin.add("default", TextButton.TextButtonStyle().apply {
            this.font = font
            fontColor = Color.WHITE
            up = drawable.tint(Color(0.18f, 0.18f, 0.20f, 1f))
            over = drawable.tint(Color(0.25f, 0.25f, 0.28f, 1f))
            down = drawable.tint(Color(0.12f, 0.12f, 0.14f, 1f))
            checked = drawable.tint(Color(0.16f, 0.45f, 0.68f, 1f))
            checkedOver = drawable.tint(Color(0.20f, 0.55f, 0.78f, 1f))
            disabled = drawable.tint(Color(0.11f, 0.11f, 0.12f, 1f))
            disabledFontColor = Color(0.5f, 0.5f, 0.52f, 1f)
        })
        skin.add("default", TextField.TextFieldStyle().apply {
            this.font = font
            fontColor = Color.WHITE
            messageFontColor = Color(0.55f, 0.56f, 0.60f, 1f)
            background = drawable.tint(Color(0.13f, 0.13f, 0.15f, 1f))
            focusedBackground = drawable.tint(Color(0.18f, 0.22f, 0.27f, 1f))
            cursor = drawable.tint(Color.WHITE)
            selection = drawable.tint(Color(0.16f, 0.45f, 0.68f, 1f))
        })
        skin.add("debug-selection", TextButton.TextButtonStyle().apply {
            this.font = font
            fontColor = Color(0.82f, 0.84f, 0.88f, 1f)
            up = drawable.tint(Color(0.18f, 0.18f, 0.20f, 1f))
            over = drawable.tint(Color(0.25f, 0.25f, 0.28f, 1f))
            down = drawable.tint(Color(0.12f, 0.12f, 0.14f, 1f))
            checked = drawable.tint(Color(0.12f, 0.39f, 0.63f, 1f))
            checkedOver = drawable.tint(Color(0.16f, 0.48f, 0.73f, 1f))
            checkedFontColor = Color.WHITE
            disabled = drawable.tint(Color(0.10f, 0.10f, 0.12f, 1f))
            disabledFontColor = Color(0.45f, 0.46f, 0.49f, 1f)
        })
        skin.add("debug-toggle", TextButton.TextButtonStyle().apply {
            this.font = font
            fontColor = Color(0.88f, 0.88f, 0.90f, 1f)
            up = drawable.tint(Color(0.27f, 0.14f, 0.16f, 1f))
            over = drawable.tint(Color(0.36f, 0.19f, 0.21f, 1f))
            down = drawable.tint(Color(0.18f, 0.09f, 0.11f, 1f))
            checked = drawable.tint(Color(0.16f, 0.48f, 0.29f, 1f))
            checkedOver = drawable.tint(Color(0.20f, 0.58f, 0.35f, 1f))
            checkedFontColor = Color.WHITE
            disabled = drawable.tint(Color(0.11f, 0.11f, 0.12f, 1f))
            disabledFontColor = Color(0.5f, 0.5f, 0.52f, 1f)
        })
        skin.add("debug-expander-header", TextButton.TextButtonStyle().apply {
            this.font = font
            fontColor = Color(0.88f, 0.89f, 0.92f, 1f)
            up = drawable.tint(Color(1f, 1f, 1f, 0f))
            over = drawable.tint(Color(1f, 1f, 1f, 0.06f))
            down = drawable.tint(Color(0f, 0f, 0f, 0.12f))
            disabledFontColor = Color(0.5f, 0.5f, 0.52f, 1f)
        })
        skin.add("default", ImageButton.ImageButtonStyle().apply {
            up = drawable.tint(Color(0.18f, 0.18f, 0.20f, 1f))
            over = drawable.tint(Color(0.25f, 0.25f, 0.28f, 1f))
            down = drawable.tint(Color(0.12f, 0.12f, 0.14f, 1f))
            checked = drawable.tint(Color(0.16f, 0.45f, 0.68f, 1f))
            checkedOver = drawable.tint(Color(0.20f, 0.55f, 0.78f, 1f))
            disabled = drawable.tint(Color(0.11f, 0.11f, 0.12f, 1f))
        })
        skin.add("default", ScrollPane.ScrollPaneStyle().apply {
            vScroll = drawable.tint(Color(0.10f, 0.10f, 0.12f, 0.85f))
            vScrollKnob = drawable.tint(Color(0.42f, 0.42f, 0.46f, 0.95f))
        })
        val dropdownListStyle = List.ListStyle().apply {
            this.font = font
            fontColorSelected = Color.WHITE
            fontColorUnselected = Color(0.82f, 0.84f, 0.88f, 1f)
            selection = drawable.tint(Color(0.12f, 0.39f, 0.63f, 1f))
            background = drawable.tint(Color(0.10f, 0.10f, 0.12f, 1f))
        }
        skin.add("default", dropdownListStyle)
        skin.add("default", SelectBox.SelectBoxStyle().apply {
            this.font = font
            fontColor = Color.WHITE

            background = drawable.tint(Color(0.23f, 0.24f, 0.28f, 1f))
            backgroundOver = drawable.tint(Color(0.31f, 0.33f, 0.38f, 1f))
            backgroundOpen = drawable.tint(Color(0.18f, 0.20f, 0.24f, 1f))

            scrollStyle = skin.get("default", ScrollPane.ScrollPaneStyle::class.java)
            listStyle = dropdownListStyle
        })
        skin.add(
            "debug-panel",
            StrataPanelStyle(
                background = drawable.tint(Color(0.08f, 0.08f, 0.10f, 0.94f)),
                padding = StrataInsets.all(10f)
            ),
            StrataPanelStyle::class.java
        )
        skin.add(
            "debug-event-card",
            StrataPanelStyle(
                background = drawable.tint(Color(0.18f, 0.18f, 0.22f, 0.98f)),
                padding = StrataInsets.all(8f)
            ),
            StrataPanelStyle::class.java
        )
        skin.add(
            "debug-setting-row",
            StrataPanelStyle(
                background = drawable.tint(Color(0.135f, 0.14f, 0.155f, 0.98f)),
                padding = StrataInsets.NONE
            ),
            StrataPanelStyle::class.java
        )
        val collapsedChevron = createChevronDrawable(skin, "right", expanded = false)
        val expandedChevron = createChevronDrawable(skin, "down", expanded = true)
        skin.add(
            "debug-expander",
            StrataExpanderStyle(
                background = null,
                padding = StrataInsets.NONE
            ).apply {
                headerButtonStyle = "debug-expander-header"
                headerBackground = drawable.tint(Color(0.055f, 0.058f, 0.066f, 1f))
                expandedHeaderBackground = drawable.tint(Color(0.085f, 0.09f, 0.105f, 1f))
                collapsedIndicator = collapsedChevron
                expandedIndicator = expandedChevron
                contentIndent = 12f
                indicatorSize = 9f
                headerPadLeft = 8f
                headerPadRight = 6f
                indicatorSpacing = 5f
                contentSpacing = 0f
                headerSeparator = drawable.tint(Color(0.25f, 0.26f, 0.29f, 0.85f))
                headerSeparatorThickness = 1f
            },
            StrataExpanderStyle::class.java
        )
        skin.add(
            "debug-separator",
            StrataSeparatorStyle(
                drawable = drawable.tint(Color(0.25f, 0.26f, 0.29f, 0.85f)),
                thickness = 1f
            ),
            StrataSeparatorStyle::class.java
        )
        return skin
    }

    private fun createChevronDrawable(
        skin: Skin,
        name: String,
        expanded: Boolean
    ): TextureRegionDrawable {
        val pixmap = Pixmap(9, 9, Pixmap.Format.RGBA8888).apply {
            setColor(Color.CLEAR)
            fill()
            setColor(Color(0.72f, 0.76f, 0.82f, 1f))
            if (expanded) {
                drawLine(1, 3, 4, 6)
                drawLine(4, 6, 7, 3)
            } else {
                drawLine(2, 1, 6, 4)
                drawLine(6, 4, 2, 7)
            }
        }
        val texture = Texture(pixmap)
        pixmap.dispose()
        skin.add("debug-chevron-$name", texture)
        return TextureRegionDrawable(TextureRegion(texture))
    }
}
