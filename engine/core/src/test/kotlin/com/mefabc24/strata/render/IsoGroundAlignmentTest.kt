package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoGroundAlignmentTest {

    private class SingleTileObject : Placeable {
        override val footprint = Footprint.square(1)
    }

    private class MultiTileObject : Placeable {
        override val footprint = Footprint.square(2)
    }

    @Test
    fun `standard geometry retains its terrain and object anchors`() {
        val projection = projection(height = 32f)
        val terrainBounds = terrainBounds(projection, elevation = 0)
        val objectBounds = objectBounds(
            projection = projection,
            placed = PlacedObject(SingleTileObject(), 0, 0),
            elevation = 0
        )

        assertEquals(-32f, terrainBounds.y)
        assertEquals(0f, terrainBounds.y + terrainBounds.height)
        assertEquals(-16f, logicalSurfaceY(projection, terrainBounds))
        assertEquals(-16f, objectBounds.y)
    }

    @Test
    fun `compact geometry aligns a padded texture and object at every elevation`() {
        val projection = projection(height = 24f)

        for (elevation in 0..2) {
            val terrainBounds = terrainBounds(projection, elevation)
            val objectBounds = objectBounds(
                projection = projection,
                placed = PlacedObject(SingleTileObject(), 0, 0),
                elevation = elevation
            )

            val expectedSurfaceY = -16f + elevation * 8f

            assertEquals(-24f + elevation * 8f, terrainBounds.y)
            assertEquals(8f + elevation * 8f, terrainBounds.y + terrainBounds.height)
            assertEquals(expectedSurfaceY, logicalSurfaceY(projection, terrainBounds))
            assertEquals(expectedSurfaceY, objectBounds.y)
        }
    }

    @Test
    fun `multi tile footprint uses the front logical terrain surface`() {
        val projection = projection(height = 24f)
        val placed = PlacedObject(MultiTileObject(), 0, 0)
        val objectBounds = objectBounds(
            projection = projection,
            placed = placed,
            elevation = 2
        )

        val frontTileBounds = terrainBounds(
            projection = projection,
            elevation = 2,
            x = 1,
            y = 1
        )

        assertEquals(
            logicalSurfaceY(projection, frontTileBounds),
            objectBounds.y
        )
        assertEquals(64f, objectBounds.width)
    }

    @Test
    fun `object offset remains an additional artistic adjustment`() {
        val projection = projection(height = 24f)
        val placed = PlacedObject(SingleTileObject(), 0, 0)

        val defaultBounds = objectBounds(
            projection = projection,
            placed = placed,
            elevation = 1
        )
        val adjustedBounds = objectBounds(
            projection = projection,
            placed = placed,
            elevation = 1,
            offsetY = 5f
        )

        assertEquals(defaultBounds.y + 5f, adjustedBounds.y)
    }

    @Test
    fun `global object offset moves bounds at every elevation`() {
        val projection = projection(height = 24f)
        val placed = PlacedObject(SingleTileObject(), 0, 0)
        val settings = ObjectRenderingSettings(
            offsetX = 4f,
            offsetY = -3f
        )

        for (elevation in listOf(0, 2)) {
            val defaultBounds = objectBounds(
                projection = projection,
                placed = placed,
                elevation = elevation
            )
            val adjustedBounds = objectBounds(
                projection = projection,
                placed = placed,
                elevation = elevation,
                objectSettings = settings
            )

            assertEquals(defaultBounds.x + 4f, adjustedBounds.x)
            assertEquals(defaultBounds.y - 3f, adjustedBounds.y)
        }
    }

    @Test
    fun `global and per object offsets are additive`() {
        val projection = projection(height = 24f)
        val placed = PlacedObject(SingleTileObject(), 0, 0)
        val bounds = objectBounds(
            projection = projection,
            placed = placed,
            elevation = 1,
            offsetX = -2f,
            offsetY = 1f,
            objectSettings = ObjectRenderingSettings(
                offsetX = 5f,
                offsetY = -3f
            )
        )
        val defaults = objectBounds(
            projection = projection,
            placed = placed,
            elevation = 1
        )

        assertEquals(defaults.x + 3f, bounds.x)
        assertEquals(defaults.y - 2f, bounds.y)
    }

    @Test
    fun `global object offset is independent of footprint size`() {
        val projection = projection(height = 24f)
        val settings = ObjectRenderingSettings(
            offsetX = -4f,
            offsetY = 6f
        )

        for (placeable in listOf(
            SingleTileObject(),
            MultiTileObject()
        )) {
            val placed = PlacedObject(placeable, 0, 0)
            val defaults = objectBounds(
                projection = projection,
                placed = placed,
                elevation = 1
            )
            val adjusted = objectBounds(
                projection = projection,
                placed = placed,
                elevation = 1,
                objectSettings = settings
            )

            assertEquals(defaults.x - 4f, adjusted.x)
            assertEquals(defaults.y + 6f, adjusted.y)
        }
    }

    private fun projection(height: Float): IsoProjection {
        return IsoProjection(
            TileGeometry(
                width = 32f,
                height = height
            )
        )
    }

    private fun terrainBounds(
        projection: IsoProjection,
        elevation: Int,
        x: Int = 0,
        y: Int = 0
    ): Rectangle {
        return IsoTerrainBounds.calculate(
            projection = projection,
            x = x,
            y = y,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle(),
            elevation = elevation
        )
    }

    private fun objectBounds(
        projection: IsoProjection,
        placed: PlacedObject,
        elevation: Int,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        objectSettings: ObjectRenderingSettings = ObjectRenderingSettings()
    ): Rectangle {
        return IsoObjectBounds.calculate(
            projection = projection,
            placed = placed,
            visual = ObjectVisual(
                texture = TextureRegion(),
                offsetX = offsetX,
                offsetY = offsetY,
                height = 32f
            ),
            result = Rectangle(),
            elevation = elevation,
            objectSettings = objectSettings
        )
    }

    private fun logicalSurfaceY(
        projection: IsoProjection,
        terrainBounds: Rectangle
    ): Float {
        return terrainBounds.y +
                projection.logicalTileHeight -
                projection.tileHeight
    }
}
