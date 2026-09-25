package com.mefabc24.strata.render

import com.mefabc24.strata.world.World

/** The two terrain faces visible from Strata's isometric camera. */
internal enum class TerrainFace {
    LEFT,
    RIGHT
}

/** One atomic exposed vertical face at one terrain elevation interval. */
internal data class TerrainSidePart(
    val levelBelowSurface: Int,
    val face: TerrainFace
)

/** Calculates exposed terrain sides without changing world data. */
internal object TerrainSidePlan {

    fun create(
        world: World,
        x: Int,
        y: Int
    ): List<TerrainSidePart> {
        val currentHeight = world.getHeight(x, y) ?: return emptyList()
        val leftLevels = exposedLevels(
            currentHeight - (world.getHeight(x, y + 1) ?: 0)
        )
        val rightLevels = exposedLevels(
            currentHeight - (world.getHeight(x + 1, y) ?: 0)
        )
        val levelCount = maxOf(leftLevels, rightLevels)

        return buildList(leftLevels + rightLevels) {
            for (level in levelCount - 1 downTo 0) {
                if (level < leftLevels) {
                    add(TerrainSidePart(level, TerrainFace.LEFT))
                }

                if (level < rightLevels) {
                    add(TerrainSidePart(level, TerrainFace.RIGHT))
                }
            }
        }
    }

    private fun exposedLevels(heightDifference: Int): Int {
        return maxOf(0, heightDifference)
    }
}
