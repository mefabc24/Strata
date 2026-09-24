package com.mefabc24.strata.render

import com.mefabc24.strata.world.World

/**
 * One repeated elevation-fill level and the visible faces needed at it.
 *
 * [levelBelowSurface] starts at one. The surface sprite already supplies the
 * first elevation step, so these parts describe only additional exposed
 * levels.
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
        val deepestLevel = maxOf(leftLevels, rightLevels)

        return buildList(deepestLevel) {
            for (level in deepestLevel downTo 1) {
                add(
                    TerrainFillPart(
                        levelBelowSurface = level,
                        leftExposed = level <= leftLevels,
                        rightExposed = level <= rightLevels
                    )
                )
            }
        }
    }

    private fun additionalLevels(heightDifference: Int): Int {
        return maxOf(0, heightDifference - 1)
    }
}
