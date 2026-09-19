package com.mefabc24.strata.scene

import com.mefabc24.strata.camera.ViewportMode
import com.mefabc24.strata.camera.ZoomAnchor
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.input.WorldInputBinding

/**
 * Configures an isometric world view.
 */
class IsoViewSettings {

    var tileWidth: Float = 64f
    var tileHeight: Float = 32f

    var viewportMode: ViewportMode = ViewportMode.FIXED_HEIGHT
    var virtualHeight: Float = 720f

    var zoomMode: ZoomMode = ZoomMode.WORLD_BASED
    var zoomAnchor: ZoomAnchor = ZoomAnchor.CURSOR

    var cameraPadding: Float = 100f
    var zoomEdgeAllowance: Float = 0f
    var worldFill: Float = 0.85f

    var bindings: List<WorldInputBinding> = emptyList()

    /**
     * Overrides the automatically calculated terrain sprite height.
     *
     * Null enables automatic calculation.
     */
    var maxTerrainSpriteHeight: Float? = null
}