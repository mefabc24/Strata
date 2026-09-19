package com.mefabc24.strata.render

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerrainDepthCullingTest {

    @Test
    fun `includes tall sprites from shallow terrain depths`() {
        val depths = TerrainDepthCulling.visibleDepths(
            visibleBottom = -120f,
            visibleTop = -100f,
            tileHeight = 32f,
            maxSpriteHeight = 160f,
            raisedOffsetY = 0f,
            maxDepth = 398
        )

        assertTrue(0 in depths)
        assertTrue(8 in depths)
        assertFalse(10 in depths)
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
    fun `unknown sprite height preserves shallow depths`() {
        val depths = TerrainDepthCulling.visibleDepths(
            visibleBottom = -120f,
            visibleTop = -110f,
            tileHeight = 32f,
            maxSpriteHeight = Float.POSITIVE_INFINITY,
            raisedOffsetY = 0f,
            maxDepth = 398
        )

        assertTrue(0 in depths)
    }

    private fun visibleDepths(offset: Float): IntRange {
        return TerrainDepthCulling.visibleDepths(
            visibleBottom = -120f,
            visibleTop = -110f,
            tileHeight = 32f,
            maxSpriteHeight = 32f,
            raisedOffsetY = offset,
            maxDepth = 398
        )
    }
}