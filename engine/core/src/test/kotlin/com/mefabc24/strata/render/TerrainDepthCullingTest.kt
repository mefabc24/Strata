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
            raisedOffsetY = 0f,
            maxDepth = 398
        )

        assertTrue(2 in depths)
        assertTrue(8 in depths)
        assertTrue(14 in depths)
        assertFalse(15 in depths)
    }

    @Test
    fun `accounts for positive and negative raised offsets`() {
        val normal = visibleDepths(0f)
        val raised = visibleDepths(100f)
        val lowered = visibleDepths(-100f)

        assertFalse(12 in normal)
        assertTrue(12 in raised)

        assertFalse(0 in normal)
        assertTrue(0 in lowered)
    }

    @Test
    fun `unknown sprite height preserves potentially visible deep terrain`() {
        val depths = TerrainDepthCulling.visibleDepths(
            visibleBottom = -120f,
            visibleTop = -110f,
            tileHeight = 32f,
            logicalTileHeight = 64f,
            maxSpriteHeight = Float.POSITIVE_INFINITY,
            raisedOffsetY = 0f,
            maxDepth = 398
        )

        assertFalse(0 in depths)
        assertTrue(2 in depths)
        assertTrue(398 in depths)
    }

    private fun visibleDepths(offset: Float): IntRange {
        return TerrainDepthCulling.visibleDepths(
            visibleBottom = -120f,
            visibleTop = -110f,
            tileHeight = 32f,
            logicalTileHeight = 64f,
            maxSpriteHeight = 64f,
            raisedOffsetY = offset,
            maxDepth = 398
        )
    }
}
