package com.mefabc24.strata.render.debug

import com.mefabc24.strata.debug.DebugInspectSettings
import com.mefabc24.strata.debug.inspector.DebugInspection

internal enum class DebugInspectionVisual {
    TILE,
    OBJECT_FOOTPRINT,
    OBJECT_ORIGIN,
    OBJECT_SPRITE_BOUNDS,
    ENTITY_TILE,
    ENTITY_POSITION,
    ENTITY_PATH,
    ENTITY_DIRECTION,
    ENTITY_SPRITE_BOUNDS
}

internal fun inspectionVisuals(
    inspection: DebugInspection?,
    active: Boolean,
    settings: DebugInspectSettings
): Set<DebugInspectionVisual> {
    if (!active || inspection == null) return emptySet()
    return when (inspection) {
        is DebugInspection.TileTarget -> buildSet {
            if (settings.showTile) add(DebugInspectionVisual.TILE)
        }
        is DebugInspection.ObjectTarget -> buildSet {
            if (settings.showObjectFootprint) add(DebugInspectionVisual.OBJECT_FOOTPRINT)
            if (settings.showObjectOrigin) add(DebugInspectionVisual.OBJECT_ORIGIN)
            if (settings.showObjectSpriteBounds) add(DebugInspectionVisual.OBJECT_SPRITE_BOUNDS)
        }
        is DebugInspection.EntityTarget -> buildSet {
            if (settings.showEntityTile) add(DebugInspectionVisual.ENTITY_TILE)
            if (settings.showEntityPosition) add(DebugInspectionVisual.ENTITY_POSITION)
            if (settings.showEntityPath) add(DebugInspectionVisual.ENTITY_PATH)
            if (settings.showEntityDirection) add(DebugInspectionVisual.ENTITY_DIRECTION)
            if (settings.showEntitySpriteBounds) add(DebugInspectionVisual.ENTITY_SPRITE_BOUNDS)
        }
    }
}
