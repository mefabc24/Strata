package com.mefabc24.strata.render.order

import kotlin.test.Test
import kotlin.test.assertEquals

class StableDependencyOrderTest {

    @Test
    fun `dependency cycles fall back without dropping items`() {
        val order = StableDependencyOrder(
            items = listOf("A", "B", "C", "D"),
            comparator = naturalOrder()
        ).apply {
            add(before = 0, after = 1)
            add(before = 1, after = 2)
            add(before = 2, after = 0)
            add(before = 3, after = 2)
        }

        val first = order.resolve()
        val second = order.resolve()

        assertEquals(listOf("D", "A", "B", "C"), first)
        assertEquals(first, second)
        assertEquals(listOf("A", "B", "C", "D"), first.sorted())
    }
}
