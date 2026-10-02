package com.mefabc24.strata.debug.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugSettingsSearchTest {
    @Test
    fun `search finds controls inside collapsed categories and reveals their category`() {
        val grid = DebugSearchCategory("Grid", setOf("Line width", "Hover fill"), expanded = false)
        val entities = DebugSearchCategory(
            "Entities",
            setOf("Movement trail", "Speed labels"),
            expanded = false
        )
        val search = DebugSettingsSearch(listOf(grid, entities))

        val result = search.update("speed labels")

        assertEquals(setOf(1), result.matchingIndices)
        assertTrue(result.hasMatches)
        assertFalse(grid.expanded)
        assertTrue(entities.expanded)
    }

    @Test
    fun `clearing search restores category expansion state`() {
        val grid = DebugSearchCategory("Grid", setOf("Line width"), expanded = true)
        val entities = DebugSearchCategory("Entities", setOf("Movement trail"), expanded = false)
        val search = DebugSettingsSearch(listOf(grid, entities))

        search.update("movement")
        assertTrue(entities.expanded)

        val cleared = search.update("")

        assertEquals(setOf(0, 1), cleared.matchingIndices)
        assertTrue(grid.expanded)
        assertFalse(entities.expanded)
    }

    @Test
    fun `empty search result is reported for panel empty state`() {
        val search = DebugSettingsSearch(
            listOf(DebugSearchCategory("Camera", setOf("Clamp bounds")))
        )

        val result = search.update("movement speed")

        assertFalse(result.hasMatches)
        assertTrue(result.matchingIndices.isEmpty())
    }

    @Test
    fun `expand and collapse all become the restored state during search`() {
        val categories = listOf(
            DebugSearchCategory("Grid", setOf("Line width"), expanded = false),
            DebugSearchCategory("Camera", setOf("Clamp bounds"), expanded = true)
        )
        val search = DebugSettingsSearch(categories)

        search.update("line")
        search.setAllExpanded(false)
        search.update("")
        assertTrue(categories.none { it.expanded })

        search.setAllExpanded(true)
        assertTrue(categories.all { it.expanded })
    }
}
