package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoWorldRendererTerrainStatsTest {

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `tracks drawn ground and overlay terrain separately`() {
        val pixmap = Pixmap(32, 24, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(
            IsoProjection(TileGeometry(32f, 24f))
        )

        try {
            val world = World(2, 2) { _, _ -> GroundTile }
            world.addOverlayLayer("decoration")
            world.setOverlayTile("decoration", 0, 0, OverlayTile)
            world.setOverlayTile("decoration", 1, 1, OverlayTile)
            val camera = OrthographicCamera(200f, 200f).apply {
                position.set(0f, -24f, 0f)
                update()
            }

            renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> TextureRegion(texture) }
            )

            assertEquals(4, renderer.stats.groundTerrainDrawn)
            assertEquals(2, renderer.stats.overlayTerrainDrawn)
            assertEquals(6, renderer.stats.terrainDrawn)
            assertEquals(8, renderer.stats.terrainChecked)
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    private data object GroundTile : Tile
    private data object OverlayTile : Tile
}
