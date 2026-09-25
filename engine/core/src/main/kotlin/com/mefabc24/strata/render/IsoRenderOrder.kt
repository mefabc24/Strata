package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection

/** An explicit ordering rule between two primitive indices. */
internal data class IsoRenderDependency(
    val before: Int,
    val after: Int
)

/** Orders all isometric primitives through their shared spatial model. */
internal object IsoRenderOrder {

    fun <T : IsoSortable> backToFront(
        items: List<T>,
        projection: IsoProjection,
        explicitDependencies: List<IsoRenderDependency> = emptyList()
    ): List<T> {
        val dependencies = StableDependencyOrder(
            items = items,
            comparator = comparator(projection)
        )

        for (firstIndex in items.indices) {
            for (secondIndex in firstIndex + 1 until items.size) {
                when (
                    items[firstIndex].sortVolume.relationTo(
                        items[secondIndex].sortVolume
                    )
                ) {
                    IsoSpatialRelation.BEHIND -> {
                        dependencies.add(firstIndex, secondIndex)
                    }

                    IsoSpatialRelation.IN_FRONT -> {
                        dependencies.add(secondIndex, firstIndex)
                    }

                    IsoSpatialRelation.AMBIGUOUS -> Unit
                }
            }
        }

        for ((before, after) in explicitDependencies) {
            dependencies.add(before, after)
        }

        return dependencies.resolve()
    }

    private fun <T : IsoSortable> comparator(
        projection: IsoProjection
    ): Comparator<T> {
        return compareByDescending<T> {
            it.sortVolume.projectedFrontY(projection)
        }.thenBy { it.sortVolume.minZ }
            .thenBy { it.sortVolume.minY }
            .thenBy { it.sortVolume.minX }
            .thenBy { it.sortVolume.maxZ }
            .thenBy { it.sortVolume.maxY }
            .thenBy { it.sortVolume.maxX }
            .thenBy { it.sortKind }
            .thenBy { it.stableSortKey }
    }
}
