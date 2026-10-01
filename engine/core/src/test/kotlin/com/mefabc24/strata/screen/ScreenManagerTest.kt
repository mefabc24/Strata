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
import kotlin.test.assertFailsWith
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
                "menu create", "menu activate", "game create",
                "menu deactivate", "game activate", "game deactivate",
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

    @Test
    fun `failed creation disposes ui and can be retried`() {
        val fixture = fixture()
        var fail = true
        fixture.screens.register("stable") { }
        fixture.screens.register("unstable") {
            ui { }
            onCreate {
                if (fail) error("creation failed")
            }
        }
        fixture.screens.navigate("stable")

        assertFailsWith<IllegalStateException> {
            fixture.screens.navigate("unstable")
        }
        assertEquals(ScreenId("stable"), fixture.screens.currentId)
        assertTrue(fixture.screens.history.isEmpty())
        assertEquals(1, fixture.createdUiCount)
        assertEquals(1, fixture.disposedUiCount)

        fail = false
        fixture.screens.navigate("unstable")

        assertEquals(ScreenId("unstable"), fixture.screens.currentId)
        assertEquals(listOf(ScreenId("stable")), fixture.screens.history)
        assertEquals(2, fixture.createdUiCount)
        assertEquals(1, fixture.disposedUiCount)
    }

    @Test
    fun `screen ids and registrations reject invalid definitions`() {
        val fixture = fixture()

        listOf("", " ", "\t").forEach { value ->
            assertFailsWith<IllegalArgumentException> { ScreenId(value) }
        }
        fixture.screens.register("menu") { }
        assertFailsWith<IllegalArgumentException> {
            fixture.screens.register("menu") { }
        }
        assertFailsWith<IllegalArgumentException> {
            fixture.screens.register("missing-world") { world("missing") }
        }
        assertFailsWith<IllegalStateException> {
            fixture.screens.navigate("missing")
        }

        assertEquals(setOf(ScreenId("menu")), fixture.screens.ids)
        assertTrue(ScreenId("menu") in fixture.screens)
    }

    @Test
    fun `screen definition allows only one ui`() {
        val fixture = fixture()

        assertFailsWith<IllegalStateException> {
            fixture.screens.register("invalid") {
                ui { }
                ui { }
            }
        }

        assertFalse(ScreenId("invalid") in fixture.screens)
    }

    @Test
    fun `same screen and equal parameters are a navigation no op`() {
        val fixture = fixture()
        val events = mutableListOf<String>()
        fixture.screens.register("gameplay") {
            onActivate { events += "activate:$parameters" }
            onDeactivate { events += "deactivate:$parameters" }
        }

        fixture.screens.navigate("gameplay", listOf(1, 2))
        fixture.screens.navigate("gameplay", listOf(1, 2))

        assertEquals(listOf("activate:[1, 2]"), events)
        assertTrue(fixture.screens.history.isEmpty())
    }

    @Test
    fun `same screen with changed parameters reactivates without adding history`() {
        val fixture = fixture()
        val events = mutableListOf<String>()
        fixture.screens.register("gameplay") {
            onActivate { events += "activate:$parameters" }
            onDeactivate { events += "deactivate:$parameters" }
        }

        fixture.screens.navigate("gameplay", "first")
        fixture.screens.navigate("gameplay", "second")

        assertEquals(
            listOf("activate:first", "deactivate:first", "activate:second"),
            events
        )
        assertTrue(fixture.screens.history.isEmpty())
    }

    @Test
    fun `back dismisses overlays before consuming screen history`() {
        val fixture = fixture()
        fixture.screens.register("menu") { }
        fixture.screens.register("gameplay") { world("city") }
        fixture.screens.register("pause") { }
        fixture.screens.navigate("menu")
        fixture.screens.navigate("gameplay")
        fixture.screens.showOverlay("pause")

        assertTrue(fixture.screens.back())
        assertEquals(ScreenId("gameplay"), fixture.screens.currentId)
        assertEquals(listOf(ScreenId("menu")), fixture.screens.history)
        assertTrue(fixture.screens.back())
        assertEquals(ScreenId("menu"), fixture.screens.currentId)
        assertFalse(fixture.screens.back())
        assertFalse(fixture.screens.canGoBack)
    }

    @Test
    fun `clear history leaves the current presentation unchanged`() {
        val fixture = fixture()
        fixture.screens.register("menu") { }
        fixture.screens.register("gameplay") { }
        fixture.screens.navigate("menu")
        fixture.screens.navigate("gameplay")

        fixture.screens.clearHistory()

        assertEquals(ScreenId("gameplay"), fixture.screens.currentId)
        assertTrue(fixture.screens.history.isEmpty())
        assertFalse(fixture.screens.canGoBack)
    }

    @Test
    fun `overlay rules reject missing base and duplicate visible screen`() {
        val fixture = fixture()
        fixture.screens.register("base") { }
        fixture.screens.register("overlay") { }

        assertFailsWith<IllegalStateException> {
            fixture.screens.showOverlay("overlay")
        }
        fixture.screens.navigate("base")
        fixture.screens.showOverlay("overlay")
        assertFailsWith<IllegalArgumentException> {
            fixture.screens.showOverlay("overlay")
        }
        assertFailsWith<IllegalArgumentException> {
            fixture.screens.showOverlay("base")
        }
    }

    @Test
    fun `non blocking overlay lets input reach the active world`() {
        val calls = mutableListOf<String>()
        val fixture = fixture(calls)
        fixture.screens.register("gameplay") { world("city") }
        fixture.screens.register("hud") { ui { } }
        fixture.screens.navigate("gameplay")
        fixture.screens.showOverlay("hud", blocksInput = false)

        assertTrue(fixture.input.processor.touchDown(0, 0, 0, 0))

        assertEquals(listOf("world"), calls)
    }

    @Test
    fun `world overlay temporarily replaces and then restores base world`() {
        val fixture = fixture()
        fixture.screens.register("gameplay") { world("city") }
        fixture.screens.register("map") { world("underground") }
        fixture.screens.navigate("gameplay")

        fixture.screens.showOverlay("map")
        assertEquals(WorldId("underground"), fixture.worlds.activeId)
        fixture.screens.dismissOverlay()

        assertEquals(WorldId("city"), fixture.worlds.activeId)
    }

    @Test
    fun `removing overlay restores world and disposes its retained ui`() {
        val fixture = fixture()
        val events = mutableListOf<String>()
        fixture.screens.register("gameplay") { world("city") }
        fixture.screens.register("map") {
            world("underground")
            ui { }
            onDeactivate { events += "deactivate" }
            onDispose { events += "dispose" }
        }
        fixture.screens.navigate("gameplay")
        fixture.screens.showOverlay("map")

        assertTrue(fixture.screens.remove("map"))

        assertEquals(WorldId("city"), fixture.worlds.activeId)
        assertEquals(listOf("deactivate", "dispose"), events)
        assertEquals(1, fixture.disposedUiCount)
        assertFalse(fixture.screens.remove("map"))
    }

    @Test
    fun `removing current base clears presentation world and history references`() {
        val fixture = fixture()
        fixture.screens.register("menu") { }
        fixture.screens.register("gameplay") { world("city") }
        fixture.screens.navigate("menu")
        fixture.screens.navigate("gameplay")

        assertTrue(fixture.screens.remove("gameplay"))

        assertNull(fixture.screens.currentId)
        assertNull(fixture.worlds.activeId)
        assertEquals(listOf(ScreenId("menu")), fixture.screens.history)
    }

    @Test
    fun `visible ui layers update render and all created ui layers resize`() {
        val fixture = fixture()
        fixture.screens.register("base") { ui { } }
        fixture.screens.register("overlay") { ui { } }
        fixture.screens.register("hidden") { ui { } }
        fixture.screens.navigate("hidden")
        fixture.screens.navigate("base")
        fixture.screens.showOverlay("overlay")

        fixture.screens.update(0.125f)
        fixture.screens.render()
        fixture.screens.resize(1024, 768)

        assertTrue(fixture.stages[0].actDeltas.isEmpty())
        assertEquals(listOf(0.125f), fixture.stages[1].actDeltas)
        assertEquals(listOf(0.125f), fixture.stages[2].actDeltas)
        assertEquals(0, fixture.stages[0].drawCalls)
        assertEquals(1, fixture.stages[1].drawCalls)
        assertEquals(1, fixture.stages[2].drawCalls)
        fixture.stages.forEach { stage ->
            assertEquals(1024, stage.viewport.screenWidth)
            assertEquals(768, stage.viewport.screenHeight)
        }
    }

    @Test
    fun `dispose deactivates top down and disposes entries in reverse registration order`() {
        val fixture = fixture()
        val events = mutableListOf<String>()
        for (id in listOf("base", "lower", "upper")) {
            fixture.screens.register(id) {
                ui { }
                onDeactivate { events += "$id deactivate" }
                onDispose { events += "$id dispose" }
            }
        }
        fixture.screens.navigate("base")
        fixture.screens.showOverlay("lower")
        fixture.screens.showOverlay("upper")

        fixture.screens.dispose()
        fixture.screens.dispose()

        assertEquals(
            listOf(
                "upper deactivate", "lower deactivate", "base deactivate",
                "upper dispose", "lower dispose", "base dispose"
            ),
            events
        )
        assertEquals(3, fixture.disposedUiCount)
        assertTrue(fixture.screens.ids.isEmpty())
        assertTrue(fixture.screens.activeScreenIds.isEmpty())
    }

    @Test
    fun `mutating screen operations fail after disposal`() {
        val fixture = fixture()
        fixture.screens.dispose()

        assertFailsWith<IllegalStateException> {
            fixture.screens.register("menu") { }
        }
        assertFailsWith<IllegalStateException> { fixture.screens.navigate("menu") }
        assertFailsWith<IllegalStateException> { fixture.screens.back() }
        assertFailsWith<IllegalStateException> { fixture.screens.clearHistory() }
        assertFailsWith<IllegalStateException> { fixture.screens.remove("menu") }
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
        val stages = mutableListOf<TrackingStage>()
        val screens = ScreenManager(
            worlds = worlds,
            input = input,
            uiFactory = ManagedUiFactory {
                created++
                val stage = TrackingStage()
                stages += stage
                val ui = StrataUi(
                    skin = Skin(),
                    theme = StrataUiTheme(),
                    stage = stage
                )
                ManagedUi(ui) {
                    disposed++
                    ui.dispose()
                }
            }
        )
        return Fixture(input, worlds, screens, stages, { created }, { disposed })
    }

    private class Fixture(
        val input: StrataInput,
        val worlds: WorldManager,
        val screens: ScreenManager,
        val stages: List<TrackingStage>,
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
    ) {
        val actDeltas = mutableListOf<Float>()
        var drawCalls = 0
            private set

        override fun act(delta: Float) {
            actDeltas += delta
        }

        override fun draw() {
            drawCalls++
        }
    }

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
