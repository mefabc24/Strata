package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class DebugContentPreviewTest {
    @Test
    fun `hover preview only hides the entry that is still active`() {
        val firstKey = Any()
        val secondKey = Any()
        val texture = TextureRegion()
        val state = DebugContentPreviewState()
        state.show(DebugContentPreview(firstKey, DebugContentKind.OBJECT, "House", texture))
        state.show(DebugContentPreview(secondKey, DebugContentKind.ENTITY, "Wolf", texture))
        state.hide(firstKey)
        assertSame(secondKey, state.current?.key)
        assertEquals("Wolf", state.current?.name)
        state.hide(secondKey)
        assertNull(state.current)
    }
}
