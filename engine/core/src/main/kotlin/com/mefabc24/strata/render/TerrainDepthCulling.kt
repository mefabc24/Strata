package com.mefabc24.strata.render

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Calculates a conservative range of terrain depths that may be visible.
 */
internal object TerrainDepthCulling {

    fun visibleDepths(
        visibleBottom: Float,
        visibleTop: Float,
        tileHeight: Float,
        maxSpriteHeight: Float,
        raisedOffsetY: Float,
        maxDepth: Int,
        maxElevationOffset: Float = 0f
    ): IntRange {
        require(maxDepth >= 0)
        require(tileHeight > 0f && tileHeight.isFinite())
        require(maxSpriteHeight > 0f && !maxSpriteHeight.isNaN())
        require(raisedOffsetY.isFinite())
        require(maxElevationOffset >= 0f && maxElevationOffset.isFinite())

        val halfTileHeight = tileHeight / 2f

        val minOffset = minOf(0f, raisedOffsetY)
        val maxOffset = maxOf(0f, raisedOffsetY) + maxElevationOffset

        val firstDepth = if (maxSpriteHeight.isInfinite()) {
            0
        } else {
            floor(
                (minOffset - maxSpriteHeight - visibleTop) /
                        halfTileHeight
            ).toInt().coerceIn(0, maxDepth + 1)
        }

        val lastDepth = ceil(
            (maxOffset - visibleBottom) / halfTileHeight
        ).toInt().coerceIn(-1, maxDepth)

        return firstDepth..lastDepth
    }
}