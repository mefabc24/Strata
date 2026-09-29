package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.mefabc24.strata.ui.StrataInsets
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
                background = drawable.tint(Color(0.13f, 0.13f, 0.16f, 0.96f)),
                padding = StrataInsets.all(8f)
            ),
            StrataPanelStyle::class.java
        )
        skin.add(
            "debug-separator",
            StrataSeparatorStyle(
                drawable = drawable.tint(Color(0.35f, 0.35f, 0.38f, 1f)),
                thickness = 1f
            ),
            StrataSeparatorStyle::class.java
        )
        return skin
    }
}
