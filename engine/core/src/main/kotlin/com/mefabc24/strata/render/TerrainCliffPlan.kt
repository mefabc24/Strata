package com.mefabc24.strata.render

import com.mefabc24.strata.world.World

/** The two terrain faces visible from Strata's isometric camera. */
internal enum class TerrainCliffSide {
    LEFT,
    RIGHT
}

/**
 * One additional visual cliff segment below a terrain surface.
 *
 * [levelBelowSurface] starts at one. The surface sprite already supplies the
 * first elevation step, so this value describes only extra exposed levels.
 */
internal data class TerrainCliffPart(
    val side: TerrainCliffSide,
    val levelBelowSurface: Int
)

/** Calculates additional exposed cliff segments without changing world data. */
internal object TerrainCliffPlan {

    fun create(
        world: World,
        x: Int,
        y: Int
    ): List<TerrainCliffPart> {
        val currentHeight = world.getHeight(x, y) ?: return emptyList()
        val parts = mutableListOf<TerrainCliffPart>()

        addSide(
            parts = parts,
            side = TerrainCliffSide.LEFT,
            heightDifference = currentHeight - (world.getHeight(x, y + 1) ?: 0)
        )
        addSide(
            parts = parts,
            side = TerrainCliffSide.RIGHT,
            heightDifference = currentHeight - (world.getHeight(x + 1, y) ?: 0)
        )

        return parts
    }

    private fun addSide(
        parts: MutableList<TerrainCliffPart>,
        side: TerrainCliffSide,
        heightDifference: Int
    ) {
        for (level in heightDifference - 1 downTo 1) {
            parts += TerrainCliffPart(
                side = side,
                levelBelowSurface = level
            )
        }
    }
}
