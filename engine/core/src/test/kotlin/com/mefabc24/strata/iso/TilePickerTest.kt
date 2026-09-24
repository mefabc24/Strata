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
            height = 32f
        )
    )

    @Test
    fun `picks elevated tile at its rendered position`() {
        val world = createWorld()

        world.setHeight(1, 1, 1)

        val picker = TilePicker(
            camera = OrthographicCamera(),
            projection = projection,
            world = world
        )

        val position = projection.tileToWorld(
            x = 1,
            y = 1,
            elevation = 1
        )

        assertEquals(
            TilePosition(1, 1),
            picker.pickWorld(position.x, position.y)
        )
    }

    @Test
    fun `falls back to lower terrain when higher elevation does not match`() {
        val world = createWorld()

        world.setHeight(1, 1, 1)

        val picker = TilePicker(
            camera = OrthographicCamera(),
            projection = projection,
            world = world
        )

        val position = projection.tileToWorld(
            x = 2,
            y = 0
        )

        assertEquals(
            TilePosition(2, 0),
            picker.pickWorld(position.x, position.y)
        )
    }

    private fun createWorld(): World {
        return World(5, 5) { _, _ ->
            TestTile()
        }
    }
}