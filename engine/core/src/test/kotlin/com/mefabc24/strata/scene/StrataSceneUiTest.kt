package com.mefabc24.strata.scene

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.Viewport
import com.mefabc24.strata.audio.SoundCategoryId
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.testing.defaultValue
import com.mefabc24.strata.testing.proxy
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.terrain.TerrainId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StrataSceneUiTest {

    private enum class Terrain : TerrainId {
        GRASS
    }

    private enum class SoundCategory : SoundCategoryId {
        EFFECT
    }

    private class TestTile : Tile

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
    fun `enabled debug panel owns an independent automatic ui lifecycle`() {
        val inputState = TestGdxEnvironment.install()
        val stage = TrackingStage()
        val scene = sceneWith(stage, configure = {
            registrations {
                terrain { register(Terrain.GRASS, "com/badlogic/gdx/utils/lsans-15.png") }
            }
            debug { panel { enabled = true } }
        })

        scene.attachWorld(
            World(2, 2) { _, _ -> TestTile() },
            terrainFor = { Terrain.GRASS }
        )
        assertTrue(inputState.inputProcessor === scene.input.processor)
        assertFalse(scene.debug.panel.visible)
        assertTrue(scene.input.processor.keyDown(Input.Keys.F3))
        assertTrue(scene.debug.panel.visible)

        scene.update(0.1f)
        scene.render()
        scene.resize(900, 700)
        assertEquals(listOf(0.1f), stage.actDeltas)
        assertEquals(1, stage.drawCalls)
        assertEquals(900, stage.viewport.screenWidth)
        assertEquals(700, stage.viewport.screenHeight)

        scene.dispose()
        assertTrue(stage.disposed)
        assertNull(inputState.inputProcessor)
    }

    @Test
    fun `scene coordinates world and ui layers together`() {
        val inputState = TestGdxEnvironment.install()
        val stage = TrackingStage()
        val worldView = TrackingWorldView()
        val scene = sceneWith(
            stage = stage,
            worldViewFactory = SceneWorldViewFactory { worldView }
        )

        scene.createUi(TrackingSkin())
        scene.attachWorld(
            world = World(2, 2) { _, _ -> TestTile() },
            terrainFor = { Terrain.GRASS }
        )

        scene.update(0.25f)
        scene.render()
        scene.resize(800, 600)

        assertTrue(inputState.inputProcessor === scene.input.processor)
        assertEquals(listOf(0.25f to 0.25f), worldView.updateDeltas)
        assertEquals(listOf(0.25f), stage.actDeltas)
        assertEquals(1, worldView.renderCalls)
        assertEquals(1, stage.drawCalls)
        assertEquals(listOf(800 to 600), worldView.resizes)
        assertEquals(800, stage.viewport.screenWidth)
        assertEquals(600, stage.viewport.screenHeight)

        scene.dispose()

        assertTrue(worldView.disposed)
        assertTrue(stage.disposed)
        assertNull(inputState.inputProcessor)
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
    fun `failed screen ui configuration disposes its stage`() {
        val stage = TrackingStage()
        val scene = sceneWith(stage)
        val skin = TrackingSkin()
        scene.screens.register("broken") {
            ui(skin) {
                error("configuration failed")
            }
        }

        assertFailsWith<IllegalStateException> {
            scene.screens.navigate("broken")
        }

        assertTrue(stage.disposed)
        assertFalse(skin.disposed)
        assertNull(scene.screens.currentId)
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
        stage: Stage,
        worldViewFactory: SceneWorldViewFactory =
            DefaultSceneWorldViewFactory,
        configure: StrataScene.() -> Unit = {}
    ): StrataScene {
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
            worldViewFactory = worldViewFactory,
            configure = configure
        )
    }

    private class TrackingWorldView : SceneWorldView {
        override val publicView: IsoWorldView? = null
        override val inputProcessor: InputProcessor = InputAdapter()
        override val hoveredTile: TilePosition? = null
        override val renderStats = RenderStats()

        val updateDeltas = mutableListOf<Pair<Float, Float>>()
        var renderCalls = 0
            private set
        val resizes = mutableListOf<Pair<Int, Int>>()
        var disposed = false
            private set

        override fun update(
            realDelta: Float,
            simulationDelta: Float
        ) {
            updateDeltas += realDelta to simulationDelta
        }

        override fun render(previews: List<PlacementPreview>) {
            renderCalls++
        }

        override fun resize(width: Int, height: Int) {
            resizes += width to height
        }

        override fun dispose() {
            disposed = true
        }
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
