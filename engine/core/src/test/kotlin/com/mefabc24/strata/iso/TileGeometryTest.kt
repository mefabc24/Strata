package com.mefabc24.strata.iso

import kotlin.test.Test
import kotlin.test.assertEquals

class TileGeometryTest {

    @Test
    fun `derives face height from tile width`() {
        val geometry = TileGeometry(
            width = 64f,
            height = 64f
        )

        assertEquals(32f, geometry.faceHeight)
    }

    @Test
    fun `keeps logical height independent from face height`() {
        val geometry = TileGeometry(
            width = 64f,
            height = 48f
        )

        assertEquals(32f, geometry.faceHeight)
        assertEquals(48f, geometry.height)
    }
}
