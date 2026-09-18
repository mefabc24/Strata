package com.mefabc24.strata.render

import com.mefabc24.strata.world.PlacedObject

/**
 * Describes an object placement preview and its appearance.
 */
data class PlacementPreview(
    val placedObject: PlacedObject,
    val valid: Boolean,
    val style: PlacementPreviewStyle = PlacementPreviewStyle.DEFAULT
)