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
    fun `height differences zero through four add only missing levels`() {
        val world = world()

        for (height in 0..4) {
            world.terrain.setHeight(1, 1, height)

            assertEquals(
                height,
                TerrainFillPlan.create(world, 1, 1).size,
                "Unexpected fill count for height difference $height."
            )
        }
    }

    @Test
    fun `both exposed faces share one fill part per level`() {
        val world = world()
        world.terrain.setHeight(1, 1, 3)

        assertEquals(
            listOf(
                TerrainFillPart(2, leftExposed = true, rightExposed = true),
                TerrainFillPart(1, leftExposed = true, rightExposed = true),
                TerrainFillPart(0, leftExposed = true, rightExposed = true)
            ),
            TerrainFillPlan.create(world, 1, 1)
        )
    }

    @Test
    fun `one source fill records asymmetric face exposure`() {
        val world = world()
        world.terrain.setHeight(1, 1, 4)
        world.terrain.setHeight(1, 2, 3)
        world.terrain.setHeight(2, 1, 1)

        assertEquals(
            listOf(
                TerrainFillPart(2, leftExposed = false, rightExposed = true),
                TerrainFillPart(1, leftExposed = false, rightExposed = true),
                TerrainFillPart(0, leftExposed = true, rightExposed = true)
            ),
            TerrainFillPlan.create(world, 1, 1)
        )
    }

    @Test
    fun `different face heights share levels without duplicating full fills`() {
        val world = world()
        world.terrain.setHeight(1, 1, 4)
        world.terrain.setHeight(1, 2, 1)
        world.terrain.setHeight(2, 1, 2)

        assertEquals(
            listOf(
                TerrainFillPart(2, leftExposed = true, rightExposed = false),
                TerrainFillPart(1, leftExposed = true, rightExposed = true),
                TerrainFillPart(0, leftExposed = true, rightExposed = true)
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
    fun `height one at the map edge exposes its first fill`() {
        val world = world()
        world.terrain.setHeight(3, 3, 1)

        assertEquals(
            listOf(
                TerrainFillPart(
                    levelBelowSurface = 0,
                    leftExposed = true,
                    rightExposed = true
                )
            ),
            TerrainFillPlan.create(world, 3, 3)
        )
    }

    @Test
    fun `compact geometry spaces full canvas fills by one logical step`() {
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )

        val first = fillBounds(
            projection = projection,
            elevation = 3,
            level = 0,
            textureHeight = 32
        )
        val second = fillBounds(
            projection = projection,
            elevation = 3,
            level = 1,
            textureHeight = 32
        )

        assertEquals(8f, first.y - second.y)
        assertEquals(8f, projection.elevationStep)
        assertEquals(32f, first.width)
        assertEquals(32f, first.height)
    }

    @Test
    fun `first fill is one logical step below the surface sprite`() {
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )
        val surface = IsoTerrainBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            elevation = 1,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle()
        )
        val firstFill = fillBounds(
            projection = projection,
            elevation = 1,
            level = 0,
            textureHeight = 32
        )

        assertEquals(
            projection.elevationStep,
            surface.y - firstFill.y
        )
    }

    @Test
    fun `fill spacing does not depend on canvas height`() {
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )

        val shortFirst = fillBounds(projection, 3, 0, textureHeight = 8)
        val shortSecond = fillBounds(projection, 3, 1, textureHeight = 8)
        val tallFirst = fillBounds(projection, 3, 0, textureHeight = 48)
        val tallSecond = fillBounds(projection, 3, 1, textureHeight = 48)

        assertEquals(8f, shortFirst.y - shortSecond.y)
        assertEquals(8f, tallFirst.y - tallSecond.y)
    }

    @Test
    fun `one fill canvas splits into complete non overlapping faces`() {
        val even = TerrainFillSlices.calculate(32)
        val odd = TerrainFillSlices.calculate(33)

        assertEquals(16, even.leftWidth)
        assertEquals(16, even.rightWidth)
        assertEquals(32, even.leftWidth + even.rightWidth)
        assertEquals(16, odd.leftWidth)
        assertEquals(17, odd.rightWidth)
        assertEquals(33, odd.leftWidth + odd.rightWidth)
    }

    @Test
    fun `world bounds contain the deepest full canvas fill`() {
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )
        val worldBounds = projection.worldBounds(
            width = 1,
            height = 1,
            maxElevation = 4,
            maxSpriteHeight = 48f
        )
        val deepestFill = fillBounds(
            projection = projection,
            elevation = 4,
            level = 3,
            textureHeight = 48
        )

        assertTrue(deepestFill.y >= worldBounds.y)
        assertTrue(
            deepestFill.y + deepestFill.height <=
                    worldBounds.y + worldBounds.height
        )
    }

    @Test
    fun `fill bounds reject invalid visual data`() {
        val projection = IsoProjection(
            TileGeometry(width = 32f, height = 24f)
        )

        assertFailsWith<IllegalArgumentException> {
            IsoTerrainFillBounds.calculate(
                projection = projection,
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

    private fun world(): World {
        return World(width = 4, height = 4) { _, _ -> TestTile() }
    }

    private fun fillBounds(
        projection: IsoProjection,
        elevation: Int,
        level: Int,
        textureHeight: Int
    ): Rectangle {
        return IsoTerrainFillBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            elevation = elevation,
            levelBelowSurface = level,
            textureWidth = 32,
            textureHeight = textureHeight,
            result = Rectangle()
        )
    }
}
