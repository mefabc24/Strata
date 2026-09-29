package com.mefabc24.strata.render.preview

import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.WorldEntity

/** Visual-only entity preview rendered independently of world state. */
class EntityPreview internal constructor(
    val position: EntityPosition,
    internal val visual: EntityPreviewVisual,
    val valid: Boolean,
    val style: PlacementPreviewStyle
)

internal sealed interface EntityPreviewVisual {
    data class Representative(val value: EntityVisual) : EntityPreviewVisual
    data class Existing(val entity: WorldEntity) : EntityPreviewVisual
}
