package com.mefabc24.strata.iso

import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.random.Random

class IsoProjectionTest {

    @Test
    fun `float tile positions project centers and interpolate linearly`() {
        val projection = IsoProjection(
            TileGeometry(width = 64f, height = 32f)
        )

        assertEquals(Vector2(0f, -16f), projection.tileToWorld(0.5f, 0.5f))
        assertEquals(Vector2(32f, -32f), projection.tileToWorld(1.5f, 0.5f))
        assertEquals(Vector2(16f, -24f), projection.tileToWorld(1f, 0.5f))
    }

    @Test
    fun `integer projection behavior remains unchanged`() {
        val projection = IsoProjection(
            TileGeometry(width = 64f, height = 32f)
        )

        assertEquals(Vector2(32f, -16f), projection.tileToWorld(1, 0))
        assertEquals(Vector2(-32f, -16f), projection.tileToWorld(0, 1))
    }

    private val projection = IsoProjection(
        TileGeometry(
            width = 64f,
            height = 64f
        )
    )

    @Test
    fun `origin maps to world origin`() {
        val position = projection.tileToWorld(0, 0)

        assertEquals(0f, position.x)
        assertEquals(0f, position.y)
    }

    @Test
    fun `tile coordinates map to expected world position`() {
        val position = projection.tileToWorld(2, 3)

        assertEquals(-32f, position.x)
        assertEquals(-80f, position.y)
    }

    @Test
    fun `tile origins can be converted back to tile coordinates`() {
        for (y in 0 until 10) {
            for (x in 0 until 10) {
                val position = projection.tileToWorld(x, y)

                val result = projection.worldToTile(
                    position.x,
                    position.y
                )

                assertEquals(
                    TilePosition(x, y),
                    result
                )
            }
        }
    }

    @Test
    fun `tile dimensions must be valid`() {
        assertFailsWith<IllegalArgumentException> {
            IsoProjection(
                TileGeometry(
                    width = 0f,
                    height = 64f
                )
            )
        }

        assertFailsWith<IllegalArgumentException> {
            IsoProjection(
                TileGeometry(
                    width = 64f,
                    height = 16f
                )
            )
        }
    }

    @Test
    fun `world bounds include logical terrain and sprite height`() {
        val bounds = projection.worldBounds(
            width = 3,
            height = 2,
            maxSpriteHeight = 64f
        )

        assertEquals(-64f, bounds.x)
        assertEquals(-112f, bounds.y)
        assertEquals(160f, bounds.width)
        assertEquals(112f, bounds.height)
    }

    @Test
    fun `compact world bounds include texture overhang above logical terrain`() {
        val compactProjection = IsoProjection(
            TileGeometry(
                width = 32f,
                height = 24f
            )
        )

        val bounds = compactProjection.worldBounds(
            width = 3,
            height = 2,
            maxSpriteHeight = 32f
        )

        assertEquals(-32f, bounds.x)
        assertEquals(-48f, bounds.y)
        assertEquals(80f, bounds.width)
        assertEquals(56f, bounds.height)
    }

    @Test
    fun `fixed seed continuous coordinates round trip to their containing tile`() {
        val random = Random(20261001)

        repeat(1_000) {
            val tileX = random.nextInt(-100, 101)
            val tileY = random.nextInt(-100, 101)
            val x = tileX + random.nextFloat() * 0.8f + 0.1f
            val y = tileY + random.nextFloat() * 0.8f + 0.1f
            val world = projection.tileToWorld(x, y)

            assertEquals(
                TilePosition(tileX, tileY),
                projection.worldToTile(world.x, world.y)
            )
        }
    }

    @Test
    fun `allocation free projection writes into and returns supplied vector`() {
        val result = Vector2(999f, 999f)

        val returned = projection.tileToWorld(2.5f, 1.5f, result)

        assertSame(result, returned)
        assertEquals(Vector2(32f, -64f), result)
    }

    @Test
    fun `top face includes vertices and edges but excludes nearby exterior points`() {
        val top = projection.tileToWorld(2, 3)
        val halfWidth = projection.tileWidth / 2f
        val halfHeight = projection.tileHeight / 2f
        val centerY = top.y - halfHeight

        val boundary = listOf(
            top.x to top.y,
            top.x to top.y - projection.tileHeight,
            top.x - halfWidth to centerY,
            top.x + halfWidth to centerY,
            top.x + halfWidth / 2f to centerY + halfHeight / 2f
        )
        boundary.forEach { (x, y) ->
            assertTrue(projection.containsTopFace(x, y, 2, 3), "$x,$y")
        }
        assertFalse(projection.containsTopFace(top.x + halfWidth + 0.01f, centerY, 2, 3))
        assertFalse(projection.containsTopFace(top.x, top.y + 0.01f, 2, 3))
    }

    @Test
    fun `world bounds apply padding on every side`() {
        val unpadded = projection.worldBounds(4, 3, maxSpriteHeight = 80f)
        val padded = projection.worldBounds(4, 3, padding = 7.5f, maxSpriteHeight = 80f)

        assertEquals(unpadded.x - 7.5f, padded.x)
        assertEquals(unpadded.y - 7.5f, padded.y)
        assertEquals(unpadded.width + 15f, padded.width)
        assertEquals(unpadded.height + 15f, padded.height)
    }

    @Test
    fun `sprite shorter than logical terrain does not shrink world bounds`() {
        val shortSprite = projection.worldBounds(2, 2, maxSpriteHeight = 16f)
        val logicalHeight = projection.worldBounds(2, 2, maxSpriteHeight = 64f)

        assertEquals(logicalHeight, shortSprite)
    }

    @Test
    fun `world bounds reject invalid dimensions padding and sprite height`() {
        assertFailsWith<IllegalArgumentException> { projection.worldBounds(0, 1) }
        assertFailsWith<IllegalArgumentException> { projection.worldBounds(1, -1) }
        for (padding in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                projection.worldBounds(1, 1, padding = padding)
            }
        }
        for (height in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                projection.worldBounds(1, 1, maxSpriteHeight = height)
            }
        }
    }

    @Test
    fun `tile step length follows configured two to one face geometry`() {
        val small = IsoProjection(TileGeometry(width = 40f, height = 30f))

        assertEquals(22.36068f, small.tileStepLength, absoluteTolerance = 0.00001f)
    }
}
