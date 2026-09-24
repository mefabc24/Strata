package com.mefabc24.strata.iso

import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IsoProjectionTest {

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
    fun `elevation shifts tile vertically`() {
        val position = projection.tileToWorld(
            x = 2,
            y = 3,
            elevation = 2
        )

        assertEquals(-32f, position.x)
        assertEquals(-16f, position.y)
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
    fun `world bounds include terrain elevation and sprite height`() {
        val bounds = projection.worldBounds(
            width = 3,
            height = 2,
            maxElevation = 2,
            maxSpriteHeight = 64f
        )

        assertEquals(-64f, bounds.x)
        assertEquals(-112f, bounds.y)
        assertEquals(160f, bounds.width)
        assertEquals(176f, bounds.height)
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
            maxElevation = 2,
            maxSpriteHeight = 32f
        )

        assertEquals(-32f, bounds.x)
        assertEquals(-48f, bounds.y)
        assertEquals(80f, bounds.width)
        assertEquals(72f, bounds.height)
    }
}
