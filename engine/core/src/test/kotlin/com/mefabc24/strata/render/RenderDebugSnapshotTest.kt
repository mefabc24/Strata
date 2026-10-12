package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.render.order.TerrainCell
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RenderDebugSnapshotTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity
    private class TestObject : Placeable {
        override val footprint = Footprint.square(1)
    }
    private class WideObject : Placeable {
        override val footprint = Footprint.rectangle(2, 1)
    }

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `snapshot collection is opt in and reports actual culling result`() {
        var enabled = false
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(
            projection = IsoProjection(TileGeometry(32f, 24f)),
            collectDebugSnapshot = { enabled }
        )
        try {
            val world = World(2, 2) { _, _ -> TestTile }
            val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
            val visual = EntityVisual(TextureRegion(texture))
            val camera = OrthographicCamera(100f, 100f).apply {
                position.set(0f, -12f, 0f)
                update()
            }
            renderer.render(world, camera, { _, _ -> null }, entityVisualFor = { visual })
            assertNull(renderer.debugSnapshot)

            enabled = true
            renderer.render(world, camera, { _, _ -> null }, entityVisualFor = { visual })
            val visible = requireNotNull(renderer.debugSnapshot).items.first { it.entity === entity }
            assertSame(entity, visible.entity)
            assertTrue(visible.drawn)

            entity.teleport(EntityPosition(1000f, 1000f))
            renderer.render(world, camera, { _, _ -> null }, entityVisualFor = { visual })
            assertFalse(
                requireNotNull(renderer.debugSnapshot).items.first { it.entity === entity }.drawn
            )
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `terrain snapshots preserve global indices and terrain ranks across content and culling`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(
            projection = IsoProjection(TileGeometry(32f, 24f)),
            collectDebugSnapshot = { true }
        )
        try {
            val world = World(3, 1) { _, _ -> TestTile }
            val terrainVisual = TextureRegion(texture)
            val objectVisual = ObjectVisual(TextureRegion(texture))
            val entityVisual = EntityVisual(TextureRegion(texture))
            val camera = OrthographicCamera(1000f, 1000f).apply {
                position.set(0f, 0f, 0f)
                update()
            }

            renderer.render(world, camera, { _, _ -> terrainVisual })
            val ranksBeforeObject = terrainRanks(requireNotNull(renderer.debugSnapshot))

            world.place(TestObject(), 1, 0)
            world.addEntity(TestEntity, EntityPosition(1.5f, 0.5f))
            renderer.render(
                world,
                camera,
                { _, _ -> terrainVisual },
                objectVisualFor = { objectVisual },
                entityVisualFor = { entityVisual }
            )
            val complete = requireNotNull(renderer.debugSnapshot)
            val terrainItems = complete.items.filter { it.terrain != null }
            val plan = renderer.currentRenderPlan(world)

            assertEquals(3, complete.terrainCount)
            assertEquals(listOf(0, 1, 2), terrainItems.map { requireNotNull(it.terrain).rank })
            assertEquals(ranksBeforeObject, terrainRanks(complete))
            terrainItems.forEach { item ->
                val terrain = requireNotNull(item.terrain)
                val planIndex = plan.indexOfFirst { primitive ->
                    primitive is TerrainCell &&
                        primitive.x == terrain.position.x &&
                        primitive.y == terrain.position.y
                }
                assertEquals(planIndex, item.index)
            }

            camera.viewportWidth = 1f
            camera.viewportHeight = 1f
            camera.position.set(1000f, 1000f, 0f)
            camera.update()
            renderer.render(
                world,
                camera,
                { _, _ -> terrainVisual },
                objectVisualFor = { objectVisual },
                entityVisualFor = { entityVisual }
            )
            val culled = requireNotNull(renderer.debugSnapshot)

            assertEquals(terrainMetadata(complete), terrainMetadata(culled))
            assertTrue(culled.items.filter { it.terrain != null }.none { it.drawn })
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `actual order records terrain overlays before world objects and entities`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(
            projection = IsoProjection(TileGeometry(32f, 24f)),
            collectDebugSnapshot = { true }
        )
        try {
            val world = World(3, 1) { _, _ -> TestTile }
            world.addOverlayLayer("detail")
            world.setOverlayTile("detail", 1, 0, TestTile)
            val placed = requireNotNull(world.place(TestObject(), 1, 0))
            val entity = world.addEntity(TestEntity, EntityPosition(1.5f, 0.5f))
            val region = TextureRegion(texture)
            val camera = OrthographicCamera(1000f, 1000f).apply {
                position.set(0f, 0f, 0f)
                update()
            }

            renderer.render(
                world,
                camera,
                { _, _ -> region },
                objectVisualFor = { ObjectVisual(region) },
                entityVisualFor = { EntityVisual(region) }
            )
            val snapshot = requireNotNull(renderer.debugSnapshot)
            val terrain = snapshot.items
                .filter { it.terrain != null }
                .sortedBy { requireNotNull(it.terrain).position.x }

            assertEquals(3, snapshot.actualTerrainCount)
            assertEquals(listOf(0, 1, 3), terrain.map { it.actualIndex })
            assertEquals(
                listOf(0, 1, 2),
                terrain.map { requireNotNull(it.terrain).actualRank }
            )

            val objectItem = snapshot.items.single { it.placedObject === placed }
            val entityItem = snapshot.items.single { it.entity === entity }
            assertEquals(setOf(4, 5), setOf(objectItem.actualIndex, entityItem.actualIndex))
            assertTrue(terrain.all { requireNotNull(it.actualIndex) < 4 })

            val plan = renderer.currentRenderPlan(world)
            snapshot.items.forEach { item ->
                assertEquals(plan.indexOfFirst { primitive ->
                    when {
                        item.terrain != null -> primitive is TerrainCell &&
                            primitive.x == item.terrain.position.x &&
                            primitive.y == item.terrain.position.y
                        item.placedObject != null ->
                            (primitive as? com.mefabc24.strata.render.order.WorldObjectPrimitive)
                                ?.placedObject === item.placedObject
                        item.entity != null ->
                            (primitive as? com.mefabc24.strata.render.order.WorldEntityPrimitive)
                                ?.worldEntity === item.entity
                        else -> false
                    }
                }, item.index)
            }
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `actual indices omit culled items while calculated indices remain stable`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(
            projection = IsoProjection(TileGeometry(32f, 24f)),
            collectDebugSnapshot = { true }
        )
        try {
            val world = World(10, 1) { _, _ -> TestTile }
            val near = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
            val far = world.addEntity(TestEntity, EntityPosition(9.5f, 0.5f))
            val region = TextureRegion(texture)
            val visual = EntityVisual(region)
            val camera = OrthographicCamera(20f, 20f).apply {
                position.set(0f, -8f, 0f)
                update()
            }

            renderer.render(
                world,
                camera,
                { _, _ -> region },
                entityVisualFor = { visual },
                maxTerrainSpriteHeight = 48f
            )
            val nearView = requireNotNull(renderer.debugSnapshot)
            val nearItem = nearView.items.single { it.entity === near }
            val farCulled = nearView.items.single { it.entity === far }
            val calculated = nearView.items.associate { debugIdentity(it) to it.index }

            assertTrue(nearItem.drawn)
            assertEquals(nearView.actualTerrainCount, nearItem.actualIndex)
            assertFalse(farCulled.drawn)
            assertNull(farCulled.actualIndex)
            assertEquals(
                (0..nearView.actualTerrainCount).toList(),
                nearView.items.mapNotNull { it.actualIndex }.sorted()
            )

            camera.position.set(144f, -80f, 0f)
            camera.update()
            renderer.render(
                world,
                camera,
                { _, _ -> region },
                entityVisualFor = { visual },
                maxTerrainSpriteHeight = 48f
            )
            val farView = requireNotNull(renderer.debugSnapshot)
            val nearCulled = farView.items.single { it.entity === near }
            val farItem = farView.items.single { it.entity === far }

            assertFalse(nearCulled.drawn)
            assertNull(nearCulled.actualIndex)
            assertTrue(farItem.drawn)
            assertEquals(farView.actualTerrainCount, farItem.actualIndex)
            assertEquals(
                calculated,
                farView.items.associate { debugIdentity(it) to it.index }
            )
            assertEquals(
                (0..farView.actualTerrainCount).toList(),
                farView.items.mapNotNull { it.actualIndex }.sorted()
            )
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `snapshot exposes exact sort volumes projected front positions and priorities`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val projection = IsoProjection(TileGeometry(32f, 24f))
        val renderer = IsoWorldRenderer(
            projection = projection,
            collectDebugSnapshot = { true },
            objectPriorityFor = { 5 },
            entityPriorityFor = { -2 }
        )
        try {
            val world = World(4, 4) { _, _ -> TestTile }
            val placed = requireNotNull(world.place(WideObject(), 1, 2))
            val entity = world.addEntity(TestEntity, EntityPosition(2.25f, 1.75f))
            val region = TextureRegion(texture)
            val camera = OrthographicCamera(1000f, 1000f).apply { update() }

            renderer.render(
                world,
                camera,
                { _, _ -> region },
                objectVisualFor = { ObjectVisual(region) },
                entityVisualFor = { EntityVisual(region) }
            )

            val snapshot = requireNotNull(renderer.debugSnapshot)
            val objectSort = requireNotNull(
                snapshot.items.single { it.placedObject === placed }.sort
            )
            assertEquals(1f, objectSort.minX)
            assertEquals(3f, objectSort.maxX)
            assertEquals(2f, objectSort.minY)
            assertEquals(3f, objectSort.maxY)
            assertEquals(5, objectSort.renderPriority)
            assertEquals(
                projection.tileToWorld(objectSort.maxX, objectSort.maxY).x,
                objectSort.projectedFrontX
            )
            assertEquals(
                projection.tileToWorld(objectSort.maxX, objectSort.maxY).y,
                objectSort.projectedFrontY
            )

            val entitySort = requireNotNull(
                snapshot.items.single { it.entity === entity }.sort
            )
            assertEquals(entity.position.x, entitySort.minX)
            assertEquals(entity.position.x, entitySort.maxX)
            assertEquals(entity.position.y, entitySort.minY)
            assertEquals(entity.position.y, entitySort.maxY)
            assertEquals(-2, entitySort.renderPriority)
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `render diagnostics follow relocated object spatial data and order`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val projection = IsoProjection(TileGeometry(32f, 24f))
        val renderer = IsoWorldRenderer(
            projection = projection,
            collectDebugSnapshot = { true }
        )
        try {
            val world = World(8, 8) { _, _ -> TestTile }
            val rear = requireNotNull(world.place(TestObject(), 3, 3))
            val futureFront = requireNotNull(world.place(TestObject(), 1, 1))
            val region = TextureRegion(texture)
            val visual = ObjectVisual(region)
            val camera = OrthographicCamera(1000f, 1000f).apply { update() }

            renderer.render(
                world,
                camera,
                { _, _ -> region },
                objectVisualFor = { visual }
            )
            assertTrue(world.relocate(futureFront, TilePosition(4, 4)))
            renderer.render(
                world,
                camera,
                { _, _ -> region },
                objectVisualFor = { visual }
            )

            val snapshot = requireNotNull(renderer.debugSnapshot)
            val rearItem = snapshot.items.single { it.placedObject === rear }
            val frontItem = snapshot.items.single { it.placedObject === futureFront }
            val frontSort = requireNotNull(frontItem.sort)

            assertTrue(rearItem.index < frontItem.index)
            assertTrue(requireNotNull(rearItem.actualIndex) < requireNotNull(frontItem.actualIndex))
            assertEquals(4f, frontSort.minX)
            assertEquals(5f, frontSort.maxX)
            assertEquals(4f, frontSort.minY)
            assertEquals(5f, frontSort.maxY)
            assertEquals(
                projection.tileToWorld(5f, 5f).x,
                frontSort.projectedFrontX
            )
            assertEquals(
                projection.tileToWorld(5f, 5f).y,
                frontSort.projectedFrontY
            )
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    private fun terrainRanks(snapshot: RenderDebugSnapshot): Map<TilePosition, Int> =
        snapshot.items.mapNotNull { item ->
            item.terrain?.let { it.position to it.rank }
        }.toMap()

    private fun terrainMetadata(
        snapshot: RenderDebugSnapshot
    ): Map<TilePosition, Pair<Int, Int>> = snapshot.items.mapNotNull { item ->
        item.terrain?.let { it.position to (item.index to it.rank) }
    }.toMap()

    private fun debugIdentity(item: RenderItemDebugSnapshot): Any =
        item.terrain?.position ?: item.placedObject ?: requireNotNull(item.entity)
}
