package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RenderDebugSnapshotTest {
    private data object TestTile : Tile
    private data object TestEntity : Entity

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
            val visible = requireNotNull(renderer.debugSnapshot).items.single()
            assertSame(entity, visible.entity)
            assertTrue(visible.drawn)

            entity.teleport(EntityPosition(1000f, 1000f))
            renderer.render(world, camera, { _, _ -> null }, entityVisualFor = { visual })
            assertFalse(requireNotNull(renderer.debugSnapshot).items.single().drawn)
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }
}
