package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject

/** Adapts object-only consumers to the shared isometric render ordering. */
internal object IsoObjectOrdering {

    fun backToFront(
        objects: Collection<PlacedObject>,
        projection: IsoProjection,
        elevationFor: (PlacedObject) -> Int
    ): List<PlacedObject> {
        val primitives = objects.map { placed ->
            WorldObjectPrimitive(
                placedObject = placed,
                supportElevation = elevationFor(placed)
            )
        }
        val candidates = buildList {
            for (first in primitives.indices) {
                for (second in first + 1 until primitives.size) {
                    add(IsoRenderCandidate(first, second))
                }
            }
        }

        return IsoRenderOrder.backToFront(
            items = primitives,
            projection = projection,
            relationCandidates = candidates
        )
            .map(WorldObjectPrimitive::placedObject)
    }
}
