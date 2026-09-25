package com.mefabc24.strata.render

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerrainDepthCullingTest {

    @Test
    fun `accounts for texture overhang above logical terrain`() {
        val depths = TerrainDepthCulling.visibleDepths(
            visibleBottom = -120f,
            visibleTop = -100f,
            tileHeight = 32f,
            logicalTileHeight = 64f,
            maxSpriteHeight = 160f,
            maxDepth = 398
        )

        assertTrue(2 in depths)
        assertTrue(8 in depths)
        assertTrue(14 in depths)
        assertFalse(15 in depths)
    }

    @Test
    fun `unknown sprite height preserves potentially visible deep terrain`() {
        val depths = TerrainDepthCulling.visibleDepths(
            visibleBottom = -120f,
            visibleTop = -110f,
            tileHeight = 32f,
            logicalTileHeight = 64f,
            maxSpriteHeight = Float.POSITIVE_INFINITY,
            maxDepth = 398
        )

        assertFalse(0 in depths)
        assertTrue(2 in depths)
        assertTrue(398 in depths)
    }

    @Test
    fun `compact geometry keeps padded terrain sprites visible`() {
        val depths = TerrainDepthCulling.visibleDepths(
            visibleBottom = -96f,
            visibleTop = -88f,
            tileHeight = 16f,
            logicalTileHeight = 24f,
            maxSpriteHeight = 32f,
            maxDepth = 20
        )

        assertTrue(10 in depths)
    }
}
