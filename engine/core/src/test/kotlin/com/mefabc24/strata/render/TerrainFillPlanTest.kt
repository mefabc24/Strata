package com.mefabc24.strata.render

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TerrainFillPlanTest {

    private class TestTile : Tile

    @Test
    fun `height differences create one fill per exposed face and level`() {
        val world = world()

        for (height in 0..4) {
            world.terrain.setHeight(1, 1, height)

            assertEquals(
                height * 2,
                TerrainFillPlan.create(world, 1, 1).size
            )
        }
    }

    @Test
    fun `asymmetric exposure creates only the visible face`() {
        val world = world()
        world.terrain.setHeight(1, 1, 3)
        world.terrain.setHeight(1, 2, 3)

        assertEquals(
            listOf(
                TerrainFillPart(0, TerrainFace.RIGHT),
                TerrainFillPart(1, TerrainFace.RIGHT),
                TerrainFillPart(2, TerrainFace.RIGHT)
            ),
            TerrainFillPlan.create(world, 1, 1)
        )
    }

    @Test
    fun `different neighbor heights expose each face independently`() {
        val world = world()
        world.terrain.setHeight(1, 1, 4)
        world.terrain.setHeight(1, 2, 1)
        world.terrain.setHeight(2, 1, 2)

        assertEquals(
            listOf(
                TerrainFillPart(0, TerrainFace.LEFT),
                TerrainFillPart(0, TerrainFace.RIGHT),
                TerrainFillPart(1, TerrainFace.LEFT),
                TerrainFillPart(1, TerrainFace.RIGHT),
                TerrainFillPart(2, TerrainFace.LEFT)
            ),
            TerrainFillPlan.create(world, 1, 1)
        )
    }

    @Test
    fun `same height plateau has no internal fills`() {
        val world = world()
        world.terrain.setHeight(1..2, 1..2, 3)

        assertEquals(emptyList(), TerrainFillPlan.create(world, 1, 1))
    }

    @Test
    fun `height one at map edge exposes both first fills`() {
        val world = world()
        world.terrain.setHeight(3, 3, 1)

        assertEquals(
            listOf(
                TerrainFillPart(0, TerrainFace.LEFT),
                TerrainFillPart(0, TerrainFace.RIGHT)
            ),
            TerrainFillPlan.create(world, 3, 3)
        )
    }

    @Test
    fun `one fill canvas splits into complete non overlapping faces`() {
        val even = TerrainFillSlices.calculate(32)
        val odd = TerrainFillSlices.calculate(33)

        assertEquals(32, even.leftWidth + even.rightWidth)
        assertEquals(33, odd.leftWidth + odd.rightWidth)
    }

    @Test
    fun `world bounds contain the deepest fill canvas`() {
        val projection = projection()
        val worldBounds = projection.worldBounds(
            width = 1,
            height = 1,
            maxElevation = 4,
            maxSpriteHeight = 48f
        )
        val deepestFill = IsoTerrainFillBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            elevation = 4,
            levelBelowSurface = 3,
            textureWidth = 32,
            textureHeight = 48,
            result = Rectangle()
        )

        assertTrue(deepestFill.y >= worldBounds.y)
        assertTrue(
            deepestFill.y + deepestFill.height <=
                    worldBounds.y + worldBounds.height
        )
    }

    @Test
    fun `fill bounds reject negative levels`() {
        assertFailsWith<IllegalArgumentException> {
            IsoTerrainFillBounds.calculate(
                projection = projection(),
                x = 0,
                y = 0,
                elevation = 1,
                levelBelowSurface = -1,
                textureWidth = 32,
                textureHeight = 32,
                result = Rectangle()
            )
        }
    }

    private fun projection() = IsoProjection(
        TileGeometry(width = 32f, height = 24f)
    )

    private fun world(): World {
        return World(width = 4, height = 4) { _, _ -> TestTile() }
    }
}
