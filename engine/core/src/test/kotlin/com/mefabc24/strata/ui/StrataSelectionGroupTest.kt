package com.mefabc24.strata.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StrataSelectionGroupTest {

    private enum class Option {
        FIRST,
        SECOND,
        THIRD
    }

    @Test
    fun `required group selects its first option by default`() {
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        assertEquals(Option.FIRST, group.selected)
        assertTrue(group.selectionRequired)
    }

    @Test
    fun `explicit initial selection is retained`() {
        val group = StrataSelectionGroup(
            options = Option.entries,
            initialSelection = Option.SECOND
        )

        assertEquals(Option.SECOND, group.selected)
    }

    @Test
    fun `select changes one value and invokes callbacks once`() {
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        val changes = mutableListOf<Option?>()
        group.onSelectionChanged(changes::add)

        assertTrue(group.select(Option.SECOND))
        assertFalse(group.select(Option.SECOND))

        assertEquals(Option.SECOND, group.selected)
        assertEquals(listOf<Option?>(Option.SECOND), changes)
    }

    @Test
    fun `required group cannot be cleared`() {
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        assertFalse(group.clearSelection())
        assertEquals(Option.FIRST, group.selected)
    }

    @Test
    fun `optional group supports selection and deselection`() {
        val group = StrataSelectionGroup(
            options = Option.entries,
            selectionRequired = false
        )

        val changes = mutableListOf<Option?>()
        group.onSelectionChanged(changes::add)

        assertNull(group.selected)
        assertTrue(group.select(Option.THIRD))
        assertTrue(group.clearSelection())
        assertFalse(group.clearSelection())

        assertNull(group.selected)
        assertEquals(listOf(Option.THIRD, null), changes)
    }

    @Test
    fun `disposed callback is no longer invoked`() {
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        var changes = 0
        val subscription = group.onSelectionChanged {
            changes++
        }

        group.select(Option.SECOND)
        subscription.dispose()
        subscription.dispose()
        group.select(Option.THIRD)

        assertEquals(1, changes)
    }

    @Test
    fun `unknown and invalid options are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            StrataSelectionGroup(
                options = listOf(Option.FIRST, Option.FIRST)
            )
        }

        assertFailsWith<IllegalArgumentException> {
            StrataSelectionGroup(
                options = listOf(Option.FIRST),
                initialSelection = Option.SECOND
            )
        }

        val group = StrataSelectionGroup(
            options = listOf(Option.FIRST)
        )

        assertFailsWith<IllegalArgumentException> {
            group.select(Option.SECOND)
        }
    }

    @Test
    fun `required group rejects an empty option set`() {
        assertFailsWith<IllegalArgumentException> {
            StrataSelectionGroup<Option>(
                options = emptyList()
            )
        }

        val optional = StrataSelectionGroup<Option>(
            options = emptyList(),
            selectionRequired = false
        )

        assertNull(optional.selected)
    }

    @Test
    fun `null options are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            StrataSelectionGroup<Option?>(
                options = listOf(Option.FIRST, null),
                selectionRequired = false
            )
        }
    }

    @Test
    fun `only one control can attach to an option`() {
        val group = StrataSelectionGroup(
            options = Option.entries
        )

        group.attach(Option.FIRST)

        assertFailsWith<IllegalStateException> {
            group.attach(Option.FIRST)
        }

        group.detach(Option.FIRST)
        group.attach(Option.FIRST)
    }
}
