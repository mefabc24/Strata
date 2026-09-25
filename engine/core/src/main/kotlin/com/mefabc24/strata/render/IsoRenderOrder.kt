package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection

/** An explicit ordering rule between two primitive indices. */
internal data class IsoRenderDependency(
    val before: Int,
    val after: Int
)

/** A selected primitive pair whose spatial relationship should be checked. */
internal data class IsoRenderCandidate(
    val first: Int,
    val second: Int
)

/** Structural diagnostics for render-plan complexity tests. */
internal class IsoRenderOrderMetrics {
    var relationChecks: Int = 0
        internal set
}

/** Orders primitives by depth plus explicitly selected spatial candidates. */
internal object IsoRenderOrder {

    fun <T : IsoSortable> backToFront(
        items: List<T>,
        projection: IsoProjection,
        relationCandidates: List<IsoRenderCandidate> = emptyList(),
        explicitDependencies: List<IsoRenderDependency> = emptyList(),
        metrics: IsoRenderOrderMetrics? = null
    ): List<T> {
        metrics?.relationChecks = 0

        val dependencies = StableDependencyOrder(
            items = items,
            comparator = comparator(projection)
        )

        for ((firstIndex, secondIndex) in relationCandidates) {
            require(
                firstIndex in items.indices && secondIndex in items.indices
            ) {
                "Render candidates must refer to existing items."
            }

            metrics?.let { it.relationChecks++ }

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
        }.thenBy { it.sortVolume.minY }
            .thenBy { it.sortVolume.minX }
            .thenBy { it.sortVolume.maxY }
            .thenBy { it.sortVolume.maxX }
            .thenBy { it.sortKind }
            .thenBy { it.stableSortKey }
    }
}
