package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals

class TilePickerTest {

    private class TestTile : Tile

    private val projection = IsoProjection(
        TileGeometry(
            width = 32f,
            height = 24f
        )
    )

    @Test
    fun `picks flat tile inside its top face`() {
        val world = createWorld()
        val picker = picker(world)
        val center = topFaceCenter(x = 2, y = 2, elevation = 0)

        assertEquals(
            TilePosition(2, 2),
            picker.pickWorld(center.x, center.y)
        )
    }

    @Test
    fun `picks elevated tile inside its rendered top face`() {
        val world = createWorld()

        world.setHeight(1, 1, 1)

        val picker = picker(world)
        val center = topFaceCenter(x = 1, y = 1, elevation = 1)

        assertEquals(
            TilePosition(1, 1),
            picker.pickWorld(center.x, center.y)
        )
    }

    @Test
    fun `picks adjacent elevated tiles across their shared edge`() {
        val world = createWorld()

        world.setHeight(1, 1, 1)
        world.setHeight(2, 1, 1)

        val picker = picker(world)
        val secondCenter = topFaceCenter(x = 2, y = 1, elevation = 1)

        assertEquals(
            TilePosition(2, 1),
            picker.pickWorld(secondCenter.x, secondCenter.y)
        )

        val sharedVertex = projection.tileToWorld(
            x = 2,
            y = 1,
            elevation = 1
        )

        assertEquals(
            TilePosition(2, 1),
            picker.pickWorld(sharedVertex.x, sharedVertex.y)
        )
    }

    @Test
    fun `picks multiple compact terrain elevation levels`() {
        val world = createWorld()

        world.setHeight(1, 1, 1)
        world.setHeight(3, 2, 2)

        val picker = picker(world)

        for ((position, elevation) in listOf(
            TilePosition(4, 0) to 0,
            TilePosition(1, 1) to 1,
            TilePosition(3, 2) to 2
        )) {
            val center = topFaceCenter(
                x = position.x,
                y = position.y,
                elevation = elevation
            )

            assertEquals(
                position,
                picker.pickWorld(center.x, center.y)
            )
        }
    }

    @Test
    fun `keeps a raised tile picked across its visible logical side`() {
        val world = createWorld()
        val picker = picker(world)

        val originalCenter = topFaceCenter(
            x = 2,
            y = 2,
            elevation = 0
        )

        world.setHeight(2, 2, 2)

        assertEquals(
            TilePosition(2, 2),
            picker.pickWorld(originalCenter.x, originalCenter.y)
        )
    }

    @Test
    fun `base grid ignores the target tile elevation`() {
        val world = createWorld()
        val picker = picker(world)
        val baseCenter = topFaceCenter(2, 2, elevation = 0)

        world.setHeight(2, 2, 5)

        assertEquals(
            TilePosition(2, 2),
            picker.pickWorld(
                baseCenter.x,
                baseCenter.y,
                TilePickingMode.BASE_GRID
            )
        )
    }

    @Test
    fun `base grid result remains stable while terrain height changes`() {
        val world = createWorld()
        val picker = picker(world)
        val baseCenter = topFaceCenter(3, 1, elevation = 0)

        for (height in listOf(0, 1, 2, 5, 0)) {
            world.setHeight(3, 1, height)

            assertEquals(
                TilePosition(3, 1),
                picker.pickWorld(
                    baseCenter.x,
                    baseCenter.y,
                    TilePickingMode.BASE_GRID
                )
            )
        }
    }

    @Test
    fun `surface and base grid can resolve different coordinates`() {
        val world = createWorld()
        val picker = picker(world)
        val elevated = TilePosition(1, 1)

        world.setHeight(elevated.x, elevated.y, 2)
        val visibleCenter = topFaceCenter(
            elevated.x,
            elevated.y,
            elevation = 2
        )

        assertEquals(
            elevated,
            picker.pickWorld(
                visibleCenter.x,
                visibleCenter.y,
                TilePickingMode.SURFACE
            )
        )
        assertEquals(
            projection.worldToTile(visibleCenter.x, visibleCenter.y),
            picker.pickWorld(
                visibleCenter.x,
                visibleCenter.y,
                TilePickingMode.BASE_GRID
            )
        )
    }

    @Test
    fun `base grid ignores elevated terrain overlapping another base cell`() {
        val world = createWorld()
        val picker = picker(world)
        val elevated = TilePosition(1, 1)

        world.setHeight(elevated.x, elevated.y, 2)
        val elevatedCenter = topFaceCenter(
            elevated.x,
            elevated.y,
            elevation = 2
        )
        val baseCell = projection.worldToTile(
            elevatedCenter.x,
            elevatedCenter.y
        )

        assertEquals(
            elevated,
            picker.pickWorld(
                elevatedCenter.x,
                elevatedCenter.y,
                TilePickingMode.SURFACE
            )
        )
        assertEquals(
            baseCell,
            picker.pickWorld(
                elevatedCenter.x,
                elevatedCenter.y,
                TilePickingMode.BASE_GRID
            )
        )
    }

    @Test
    fun `falls back to lower terrain when elevated candidate does not match`() {
        val world = createWorld()

        world.setHeight(1, 1, 1)

        val picker = picker(world)

        val position = projection.tileToWorld(
            x = 2,
            y = 0
        )

        assertEquals(
            TilePosition(2, 0),
            picker.pickWorld(position.x, position.y)
        )
    }

    private fun picker(world: World): TilePicker {
        return TilePicker(
            camera = OrthographicCamera(),
            projection = projection,
            world = world
        )
    }

    private fun topFaceCenter(
        x: Int,
        y: Int,
        elevation: Int
    ) = projection.tileToWorld(x, y, elevation).add(
        0f,
        -projection.tileHeight / 2f
    )

    private fun createWorld(): World {
        return World(5, 5) { _, _ ->
            TestTile()
        }
    }
}
