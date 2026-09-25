package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun `picks points near every top face edge`() {
        val picker = picker(createWorld())
        val top = projection.tileToWorld(2, 2)
        val halfWidth = projection.tileWidth / 2f
        val halfHeight = projection.tileHeight / 2f
        val inset = 0.05f

        val points = listOf(
            top.cpy().add(0f, -inset),
            top.cpy().add(0f, -projection.tileHeight + inset),
            top.cpy().add(-halfWidth + inset, -halfHeight),
            top.cpy().add(halfWidth - inset, -halfHeight)
        )

        for (point in points) {
            assertEquals(
                TilePosition(2, 2),
                picker.pickWorld(point.x, point.y)
            )
        }
    }

    @Test
    fun `point outside a face does not pick that tile`() {
        val picker = picker(createWorld())
        val top = projection.tileToWorld(2, 2)

        assertNotEquals(
            TilePosition(2, 2),
            picker.pickWorld(top.x, top.y + 0.05f)
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
    fun `shared edge uses inverse projection candidate consistently`() {
        val picker = picker(createWorld())
        val sharedVertex = projection.tileToWorld(2, 1)
        val candidate = projection.worldToTile(sharedVertex.x, sharedVertex.y)

        assertTrue(
            projection.containsTopFace(
                sharedVertex.x,
                sharedVertex.y,
                candidate.x,
                candidate.y
            )
        )
        assertEquals(candidate, picker.pickWorld(sharedVertex.x, sharedVertex.y))
    }

    @Test
    fun `logical sprite height does not change top face picking`() {
        val compactProjection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )
        val tallProjection = IsoProjection(
            TileGeometry(width = 32f, height = 64f)
        )
        val world = createWorld()
        val compactPicker = picker(world, compactProjection)
        val tallPicker = picker(world, tallProjection)
        val point = compactProjection.tileToWorld(3, 1).add(0f, -8f)

        assertEquals(
            compactPicker.pickWorld(point.x, point.y),
            tallPicker.pickWorld(point.x, point.y)
        )
        assertEquals(
            TilePosition(3, 1),
            compactPicker.pickWorld(point.x, point.y)
        )
    }

    @Test
    fun `surface outside the world returns null`() {
        val picker = picker(createWorld())
        val outsideTop = projection.tileToWorld(0, 0)

        assertNull(picker.pickWorld(outsideTop.x, outsideTop.y + 0.05f))
    }

    @Test
    fun `returns null outside the flat world grid`() {
        val picker = picker(createWorld())
        val outside = topFaceCenter(8, 8)

        assertNull(picker.pickWorld(outside.x, outside.y))
    }

    private fun picker(
        world: World,
        projection: IsoProjection = this.projection
    ): TilePicker {
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
