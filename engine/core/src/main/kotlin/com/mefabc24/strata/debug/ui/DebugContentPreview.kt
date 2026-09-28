package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion

enum class DebugContentKind { TERRAIN, OBJECT, ENTITY }

data class DebugContentPreview(
    val key: Any,
    val kind: DebugContentKind,
    val name: String,
    val texture: TextureRegion
)

/** Small state holder shared by hover callbacks and panel lifecycle changes. */
class DebugContentPreviewState {
    var current: DebugContentPreview? = null
        private set

    fun show(preview: DebugContentPreview) {
        current = preview
    }

    fun hide(key: Any? = null) {
        if (key == null || current?.key === key) current = null
    }
}
