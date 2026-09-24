package com.mefabc24.strata

import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.scene.StrataScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class EngineSettingsTest {

    private enum class Terrain {
        GRASS
    }

    private enum class SoundCategory {
        EFFECT
    }

    @Test
    fun `default clear color is dark gray`() {
        assertEquals(
            Color(0.1f, 0.1f, 0.1f, 1f),
            EngineSettings().backgroundColor
        )
    }

    @Test
    fun `assigned clear color is copied`() {
        val color = Color(0.2f, 0.3f, 0.4f, 1f)
        val settings = EngineSettings().apply {
            backgroundColor = color
        }

        color.set(Color.RED)

        assertEquals(
            Color(0.2f, 0.3f, 0.4f, 1f),
            settings.backgroundColor
        )
    }

    @Test
    fun `clear color components must be finite`() {
        assertFailsWith<IllegalArgumentException> {
            EngineSettings().backgroundColor = Color(
                Float.NaN,
                0f,
                0f,
                1f
            )
        }

        val settings = EngineSettings()
        settings.backgroundColor.r = Float.POSITIVE_INFINITY

        assertFailsWith<IllegalArgumentException> {
            settings.backgroundColorSnapshot()
        }
    }

    @Test
    fun `scene game provides persistent default engine settings`() {
        val game = DefaultSceneGame()
        val settings = game.engineSettings

        settings.backgroundColor = Color.BLUE

        assertSame(settings, game.engineSettings)
        assertEquals(Color.BLUE, game.engineSettings.backgroundColor)
    }

    @Test
    fun `custom game explicitly owns persistent engine settings`() {
        val game = CustomGame()
        val settings = game.engineSettings

        settings.backgroundColor = Color.GREEN

        assertSame(settings, game.engineSettings)
        assertEquals(Color.GREEN, game.engineSettings.backgroundColor)
    }

    @Test
    fun `engine captures configured settings at construction`() {
        val game = CustomGame().apply {
            engineSettings.backgroundColor =
                Color(0.2f, 0.4f, 0.6f, 1f)
        }

        val engine = StrataEngine(game)
        game.engineSettings.backgroundColor = Color.RED

        assertEquals(
            Color(0.2f, 0.4f, 0.6f, 1f),
            engine.backgroundColor
        )
    }

    private class DefaultSceneGame :
        StrataSceneGame<Terrain, SoundCategory>() {

        override fun createScene(): StrataScene<Terrain, SoundCategory> {
            error("Scene creation is not needed for this test.")
        }
    }

    private class CustomGame : StrataGame {
        override val engineSettings = EngineSettings()

        override fun create() = Unit
        override fun update(delta: Float) = Unit
        override fun render() = Unit
        override fun dispose() = Unit
        override fun resize(width: Int, height: Int) = Unit
    }
}
