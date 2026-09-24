package com.mefabc24.sandbox

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.mefabc24.strata.ui.StrataUi

class SandboxUi(
    private val ui: StrataUi,
    private val painter: SandboxTerrainPainter
) {

    private val skin = ui.skin

    private lateinit var paintingLabel: Label

    init {
        buildUi()
        sync()
    }

    private fun buildUi() {
        val panel = ui.root.apply {
            top()
            left()
            pad(16f)
        }

        val content = Table(skin).apply {
            background = skin.getDrawable("panel")
            pad(12f)
        }

        val title = Label(
            "Strata UI",
            skin
        )

        paintingLabel = Label(
            "",
            skin
        )

        val togglePaintingButton = TextButton(
            "Toggle Painting",
            skin
        )

        togglePaintingButton.addListener(
            object : ChangeListener() {
                override fun changed(
                    event: ChangeEvent,
                    actor: Actor
                ) {
                    painter.enabled = !painter.enabled
                    sync()
                }
            }
        )

        content.add(title)
            .left()

        content.row()
            .padTop(8f)

        content.add(paintingLabel)
            .left()

        content.row()
            .padTop(8f)

        content.add(togglePaintingButton)
            .width(150f)
            .height(36f)

        panel.add(content)
            .top()
            .left()
    }

    fun sync() {
        paintingLabel.setText(
            "Painting: ${if (painter.enabled) "ON" else "OFF"}"
        )
    }

    companion object {

        fun createSkin(): Skin {
            val skin = Skin()

            val font = BitmapFont()

            skin.add(
                "default-font",
                font
            )

            val pixmap = Pixmap(
                1,
                1,
                Pixmap.Format.RGBA8888
            ).apply {
                setColor(Color.WHITE)
                fill()
            }

            val texture = Texture(pixmap)

            pixmap.dispose()

            skin.add(
                "white",
                texture
            )

            val baseDrawable = TextureRegionDrawable(
                TextureRegion(texture)
            )

            skin.add(
                "default",
                Label.LabelStyle(
                    font,
                    Color.WHITE
                )
            )

            skin.add(
                "default",
                TextButton.TextButtonStyle().apply {
                    this.font = font

                    up = baseDrawable.tint(
                        Color(0.18f, 0.18f, 0.20f, 1f)
                    )

                    over = baseDrawable.tint(
                        Color(0.25f, 0.25f, 0.28f, 1f)
                    )

                    down = baseDrawable.tint(
                        Color(0.12f, 0.12f, 0.14f, 1f)
                    )
                }
            )

            skin.add(
                "panel",
                baseDrawable.tint(
                    Color(0.08f, 0.08f, 0.10f, 0.92f)
                ),
                Drawable::class.java
            )

            return skin
        }
    }
}