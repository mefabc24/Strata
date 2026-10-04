package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.debug.DebugWorldVisibilitySettings
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IsoWorldRendererVisibilityTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `visibility suppresses render categories without changing world state`() {
        val pixmap = Pixmap(32, 24, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val visibility = DebugWorldVisibilitySettings()
        val renderer = IsoWorldRenderer(
            projection = IsoProjection(TileGeometry(32f, 24f)),
            collectDebugSnapshot = { true },
            visibility = visibility
        )

        try {
            val world = World(2, 2) { _, _ -> GroundTile }
            world.addOverlayLayer("roads")
            world.addOverlayLayer("decoration")
            world.setOverlayTile("roads", 0, 0, RoadTile)
            world.setOverlayTile("decoration", 1, 1, DecorationTile)
            val placed = requireNotNull(world.place(TestPlaceable, 0, 0))
            val entity = world.addEntity(TestEntity, EntityPosition(1.5f, 0.5f))
            val region = TextureRegion(texture)
            val camera = OrthographicCamera(200f, 200f).apply {
                position.set(0f, -24f, 0f)
                update()
            }
            var objectResolutions = 0
            var entityResolutions = 0

            fun render() = renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> region },
                objectVisualFor = {
                    objectResolutions++
                    ObjectVisual(region)
                },
                entityVisualFor = {
                    entityResolutions++
                    EntityVisual(region)
                }
            )

            render()
            assertEquals(4, renderer.stats.groundTerrainDrawn)
            assertEquals(2, renderer.stats.overlayTerrainDrawn)
            assertEquals(1, renderer.stats.objectsDrawn)
            assertEquals(1, renderer.stats.entitiesDrawn)

            visibility.groundTerrainVisible = false
            visibility.setOverlayLayerVisible("decoration", false)
            visibility.placedObjectsVisible = false
            visibility.entitiesVisible = false
            objectResolutions = 0
            entityResolutions = 0
            render()

            assertEquals(0, renderer.stats.groundTerrainDrawn)
            assertEquals(1, renderer.stats.overlayTerrainDrawn)
            assertEquals(0, renderer.stats.objectsDrawn)
            assertEquals(0, renderer.stats.entitiesDrawn)
            assertEquals(0, objectResolutions)
            assertEquals(0, entityResolutions)
            assertEquals(4, renderer.stats.groundTerrainTotal)
            assertEquals(2, renderer.stats.overlayTerrainTotal)
            assertEquals(1, renderer.stats.objectsTotal)
            assertEquals(1, renderer.stats.entitiesTotal)
            assertSame(placed, world.getObjectAt(0, 0))
            assertSame(RoadTile, world.getOverlayTile("roads", 0, 0))
            assertSame(DecorationTile, world.getOverlayTile("decoration", 1, 1))
            assertEquals(1, world.entityCount)
            assertEquals(EntityPosition(1.5f, 0.5f), entity.position)
            assertTrue(renderer.debugSnapshot!!.items.any {
                it.placedObject === placed && !it.drawn
            })
            assertTrue(renderer.debugSnapshot!!.items.any {
                it.entity === entity && !it.drawn
            })

            visibility.terrainOverlaysVisible = false
            render()
            assertEquals(0, renderer.stats.overlayTerrainDrawn)

            visibility.showAll()
            render()
            assertEquals(4, renderer.stats.groundTerrainDrawn)
            assertEquals(2, renderer.stats.overlayTerrainDrawn)
            assertEquals(1, renderer.stats.objectsDrawn)
            assertEquals(1, renderer.stats.entitiesDrawn)
            assertTrue(visibility.isOverlayLayerVisible("decoration"))
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `visibility defaults preserve all categories and overlay layers`() {
        val visibility = DebugWorldVisibilitySettings()

        assertTrue(visibility.groundTerrainVisible)
        assertTrue(visibility.terrainOverlaysVisible)
        assertTrue(visibility.placedObjectsVisible)
        assertTrue(visibility.entitiesVisible)
        assertTrue(visibility.isOverlayLayerVisible("any-layer"))

        visibility.setOverlayLayerVisible("any-layer", false)
        assertFalse(visibility.isOverlayLayerVisible("any-layer"))
        visibility.setOverlayLayerVisible("any-layer", true)
        assertTrue(visibility.isOverlayLayerVisible("any-layer"))
    }

    private data object GroundTile : Tile
    private data object RoadTile : Tile
    private data object DecorationTile : Tile
    private data object TestEntity : Entity
    private data object TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }
}
