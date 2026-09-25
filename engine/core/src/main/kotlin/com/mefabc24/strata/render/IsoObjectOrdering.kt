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

        return IsoRenderOrder.backToFront(primitives, projection)
            .map(WorldObjectPrimitive::placedObject)
    }
}
