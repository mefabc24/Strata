package com.mefabc24.strata.ui

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StrataAdvancedWidgetsTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `responsive columns respect width and limits`() {
        assertEquals(1, responsiveColumnCount(90f, 100f, 8f, 3))
        assertEquals(2, responsiveColumnCount(215f, 100f, 8f, 3))
        assertEquals(3, responsiveColumnCount(1000f, 100f, 8f, 3))
    }

    @Test
    fun `numeric stepper clamps and can synchronize silently`() {
        val changes = mutableListOf<Float>()
        val skin = skin()
        val stepper = StrataNumericStepper(
            "Alpha", skin, value = 0.5f, minimum = 0f,
            maximum = 1f, step = 0.25f, onChanged = changes::add
        )
        repeat(4) { stepper.increment() }
        assertEquals(1f, stepper.value)
        repeat(8) { stepper.decrement() }
        assertEquals(0f, stepper.value)
        stepper.sync(0.75f)
        assertEquals(0.75f, stepper.value)
        assertEquals(listOf(0.75f, 1f, 0.75f, 0.5f, 0.25f, 0f), changes)
        skin.dispose()
    }

    @Test
    fun `numeric stepper centers its displayed value`() {
        val skin = skin()
        val stepper = StrataNumericStepper(
            "Alpha", skin, value = 1.25f, minimum = 0f,
            maximum = 2f, step = 0.25f
        )

        val valueLabel = stepper.children
            .filterIsInstance<Label>()
            .first { it.text.toString() == "1.25" }

        assertEquals(Align.center, valueLabel.labelAlign)
        skin.dispose()
    }

    private fun skin(): Skin {
        val font = BitmapFont(
            BitmapFont.BitmapFontData(),
            Array<TextureRegion>().apply { add(TextureRegion()) },
            false
        )
        return Skin().apply {
            add("font", font)
            add("default", Label.LabelStyle(font, null))
            add("default", TextButton.TextButtonStyle().apply { this.font = font })
        }
    }
}
