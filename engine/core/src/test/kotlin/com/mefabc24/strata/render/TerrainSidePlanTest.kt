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

class TerrainSidePlanTest {

    private class TestTile : Tile

    @Test
    fun `height differences create one primitive per exposed face and level`() {
        val world = world()

        for (height in 0..4) {
            world.terrain.setHeight(1, 1, height)

            assertEquals(
                height * 2,
                TerrainSidePlan.create(world, 1, 1).size
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
                TerrainSidePart(2, TerrainFace.RIGHT),
                TerrainSidePart(1, TerrainFace.RIGHT),
                TerrainSidePart(0, TerrainFace.RIGHT)
            ),
            TerrainSidePlan.create(world, 1, 1)
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
                TerrainSidePart(2, TerrainFace.LEFT),
                TerrainSidePart(1, TerrainFace.LEFT),
                TerrainSidePart(1, TerrainFace.RIGHT),
                TerrainSidePart(0, TerrainFace.LEFT),
                TerrainSidePart(0, TerrainFace.RIGHT)
            ),
            TerrainSidePlan.create(world, 1, 1)
        )
    }

    @Test
    fun `same height plateau has no internal sides`() {
        val world = world()
        world.terrain.setHeight(1..2, 1..2, 3)

        assertEquals(emptyList(), TerrainSidePlan.create(world, 1, 1))
    }

    @Test
    fun `height one at the map edge exposes both first faces`() {
        val world = world()
        world.terrain.setHeight(3, 3, 1)

        assertEquals(
            listOf(
                TerrainSidePart(0, TerrainFace.LEFT),
                TerrainSidePart(0, TerrainFace.RIGHT)
            ),
            TerrainSidePlan.create(world, 3, 3)
        )
    }

    @Test
    fun `compact geometry spaces fill canvases by one logical step`() {
        val projection = projection()
        val first = fillBounds(projection, elevation = 3, level = 0)
        val second = fillBounds(projection, elevation = 3, level = 1)

        assertEquals(8f, first.y - second.y)
        assertEquals(8f, projection.elevationStep)
    }

    @Test
    fun `first fill uses the surface canvas alignment`() {
        val projection = projection()
        val surface = IsoTerrainBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle(),
            elevation = 1
        )
        val firstFill = fillBounds(projection, elevation = 1, level = 0)

        assertEquals(surface, firstFill)
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

    private fun fillBounds(
        projection: IsoProjection,
        elevation: Int,
        level: Int
    ): Rectangle {
        return IsoTerrainFillBounds.calculate(
            projection = projection,
            x = 0,
            y = 0,
            elevation = elevation,
            levelBelowSurface = level,
            textureWidth = 32,
            textureHeight = 32,
            result = Rectangle()
        )
    }
}
