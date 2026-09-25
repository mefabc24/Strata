package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TilePickerTest {

    private class TestTile : Tile

    private val projection = IsoProjection(
        TileGeometry(width = 32f, height = 24f)
    )

    @Test
    fun `picks compact flat tile inside its top face`() {
        val picker = picker(createWorld())
        val center = topFaceCenter(2, 2)

        assertEquals(
            TilePosition(2, 2),
            picker.pickWorld(center.x, center.y)
        )
    }

    @Test
    fun `picks adjacent flat tiles across their shared edge`() {
        val picker = picker(createWorld())
        val secondCenter = topFaceCenter(2, 1)

        assertEquals(
            TilePosition(2, 1),
            picker.pickWorld(secondCenter.x, secondCenter.y)
        )

        val sharedVertex = projection.tileToWorld(2, 1)

        assertEquals(
            TilePosition(2, 1),
            picker.pickWorld(sharedVertex.x, sharedVertex.y)
        )
    }

    @Test
    fun `returns null outside the flat world grid`() {
        val picker = picker(createWorld())
        val outside = topFaceCenter(8, 8)

        assertNull(picker.pickWorld(outside.x, outside.y))
    }

    private fun picker(world: World): TilePicker {
        return TilePicker(
            camera = OrthographicCamera(),
            projection = projection,
            world = world
        )
    }

    private fun topFaceCenter(x: Int, y: Int) =
        projection.tileToWorld(x, y).add(
            0f,
            -projection.tileHeight / 2f
        )

    private fun createWorld(): World {
        return World(5, 5) { _, _ -> TestTile() }
    }
}
