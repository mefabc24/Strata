package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.ui.StrataSelectionGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DebugToolSelectionListTest {
    private val texture = TextureRegion()
    private val items = listOf(
        DebugToolSelectionItem("house", "Town House", texture),
        DebugToolSelectionItem("tree", "Oak Tree", texture),
        DebugToolSelectionItem("villa", "Villa", texture)
    )

    @Test
    fun `search filters display names without changing registry order`() {
        assertEquals(
            listOf("house"),
            filterDebugToolSelectionItems(items, "  HOUSE ").map { it.value }
        )
        assertEquals(
            listOf("tree"),
            filterDebugToolSelectionItems(items, "oak").map { it.value }
        )
        assertEquals(items, filterDebugToolSelectionItems(items, " "))
        assertEquals(emptyList(), filterDebugToolSelectionItems(items, "missing"))
    }

    @Test
    fun `filtering does not clear a selection excluded from the results`() {
        val selection = StrataSelectionGroup(items.map { it.value }, "villa")

        filterDebugToolSelectionItems(items, "tree")

        assertEquals("villa", selection.selected)
    }

    @Test
    fun `selection list height shrinks and caps independently`() {
        assertEquals(42f, debugToolSelectionListHeight(0))
        assertEquals(56f, debugToolSelectionListHeight(1))
        assertEquals(227f, debugToolSelectionListHeight(4))
        assertEquals(228f, debugToolSelectionListHeight(40))
        assertFailsWith<IllegalArgumentException> { debugToolSelectionListHeight(-1) }
    }
}
