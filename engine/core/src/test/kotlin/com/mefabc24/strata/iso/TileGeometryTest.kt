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
    fun `derives elevation step from tile geometry`() {
        val geometry = TileGeometry(
            width = 64f,
            height = 48f
        )

        assertEquals(32f, geometry.faceHeight)
        assertEquals(16f, geometry.elevationStep)
    }

    @Test
    fun `square tile uses remaining half as elevation step`() {
        val geometry = TileGeometry(
            width = 32f,
            height = 32f
        )

        assertEquals(16f, geometry.faceHeight)
        assertEquals(16f, geometry.elevationStep)
    }
}