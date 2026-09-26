package com.mefabc24.strata

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.scene.SceneWorldView
import com.mefabc24.strata.scene.SceneWorldViewFactory
import com.mefabc24.strata.scene.StrataScene
import com.mefabc24.strata.scene.StrataUiFactory
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StrataSceneGameTest {

    private enum class Terrain {
        GRASS
    }

    private enum class SoundCategory {
        EFFECT
    }

    private class TestTile : Tile

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `scene is created during lifecycle after subclass initialization`() {
        val events = mutableListOf<String>()
        val game = TestGame(events)

        assertEquals(listOf("subclass initialized"), events)

        assertFailsWith<IllegalStateException> {
            game.scene
        }

        game.resize(100, 100)
        assertEquals(listOf("subclass initialized"), events)

        game.create()

        assertEquals(
            listOf(
                "subclass initialized",
                "create scene",
                "ready"
            ),
            events
        )

        assertFailsWith<IllegalStateException> {
            game.create()
        }

        game.dispose()
    }

    @Test
    fun `scene game delegates lifecycle in documented order`() {
        val events = mutableListOf<String>()
        val game = TestGame(events)
        game.create()
        events.clear()

        game.update(0.5f)
        game.render()
        game.resize(900, 700)
        game.dispose()
        game.dispose()

        assertEquals(
            listOf(
                "scene update 0.5",
                "game update 0.5",
                "scene render",
                "scene resize 900x700",
                "game resize 900x700",
                "scene dispose",
                "game dispose"
            ),
            events
        )

        assertFailsWith<IllegalStateException> {
            game.scene
        }
    }

    private class TestGame(
        private val events: MutableList<String>
    ) : StrataSceneGame<Terrain, SoundCategory>() {
        private val subclassInitialized = true

        init {
            events += "subclass initialized"
        }

        override fun createScene(): StrataScene<Terrain, SoundCategory> {
            check(subclassInitialized)
            events += "create scene"

            val scene = StrataScene<Terrain, SoundCategory>(
                terrainDirectory = "",
                objectDirectory = "",
                uiFactory = StrataUiFactory { _, _ ->
                    error("UI creation was not expected in this test.")
                },
                worldViewFactory = SceneWorldViewFactory {
                    RecordingWorldView(events)
                },
                configure = {}
            )

            scene.attachWorld(
                world = World(1, 1) { _, _ -> TestTile() },
                terrainFor = { Terrain.GRASS }
            )

            return scene
        }

        override fun onReady() {
            events += "ready"
        }

        override fun updateGame(delta: Float) {
            events += "game update $delta"
        }

        override fun resizeGame(width: Int, height: Int) {
            events += "game resize ${width}x$height"
        }

        override fun disposeGame() {
            events += "game dispose"
        }
    }

    private class RecordingWorldView(
        private val events: MutableList<String>
    ) : SceneWorldView {
        override val publicView: IsoWorldView? = null
        override val inputProcessor: InputProcessor = InputAdapter()
        override val hoveredTile: TilePosition? = null
        override val renderStats = RenderStats()

        override fun update(delta: Float) {
            events += "scene update $delta"
        }

        override fun render(previews: List<PlacementPreview>) {
            events += "scene render"
        }

        override fun resize(width: Int, height: Int) {
            events += "scene resize ${width}x$height"
        }

        override fun dispose() {
            events += "scene dispose"
        }
    }
}
