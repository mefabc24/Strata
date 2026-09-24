package com.mefabc24.strata.render

import com.mefabc24.strata.world.PlacedObject

/** One object-related draw item in final rendering order. */
internal sealed interface ObjectRenderItem {
    val placedObject: PlacedObject

    data class WorldObject(
        override val placedObject: PlacedObject
    ) : ObjectRenderItem

    data class Preview(
        val preview: PlacementPreview
    ) : ObjectRenderItem {
        override val placedObject: PlacedObject
            get() = preview.placedObject
    }
}

/** Appends the placement preview after every normal world object. */
internal object ObjectRenderPlan {
    fun create(
        orderedObjects: List<PlacedObject>,
        preview: PlacementPreview?
    ): List<ObjectRenderItem> {
        return buildList(orderedObjects.size + if (preview == null) 0 else 1) {
            orderedObjects.forEach { placed ->
                add(ObjectRenderItem.WorldObject(placed))
            }

            if (preview != null) {
                add(ObjectRenderItem.Preview(preview))
            }
        }
    }
}
