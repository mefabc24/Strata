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

    @Test
    fun `elevation step follows logical height independently of texture size`() {
        val expectedSteps = mapOf(
            24f to 8f,
            32f to 16f,
            40f to 24f,
            48f to 32f
        )

        for ((height, expectedStep) in expectedSteps) {
            val geometry = TileGeometry(
                width = 32f,
                height = height
            )

            assertEquals(expectedStep, geometry.elevationStep)
        }
    }
}
