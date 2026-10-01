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

    private fun terrainRanks(snapshot: RenderDebugSnapshot): Map<TilePosition, Int> =
        snapshot.items.mapNotNull { item ->
            item.terrain?.let { it.position to it.rank }
        }.toMap()

    private fun terrainMetadata(
        snapshot: RenderDebugSnapshot
    ): Map<TilePosition, Pair<Int, Int>> = snapshot.items.mapNotNull { item ->
        item.terrain?.let { it.position to (item.index to it.rank) }
    }.toMap()
}
