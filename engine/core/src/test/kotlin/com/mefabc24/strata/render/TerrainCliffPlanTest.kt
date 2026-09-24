package com.mefabc24.strata.render

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TerrainCliffPlanTest {

    private class TestTile : Tile

    @Test
    fun `height differences zero and one need no additional cliffs`() {
        val world = world()

        assertEquals(emptyList(), TerrainCliffPlan.create(world, 1, 1))

        world.terrain.setHeight(1, 1, 1)

        assertEquals(emptyList(), TerrainCliffPlan.create(world, 1, 1))
    }

    @Test
    fun `height differences two and three add the required levels on both sides`() {
        val world = world()

        world.terrain.setHeight(1, 1, 2)

        assertEquals(
            listOf(
                TerrainCliffPart(TerrainCliffSide.LEFT, 1),
                TerrainCliffPart(TerrainCliffSide.RIGHT, 1)
            ),
            TerrainCliffPlan.create(world, 1, 1)
        )

        world.terrain.setHeight(1, 1, 3)

        assertEquals(
            listOf(
                TerrainCliffPart(TerrainCliffSide.LEFT, 2),
                TerrainCliffPart(TerrainCliffSide.LEFT, 1),
                TerrainCliffPart(TerrainCliffSide.RIGHT, 2),
                TerrainCliffPart(TerrainCliffSide.RIGHT, 1)
            ),
            TerrainCliffPlan.create(world, 1, 1)
        )
    }

    @Test
    fun `only front facing lower neighbors expose cliffs`() {
        val world = world()
        world.terrain.setHeight(1, 1, 3)
        world.terrain.setHeight(1, 2, 3)
        world.terrain.setHeight(2, 1, 1)

        assertEquals(
            listOf(
                TerrainCliffPart(TerrainCliffSide.RIGHT, 1)
            ),
            TerrainCliffPlan.create(world, 1, 1)
        )
    }

    @Test
    fun `same height plateau has no internal cliffs`() {
        val world = world()
        world.terrain.setHeight(1..2, 1..2, 3)

        assertEquals(emptyList(), TerrainCliffPlan.create(world, 1, 1))
    }

    @Test
    fun `compact geometry places each cliff one logical elevation step lower`() {
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )

        val first = cliffBounds(projection, elevation = 3, level = 1)
        val second = cliffBounds(projection, elevation = 3, level = 2)

        assertEquals(8f, first.y - second.y)
        assertEquals(8f, projection.elevationStep)
        assertEquals(32f, first.width)
        assertEquals(8f, first.height)
    }

    @Test
    fun `cliff bounds reject invalid visual data`() {
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )

        assertFailsWith<IllegalArgumentException> {
            IsoCliffBounds.calculate(
                projection = projection,
                x = 0,
                y = 0,
                elevation = 1,
                levelBelowSurface = 0,
                textureWidth = 32,
                textureHeight = 8,
                result = Rectangle()
            )
        }
    }

    private fun world(): World {
        return World(width = 4, height = 4) { _, _ -> TestTile() }
    }

    private fun cliffBounds(
        projection: IsoProjection,
        elevation: Int,
        level: Int
    ): Rectangle {
        return IsoCliffBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            elevation = elevation,
            levelBelowSurface = level,
            textureWidth = 32,
            textureHeight = 8,
            result = Rectangle()
        )
    }
}
