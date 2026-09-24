package com.mefabc24.strata.render

import com.mefabc24.strata.world.World

/**
 * One repeated elevation-fill level and the visible faces needed at it.
 *
 * [levelBelowSurface] starts at zero for the first step directly beneath the
 * terrain surface.
 */
internal data class TerrainFillPart(
    val levelBelowSurface: Int,
    val leftExposed: Boolean,
    val rightExposed: Boolean
)

/** Calculates exposed elevation fills without changing world data. */
internal object TerrainFillPlan {

    fun create(
        world: World,
        x: Int,
        y: Int
    ): List<TerrainFillPart> {
        val currentHeight = world.getHeight(x, y) ?: return emptyList()
        val leftLevels = additionalLevels(
            currentHeight - (world.getHeight(x, y + 1) ?: 0)
        )
        val rightLevels = additionalLevels(
            currentHeight - (world.getHeight(x + 1, y) ?: 0)
        )
        val levelCount = maxOf(leftLevels, rightLevels)

        return buildList(levelCount) {
            for (level in levelCount - 1 downTo 0) {
                add(
                    TerrainFillPart(
                        levelBelowSurface = level,
                        leftExposed = level < leftLevels,
                        rightExposed = level < rightLevels
                    )
                )
            }
        }
    }

    private fun additionalLevels(heightDifference: Int): Int {
        return maxOf(0, heightDifference)
    }
}
