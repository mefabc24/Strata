package com.mefabc24.strata.render.terrain

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
        logicalTileHeight: Float,
        maxSpriteHeight: Float,
        maxDepth: Int
    ): IntRange {
        require(maxDepth >= 0)
        require(tileHeight > 0f && tileHeight.isFinite())
        require(logicalTileHeight >= tileHeight && logicalTileHeight.isFinite())
        require(maxSpriteHeight > 0f && !maxSpriteHeight.isNaN())

        val halfTileHeight = tileHeight / 2f
        val visualOverhang = maxOf(
            0f,
            maxSpriteHeight - logicalTileHeight
        )

        val firstDepth = floor(
            (-logicalTileHeight - visibleTop) /
                    halfTileHeight
        ).toInt().coerceIn(0, maxDepth + 1)

        val lastDepth = if (visualOverhang.isInfinite()) {
            maxDepth
        } else {
            ceil(
                (visualOverhang - visibleBottom) /
                        halfTileHeight
            ).toInt().coerceIn(-1, maxDepth)
        }

        return firstDepth..lastDepth
    }
}
