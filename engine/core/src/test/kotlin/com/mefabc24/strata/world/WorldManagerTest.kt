package com.mefabc24.strata.world

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.scene.SceneWorldView
import com.mefabc24.strata.terrain.TerrainId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WorldManagerTest {

    private enum class Terrain : TerrainId { GRASS }
    private class TestTile : Tile
    private class TestEntity : Entity

    @Test
    fun `world switching retains logical and view state`() {
        val createdViews = mutableMapOf<WorldId, RecordingView>()
        val activations = mutableListOf<Pair<WorldId?, WorldId?>>()
        val manager = manager(createdViews, activations)
        val city = world()
        val underground = world()

        manager.register("city", city) { Terrain.GRASS }
        manager.register("underground", underground) { Terrain.GRASS }
        manager.activate("city")

        city.terrain.setTile(1, 1, TestTile())
        createdViews.getValue(WorldId("city")).cameraMarker = 42
        manager.activate("underground")
        manager.activate("city")

        assertSame(city, manager.activeWorld)
        assertSame(city, manager["city"])
        assertSame(underground, manager["underground"])
        assertEquals(42, createdViews.getValue(WorldId("city")).cameraMarker)
        assertEquals(2, createdViews.size)
        assertEquals(
            listOf<Pair<WorldId?, WorldId?>>(
                null to WorldId("city"),
                WorldId("city") to WorldId("underground"),
                WorldId("underground") to WorldId("city")
            ),
            activations
        )
    }

    @Test
    fun `inactive worlds pause by default and can opt into updates`() {
        val manager = manager()
        val city = world()
        val underground = world()
        val cityEntity = movingEntity(city)
        val undergroundEntity = movingEntity(underground)
        manager.register("city", city) { Terrain.GRASS }
        manager.register("underground", underground) { Terrain.GRASS }
        manager.activate("city")

        manager.update(0.25f, 0.25f) { world, delta, _ ->
            world.updateEntities(delta)
        }

        assertEquals(EntityPosition(1f, 0.5f), cityEntity.position)
        assertEquals(EntityPosition(0.5f, 0.5f), undergroundEntity.position)

        manager.inactiveWorldPolicy = InactiveWorldPolicy.UPDATE
        manager.update(0.25f, 0.25f) { world, delta, _ ->
            world.updateEntities(delta)
        }

        assertEquals(EntityPosition(1.5f, 0.5f), cityEntity.position)
        assertEquals(EntityPosition(1f, 0.5f), undergroundEntity.position)
    }

    @Test
    fun `removing and disposing release views but preserve game worlds`() {
        val views = mutableMapOf<WorldId, RecordingView>()
        val manager = manager(views)
        val city = world()
        val underground = world()
        manager.register("city", city) { Terrain.GRASS }
        manager.register("underground", underground) { Terrain.GRASS }
        manager.activate("city")

        assertSame(city, manager.remove("city"))
        assertNull(manager.activeWorld)
        assertTrue(views.getValue(WorldId("city")).disposed)
        assertFalse(views.getValue(WorldId("underground")).disposed)
        assertEquals(25, city.groundTileCount)

        manager.dispose()
        manager.dispose()

        assertTrue(views.getValue(WorldId("underground")).disposed)
        assertEquals(25, underground.groundTileCount)
    }

    @Test
    fun `world ids reject blank values`() {
        listOf("", " ", "\t\n").forEach { value ->
            assertFailsWith<IllegalArgumentException> { WorldId(value) }
        }
    }

    @Test
    fun `registration preserves order and rejects duplicate ids and instances`() {
        val manager = manager()
        val city = world()
        val village = world()

        assertSame(city, manager.register("city", city) { Terrain.GRASS })
        manager.register("village", village) { Terrain.GRASS }

        assertEquals(
            linkedSetOf(WorldId("city"), WorldId("village")),
            manager.ids
        )
        assertTrue(manager.contains(WorldId("city")))
        assertSame(village, manager.find(WorldId("village")))
        assertNull(manager.find(WorldId("missing")))
        assertFailsWith<IllegalArgumentException> {
            manager.register("city", world()) { Terrain.GRASS }
        }
        assertFailsWith<IllegalArgumentException> {
            manager.register("alias", city) { Terrain.GRASS }
        }
    }

    @Test
    fun `unknown world lookup and activation fail without changing active world`() {
        val activations = mutableListOf<Pair<WorldId?, WorldId?>>()
        val manager = manager(activations = activations)
        val city = world()
        manager.register("city", city) { Terrain.GRASS }
        manager.activate("city")

        assertFailsWith<IllegalStateException> { manager["missing"] }
        assertFailsWith<IllegalStateException> { manager.activate("missing") }

        assertSame(city, manager.activeWorld)
        assertEquals(
            listOf<Pair<WorldId?, WorldId?>>(null to WorldId("city")),
            activations
        )
    }

    @Test
    fun `activating current world and repeated deactivation are no ops`() {
        val activations = mutableListOf<Pair<WorldId?, WorldId?>>()
        val manager = manager(activations = activations)
        val city = world()
        manager.register("city", city) { Terrain.GRASS }

        assertSame(city, manager.activate("city"))
        assertSame(city, manager.activate("city"))
        manager.deactivate()
        manager.deactivate()

        assertNull(manager.activeId)
        assertEquals(
            listOf(null to WorldId("city"), WorldId("city") to null),
            activations
        )
    }

    @Test
    fun `update render and resize affect only the appropriate views`() {
        val views = mutableMapOf<WorldId, RecordingView>()
        val manager = manager(views)
        val city = world()
        val underground = world()
        manager.register("city", city) { Terrain.GRASS }
        manager.register("underground", underground) { Terrain.GRASS }
        manager.activate("city")
        val updated = mutableListOf<Triple<World, Float, Boolean>>()

        manager.update(0.2f, 0.5f) { world, delta, active ->
            updated += Triple(world, delta, active)
        }
        manager.render()
        manager.resize(1280, 720)

        assertEquals(listOf(Triple(city, 0.5f, true)), updated)
        assertEquals(listOf(0.2f to 0.5f), views.getValue(WorldId("city")).updates)
        assertTrue(views.getValue(WorldId("underground")).updates.isEmpty())
        assertEquals(1, views.getValue(WorldId("city")).renderCalls)
        assertEquals(0, views.getValue(WorldId("underground")).renderCalls)
        assertEquals(listOf(1280 to 720), views.getValue(WorldId("city")).resizes)
        assertEquals(listOf(1280 to 720), views.getValue(WorldId("underground")).resizes)
    }

    @Test
    fun `background updates identify active world and never update inactive views`() {
        val views = mutableMapOf<WorldId, RecordingView>()
        val manager = manager(views)
        val city = world()
        val underground = world()
        manager.register("city", city) { Terrain.GRASS }
        manager.register("underground", underground) { Terrain.GRASS }
        manager.activate("underground")
        manager.inactiveWorldPolicy = InactiveWorldPolicy.UPDATE
        val updated = mutableListOf<Pair<World, Boolean>>()

        manager.update(0.25f, 0.75f) { world, delta, active ->
            assertEquals(0.75f, delta)
            updated += world to active
        }

        assertEquals(listOf(city to false, underground to true), updated)
        assertTrue(views.getValue(WorldId("city")).updates.isEmpty())
        assertEquals(
            listOf(0.25f to 0.75f),
            views.getValue(WorldId("underground")).updates
        )
    }

    @Test
    fun `removing missing or inactive worlds preserves active registration`() {
        val views = mutableMapOf<WorldId, RecordingView>()
        val manager = manager(views)
        val city = world()
        val underground = world()
        manager.register("city", city) { Terrain.GRASS }
        manager.register("underground", underground) { Terrain.GRASS }
        manager.activate("city")

        assertNull(manager.remove("missing"))
        assertSame(underground, manager.remove("underground"))

        assertSame(city, manager.activeWorld)
        assertEquals(setOf(WorldId("city")), manager.ids)
        assertFalse(views.getValue(WorldId("city")).disposed)
        assertTrue(views.getValue(WorldId("underground")).disposed)
    }

    @Test
    fun `disposal deactivates once and disposes views in reverse registration order`() {
        val disposalOrder = mutableListOf<String>()
        val activations = mutableListOf<Pair<WorldId?, WorldId?>>()
        val manager = WorldManager(
            createRuntime = { id, world, terrainFor ->
                WorldRuntime(
                    id,
                    world,
                    terrainFor,
                    RecordingView { disposalOrder += id.value },
                    placement = null
                )
            },
            activationChanged = { previous, next ->
                activations += previous?.id to next?.id
            }
        )
        manager.register("first", world()) { Terrain.GRASS }
        manager.register("second", world()) { Terrain.GRASS }
        manager.register("third", world()) { Terrain.GRASS }
        manager.activate("second")

        manager.dispose()
        manager.dispose()

        assertEquals(listOf("third", "second", "first"), disposalOrder)
        assertEquals(
            listOf(null to WorldId("second"), WorldId("second") to null),
            activations
        )
        assertTrue(manager.ids.isEmpty())
        assertNull(manager.activeWorld)
    }

    @Test
    fun `mutating operations fail after disposal`() {
        val manager = manager()
        manager.dispose()

        assertFailsWith<IllegalStateException> {
            manager.register("city", world()) { Terrain.GRASS }
        }
        assertFailsWith<IllegalStateException> { manager.activate("city") }
        assertFailsWith<IllegalStateException> { manager.deactivate() }
        assertFailsWith<IllegalStateException> { manager.remove("city") }
    }

    @Test
    fun `world switching deactivation removal and disposal cancel unfinished placement paths`() {
        val placements = mutableMapOf<WorldId, com.mefabc24.strata.placement.PlacementController>()
        val manager = WorldManager(
            createRuntime = { id, world, terrainFor ->
                val placement = com.mefabc24.strata.placement.PlacementController(world).apply {
                    selectedFactory = { object : Placeable { override val footprint = Footprint.square(1) } }
                }
                placements[id] = placement
                WorldRuntime(id, world, terrainFor, RecordingView(), placement)
            },
            activationChanged = { _, _ -> }
        )
        manager.register("first", world()) { Terrain.GRASS }
        manager.register("second", world()) { Terrain.GRASS }
        manager.activate("first")
        val first = placements.getValue(WorldId("first"))
        val factory = first.selectedFactory
        first.path.begin(TilePosition(1, 1))
        manager.activate("second")
        assertFalse(first.path.active)
        assertTrue(first.previews.isEmpty())
        assertSame(factory, first.selectedFactory)
        manager.activate("first")
        assertTrue(first.path.begin(TilePosition(2, 2)))
        manager.deactivate()
        assertFalse(first.path.active)
        first.path.begin(TilePosition(3, 3))
        manager.remove("first")
        assertFalse(first.path.active)
        val second = placements.getValue(WorldId("second"))
        second.path.begin(TilePosition(1, 1))
        manager.dispose()
        assertFalse(second.path.active)
        assertTrue(second.previews.isEmpty())
    }

    private fun manager(
        views: MutableMap<WorldId, RecordingView> = mutableMapOf(),
        activations: MutableList<Pair<WorldId?, WorldId?>> = mutableListOf()
    ) = WorldManager(
        createRuntime = { id, world, terrainFor ->
            val view = RecordingView()
            views[id] = view
            WorldRuntime(id, world, terrainFor, view, placement = null)
        },
        activationChanged = { previous, next ->
            activations += previous?.id to next?.id
        }
    )

    private fun world() = World(5, 5) { _, _ -> TestTile() }

    private fun movingEntity(world: World): WorldEntity =
        world.addEntity(TestEntity(), EntityPosition(0.5f, 0.5f)).apply {
            followPath(listOf(TilePosition(2, 0)), speed = 2f)
        }

    private class RecordingView(
        private val onDispose: () -> Unit = {}
    ) : SceneWorldView {
        override val publicView: IsoWorldView? = null
        override val inputProcessor: InputProcessor = InputAdapter()
        override val hoveredTile: TilePosition? = null
        override val renderStats = RenderStats()
        var cameraMarker = 0
        var disposed = false
        val updates = mutableListOf<Pair<Float, Float>>()
        var renderCalls = 0
        val resizes = mutableListOf<Pair<Int, Int>>()

        override fun update(realDelta: Float, simulationDelta: Float) {
            updates += realDelta to simulationDelta
        }
        override fun render(previews: List<PlacementPreview>) { renderCalls++ }
        override fun resize(width: Int, height: Int) { resizes += width to height }
        override fun dispose() {
            disposed = true
            onDispose()
        }
    }
}
