package com.mefabc24.strata.render.debug

import com.mefabc24.strata.render.RenderDebugSnapshot
import com.mefabc24.strata.render.RenderItemDebugSnapshot

internal enum class CullingDebugItemKind { OBJECT, ENTITY }

internal data class CullingDebugClassification(
    val kind: CullingDebugItemKind,
    val drawn: Boolean
)

internal data class CullingDebugCounts(
    val objectsDrawn: Int,
    val objectsCulled: Int,
    val entitiesDrawn: Int,
    val entitiesCulled: Int
)

/** Classifies renderer-owned culling data without changing its decision. */
internal fun classifyCullingItem(
    item: RenderItemDebugSnapshot
): CullingDebugClassification? {
    if (item.bounds == null) return null
    return when {
        item.placedObject != null -> CullingDebugClassification(
            CullingDebugItemKind.OBJECT,
            item.drawn
        )
        item.entity != null -> CullingDebugClassification(
            CullingDebugItemKind.ENTITY,
            item.drawn
        )
        else -> null
    }
}

internal fun cullingDebugCounts(
    snapshot: RenderDebugSnapshot?
): CullingDebugCounts {
    var objectsDrawn = 0
    var objectsCulled = 0
    var entitiesDrawn = 0
    var entitiesCulled = 0
    snapshot?.items.orEmpty().forEach { item ->
        val classification = classifyCullingItem(item) ?: return@forEach
        when (classification.kind) {
            CullingDebugItemKind.OBJECT -> {
                if (classification.drawn) objectsDrawn++ else objectsCulled++
            }
            CullingDebugItemKind.ENTITY -> {
                if (classification.drawn) entitiesDrawn++ else entitiesCulled++
            }
        }
    }
    return CullingDebugCounts(
        objectsDrawn,
        objectsCulled,
        entitiesDrawn,
        entitiesCulled
    )
}
