package com.mefabc24.strata.iso

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

                assertEquals(x to y, result)
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
}