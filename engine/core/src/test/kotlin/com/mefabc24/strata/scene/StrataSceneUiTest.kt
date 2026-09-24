package com.mefabc24.strata.scene

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.Viewport
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.testing.defaultValue
import com.mefabc24.strata.testing.proxy
import com.mefabc24.strata.ui.StrataUi
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StrataSceneUiTest {

    private enum class Terrain {
        GRASS
    }

    private enum class SoundCategory {
        EFFECT
    }

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `scene owns a ui lifecycle without requiring a world view`() {
        val inputState = TestGdxEnvironment.install()
        val stage = TrackingStage()
        val skin = TrackingSkin()
        val scene = sceneWith(stage)

        val ui = scene.createUi(skin) {
            actor(Actor())
        }

        assertSame(ui, scene.ui)
        assertSame(skin, ui.skin)
        assertSame(stage, ui.stage)
        assertTrue(inputState.inputProcessor === scene.input.processor)

        scene.update(0.25f)
        scene.render()
        scene.resize(1024, 768)

        assertEquals(listOf(0.25f), stage.actDeltas)
        assertEquals(1, stage.drawCalls)
        assertEquals(1024, stage.viewport.screenWidth)
        assertEquals(768, stage.viewport.screenHeight)

        assertFailsWith<IllegalStateException> {
            scene.createUi(TrackingSkin())
        }

        scene.dispose()
        scene.dispose()

        assertTrue(stage.disposed)
        assertFalse(skin.disposed)
        assertNull(inputState.inputProcessor)

        assertFailsWith<IllegalStateException> {
            ui.update(0f)
        }
    }

    @Test
    fun `scene remains valid with neither optional layer attached`() {
        val inputState = TestGdxEnvironment.install()
        val stage = TrackingStage()
        val scene = sceneWith(stage)

        scene.update(0f)
        scene.render()
        scene.resize(640, 480)

        assertNull(inputState.inputProcessor)
        assertEquals(emptyList(), stage.actDeltas)
        assertEquals(0, stage.drawCalls)

        scene.dispose()
        assertFalse(stage.disposed)
    }

    @Test
    fun `failed ui configuration disposes the stage without installing input`() {
        val inputState = TestGdxEnvironment.install()
        val stage = TrackingStage()
        val scene = sceneWith(stage)

        assertFailsWith<IllegalStateException> {
            scene.createUi(TrackingSkin()) {
                error("configuration failed")
            }
        }

        assertTrue(stage.disposed)
        assertNull(inputState.inputProcessor)

        scene.dispose()
    }

    @Test
    fun `ui rejects invalid update deltas`() {
        val stage = TrackingStage()
        val ui = StrataUi(
            skin = Skin(),
            theme = com.mefabc24.strata.ui.StrataUiTheme(),
            stage = stage
        )

        for (
            invalid in listOf(
                -1f,
                Float.NaN,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                ui.update(invalid)
            }
        }

        ui.dispose()
    }

    private fun sceneWith(
        stage: Stage
    ): StrataScene<Terrain, SoundCategory> {
        return StrataScene(
            terrainDirectory = "",
            objectDirectory = "",
            uiFactory = StrataUiFactory { skin, theme ->
                StrataUi(
                    skin = skin,
                    theme = theme,
                    stage = stage
                )
            },
            configure = {}
        )
    }

    private class TrackingSkin : Skin() {
        var disposed = false
            private set

        override fun dispose() {
            disposed = true
            super.dispose()
        }
    }

    private class TrackingStage : Stage(
        TestViewport(),
        proxy(
            Batch::class.java
        ) { _, method, _ ->
            defaultValue(method.returnType)
        }
    ) {
        val actDeltas = mutableListOf<Float>()
        var drawCalls = 0
            private set
        var disposed = false
            private set

        override fun act(delta: Float) {
            actDeltas += delta
        }

        override fun draw() {
            drawCalls++
        }

        override fun dispose() {
            disposed = true
        }
    }

    private class TestViewport : Viewport() {
        init {
            camera = TestCamera()
        }

        override fun update(
            screenWidth: Int,
            screenHeight: Int,
            centerCamera: Boolean
        ) {
            setScreenBounds(0, 0, screenWidth, screenHeight)
            setWorldSize(screenWidth.toFloat(), screenHeight.toFloat())

            if (centerCamera) {
                camera.position.set(worldWidth / 2f, worldHeight / 2f, 0f)
            }
        }
    }

    private class TestCamera : Camera() {
        override fun update() = Unit

        override fun update(updateFrustum: Boolean) = Unit
    }
}
