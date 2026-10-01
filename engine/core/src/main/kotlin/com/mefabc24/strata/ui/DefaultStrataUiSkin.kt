package com.mefabc24.strata.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

/** Self-contained resources used by the high-level screen UI API. */
internal object DefaultStrataUiSkin {
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
        skin.add("strata-white", texture)
        val white = TextureRegionDrawable(TextureRegion(texture))

        skin.add("default", Label.LabelStyle(font, Color.WHITE))
        skin.add("default", buttonStyle(font, white))
        skin.add("toggle", buttonStyle(font, white).apply {
            checked = white.tint(Color(0.16f, 0.48f, 0.68f, 1f))
            checkedOver = white.tint(Color(0.20f, 0.58f, 0.78f, 1f))
        })
        skin.add("default", ImageButton.ImageButtonStyle().apply {
            up = white.tint(Color(0.18f, 0.20f, 0.24f, 0.96f))
            over = white.tint(Color(0.25f, 0.29f, 0.35f, 0.98f))
            down = white.tint(Color(0.12f, 0.14f, 0.18f, 1f))
            checked = white.tint(Color(0.16f, 0.48f, 0.68f, 1f))
            disabled = white.tint(Color(0.10f, 0.11f, 0.13f, 0.8f))
        })
        skin.add("default", ScrollPane.ScrollPaneStyle().apply {
            vScroll = white.tint(Color(0.08f, 0.09f, 0.11f, 0.8f))
            vScrollKnob = white.tint(Color(0.45f, 0.48f, 0.54f, 0.95f))
        })
        skin.add(
            "panel",
            StrataPanelStyle(
                background = white.tint(Color(0.07f, 0.08f, 0.11f, 0.92f)),
                padding = StrataInsets.all(16f)
            ),
            StrataPanelStyle::class.java
        )
        skin.add(
            "separator",
            StrataSeparatorStyle(
                drawable = white.tint(Color(0.42f, 0.46f, 0.54f, 1f)),
                thickness = 1f
            ),
            StrataSeparatorStyle::class.java
        )
        return skin
    }

    private fun buttonStyle(
        font: BitmapFont,
        white: TextureRegionDrawable
    ) = TextButton.TextButtonStyle().apply {
        this.font = font
        fontColor = Color.WHITE
        up = white.tint(Color(0.18f, 0.20f, 0.24f, 0.96f))
        over = white.tint(Color(0.25f, 0.29f, 0.35f, 0.98f))
        down = white.tint(Color(0.12f, 0.14f, 0.18f, 1f))
        disabled = white.tint(Color(0.10f, 0.11f, 0.13f, 0.8f))
        disabledFontColor = Color(0.52f, 0.54f, 0.58f, 1f)
    }
}
