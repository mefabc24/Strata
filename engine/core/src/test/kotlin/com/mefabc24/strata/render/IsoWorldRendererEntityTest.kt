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
import kotlin.test.assertEquals

class IsoWorldRendererEntityTest {

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `normal rendering draws visible entities and culls distant ones`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(
            IsoProjection(TileGeometry(32f, 24f))
        )

        try {
            val world = World(3, 3) { _, _ -> TestTile }
            val entity = world.addEntity(TestEntity, EntityPosition(1.5f, 1.5f))
            val visual = EntityVisual(TextureRegion(texture))
            val camera = OrthographicCamera(100f, 100f).apply {
                position.set(0f, -24f, 0f)
                update()
            }

            renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> null },
                entityVisualFor = { visual }
            )

            assertEquals(1, renderer.stats.entitiesChecked)
            assertEquals(1, renderer.stats.entitiesDrawn)

            entity.position = EntityPosition(1000f, 1000f)
            renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> null },
                entityVisualFor = { visual }
            )

            assertEquals(1, renderer.stats.entitiesChecked)
            assertEquals(0, renderer.stats.entitiesDrawn)
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    private data object TestEntity : Entity
    private data object TestTile : Tile
}
