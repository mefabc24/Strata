package com.mefabc24.strata.render

import java.util.PriorityQueue

/** Resolves definite dependencies with a deterministic fallback for cycles. */
internal class StableDependencyOrder<T>(
    private val items: List<T>,
    comparator: Comparator<T>
) {
    private val edges = List(items.size) { mutableSetOf<Int>() }

    private val indexedComparator = Comparator<Int> { first, second ->
        comparator.compare(items[first], items[second])
            .takeIf { it != 0 }
            ?: first.compareTo(second)
    }

    fun add(
        before: Int,
        after: Int
    ) {
        require(before in items.indices && after in items.indices) {
            "Dependency indices must refer to existing items."
        }

        if (before != after) {
            edges[before] += after
        }
    }

    fun resolve(): List<T> {
        val incoming = IntArray(items.size)

        for (targets in edges) {
            for (target in targets) {
                incoming[target]++
            }
        }

        val available = PriorityQueue(indexedComparator)

        for (index in items.indices) {
            if (incoming[index] == 0) {
                available += index
            }
        }

        val emitted = BooleanArray(items.size)
        val result = ArrayList<T>(items.size)

        while (result.size < items.size) {
            if (available.isEmpty()) {
                val fallback = items.indices
                    .asSequence()
                    .filterNot { emitted[it] }
                    .minWith(indexedComparator)

                available += fallback
            }

            val current = available.remove()

            if (emitted[current]) continue

            emitted[current] = true
            result += items[current]

            for (target in edges[current]) {
                if (emitted[target]) continue

                incoming[target]--

                if (incoming[target] == 0) {
                    available += target
                }
            }
        }

        return result
    }
}
