package com.mefabc24.strata.screen

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.Viewport
import com.mefabc24.strata.input.StrataInput
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.scene.SceneWorldView
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.testing.defaultValue
import com.mefabc24.strata.testing.proxy
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.StrataUiTheme
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldId
import com.mefabc24.strata.world.WorldManager
import com.mefabc24.strata.world.WorldRuntime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScreenManagerTest {
    private class TestTile : Tile

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `navigation creates screens once and follows lifecycle order`() {
        val fixture = fixture()
        val events = mutableListOf<String>()
        fixture.screens.register("menu") {
            ui { }
            onCreate { events += "menu create" }
            onActivate { events += "menu activate" }
            onDeactivate { events += "menu deactivate" }
            onDispose { events += "menu dispose" }
        }
        fixture.screens.register("gameplay") {
            world("city")
            onCreate { events += "game create" }
            onActivate { events += "game activate" }
            onDeactivate { events += "game deactivate" }
            onDispose { events += "game dispose" }
        }

        fixture.screens.navigate("menu")
        assertNull(fixture.worlds.activeId)
        fixture.screens.navigate("gameplay")
        assertEquals(WorldId("city"), fixture.worlds.activeId)
        assertEquals(listOf(ScreenId("menu")), fixture.screens.history)
        assertTrue(fixture.screens.back())
        assertEquals(ScreenId("menu"), fixture.screens.currentId)
        assertNull(fixture.worlds.activeId)
        assertTrue(fixture.screens.history.isEmpty())
        assertEquals(1, fixture.createdUiCount)

        fixture.screens.dispose()

        assertEquals(
            listOf(
                "menu create", "menu activate", "menu deactivate",
                "game create", "game activate", "game deactivate",
                "menu activate", "menu deactivate", "game dispose", "menu dispose"
            ),
            events
        )
        assertEquals(1, fixture.disposedUiCount)
    }

    @Test
    fun `repeated switching retains each screen ui`() {
        val fixture = fixture()
        fixture.screens.register("menu") { ui { } }
        fixture.screens.register("settings") { ui { } }

        repeat(3) {
            fixture.screens.navigate("menu")
            fixture.screens.navigate("settings")
        }

        assertEquals(2, fixture.createdUiCount)
        assertEquals(ScreenId("settings"), fixture.screens.currentId)
    }

    @Test
    fun `blocking overlay preserves the world and stops underlying input`() {
        val worldInputCalls = mutableListOf<String>()
        val fixture = fixture(worldInputCalls)
        fixture.screens.register("gameplay") { world("city") }
        fixture.screens.register("pause") { ui { } }
        fixture.screens.navigate("gameplay")

        assertTrue(fixture.input.processor.touchDown(0, 0, 0, 0))
        assertEquals(listOf("world"), worldInputCalls)
        worldInputCalls.clear()

        fixture.worlds.activate("underground")

        fixture.screens.showOverlay("pause")

        assertEquals(
            listOf(ScreenId("gameplay"), ScreenId("pause")),
            fixture.screens.activeScreenIds
        )
        assertEquals(WorldId("underground"), fixture.worlds.activeId)
        assertTrue(fixture.input.processor.touchDown(0, 0, 0, 0))
        assertTrue(worldInputCalls.isEmpty())

        assertTrue(fixture.screens.dismissOverlay())
        assertFalse(fixture.screens.dismissOverlay())
        assertTrue(fixture.input.processor.touchDown(0, 0, 0, 0))
        assertEquals(listOf("world"), worldInputCalls)
    }

    @Test
    fun `activation parameters are available to callbacks`() {
        val fixture = fixture()
        var received: Any? = null
        fixture.screens.register("loading") {
            onActivate { received = parameters }
        }

        fixture.screens.navigate("loading", parameters = 73)

        assertEquals(73, received)
    }

    private fun fixture(
        worldInputCalls: MutableList<String> = mutableListOf()
    ): Fixture {
        val input = StrataInput()
        val worlds = WorldManager(
            createRuntime = { id, world, terrainFor ->
                WorldRuntime(
                    id, world, terrainFor,
                    RecordingView(worldInputCalls),
                    placement = null
                )
            },
            activationChanged = { _, next ->
                input.replaceWorldProcessor(next?.view?.inputProcessor)
            }
        )
        worlds.register("city", World(3, 3) { _, _ -> TestTile() }) {
            error("Terrain resolution is not used by this test.")
        }
        worlds.register("underground", World(3, 3) { _, _ -> TestTile() }) {
            error("Terrain resolution is not used by this test.")
        }

        var created = 0
        var disposed = 0
        val screens = ScreenManager(
            worlds = worlds,
            input = input,
            uiFactory = ManagedUiFactory {
                created++
                val ui = StrataUi(
                    skin = Skin(),
                    theme = StrataUiTheme(),
                    stage = TrackingStage()
                )
                ManagedUi(ui) {
                    disposed++
                    ui.dispose()
                }
            }
        )
        return Fixture(input, worlds, screens, { created }, { disposed })
    }

    private class Fixture(
        val input: StrataInput,
        val worlds: WorldManager,
        val screens: ScreenManager,
        private val created: () -> Int,
        private val disposed: () -> Int
    ) {
        val createdUiCount: Int get() = created()
        val disposedUiCount: Int get() = disposed()
    }

    private class RecordingView(
        private val calls: MutableList<String>
    ) : SceneWorldView {
        override val publicView: IsoWorldView? = null
        override val inputProcessor: InputProcessor = object : InputAdapter() {
            override fun touchDown(
                screenX: Int,
                screenY: Int,
                pointer: Int,
                button: Int
            ): Boolean {
                calls += "world"
                return true
            }
        }
        override val hoveredTile: TilePosition? = null
        override val renderStats = RenderStats()
        override fun update(realDelta: Float, simulationDelta: Float) = Unit
        override fun render(previews: List<PlacementPreview>) = Unit
        override fun resize(width: Int, height: Int) = Unit
        override fun dispose() = Unit
    }

    private class TrackingStage : Stage(
        TestViewport(),
        proxy(Batch::class.java) { _, method, _ ->
            defaultValue(method.returnType)
        }
    )

    private class TestViewport : Viewport() {
        init { camera = TestCamera() }

        override fun update(
            screenWidth: Int,
            screenHeight: Int,
            centerCamera: Boolean
        ) {
            setScreenBounds(0, 0, screenWidth, screenHeight)
            setWorldSize(screenWidth.toFloat(), screenHeight.toFloat())
        }
    }

    private class TestCamera : Camera() {
        override fun update() = Unit
        override fun update(updateFrustum: Boolean) = Unit
    }
}
