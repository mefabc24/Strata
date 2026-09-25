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
        val terrainBounds = terrainBounds(projection)
        val objectBounds = objectBounds(
            projection,
            PlacedObject(SingleTileObject(), 0, 0)
        )

        assertEquals(-32f, terrainBounds.y)
        assertEquals(0f, terrainBounds.y + terrainBounds.height)
        assertEquals(-16f, logicalSurfaceY(projection, terrainBounds))
        assertEquals(-16f, objectBounds.y)
    }

    @Test
    fun `compact geometry aligns padded terrain and object sprites`() {
        val projection = projection(height = 24f)
        val terrainBounds = terrainBounds(projection)
        val objectBounds = objectBounds(
            projection,
            PlacedObject(SingleTileObject(), 0, 0)
        )

        assertEquals(-24f, terrainBounds.y)
        assertEquals(8f, terrainBounds.y + terrainBounds.height)
        assertEquals(-16f, logicalSurfaceY(projection, terrainBounds))
        assertEquals(-16f, objectBounds.y)
    }

    @Test
    fun `multi tile footprint uses the front logical terrain surface`() {
        val projection = projection(height = 24f)
        val objectBounds = objectBounds(
            projection,
            PlacedObject(MultiTileObject(), 0, 0)
        )
        val frontTileBounds = terrainBounds(projection, x = 1, y = 1)

        assertEquals(
            logicalSurfaceY(projection, frontTileBounds),
            objectBounds.y
        )
        assertEquals(64f, objectBounds.width)
    }

    @Test
    fun `global and per object offsets are additive`() {
        val projection = projection(height = 24f)
        val placed = PlacedObject(SingleTileObject(), 0, 0)
        val defaults = objectBounds(projection, placed)
        val adjusted = objectBounds(
            projection = projection,
            placed = placed,
            offsetX = -2f,
            offsetY = 1f,
            objectSettings = ObjectRenderingSettings(
                offsetX = 5f,
                offsetY = -3f
            )
        )

        assertEquals(defaults.x + 3f, adjusted.x)
        assertEquals(defaults.y - 2f, adjusted.y)
    }

    @Test
    fun `global object offset is independent of footprint size`() {
        val projection = projection(height = 24f)
        val settings = ObjectRenderingSettings(offsetX = -4f, offsetY = 6f)

        for (placeable in listOf(SingleTileObject(), MultiTileObject())) {
            val placed = PlacedObject(placeable, 0, 0)
            val defaults = objectBounds(projection, placed)
            val adjusted = objectBounds(
                projection,
                placed,
                objectSettings = settings
            )

            assertEquals(defaults.x - 4f, adjusted.x)
            assertEquals(defaults.y + 6f, adjusted.y)
        }
    }

    private fun projection(height: Float) = IsoProjection(
        TileGeometry(width = 32f, height = height)
    )

    private fun terrainBounds(
        projection: IsoProjection,
        x: Int = 0,
        y: Int = 0
    ): Rectangle {
        return IsoTerrainBounds.calculate(
            projection = projection,
            x = x,
            y = y,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle()
        )
    }

    private fun objectBounds(
        projection: IsoProjection,
        placed: PlacedObject,
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
