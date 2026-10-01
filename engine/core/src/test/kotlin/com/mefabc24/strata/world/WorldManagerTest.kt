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

    private class RecordingView : SceneWorldView {
        override val publicView: IsoWorldView? = null
        override val inputProcessor: InputProcessor = InputAdapter()
        override val hoveredTile: TilePosition? = null
        override val renderStats = RenderStats()
        var cameraMarker = 0
        var disposed = false

        override fun update(realDelta: Float, simulationDelta: Float) = Unit
        override fun render(previews: List<PlacementPreview>) = Unit
        override fun resize(width: Int, height: Int) = Unit
        override fun dispose() { disposed = true }
    }
}
