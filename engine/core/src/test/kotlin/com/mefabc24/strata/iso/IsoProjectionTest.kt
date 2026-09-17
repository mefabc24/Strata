
package com.mefabc24.strata.iso

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IsoProjectionTest {

    private val projection = IsoProjection(64f, 32f)

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
                val result = projection.worldToTile(position.x, position.y)

                assertEquals(x to y, result)
            }
        }
    }

    @Test
    fun `tile dimensions must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            IsoProjection(0f, 32f)
        }

        assertFailsWith<IllegalArgumentException> {
            IsoProjection(64f, -1f)
        }
    }
}