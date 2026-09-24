package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject
import java.util.PriorityQueue

/** Builds a deterministic back-to-front order from object footprints. */
internal object IsoObjectOrdering {

    fun backToFront(
        objects: Collection<PlacedObject>,
        projection: IsoProjection,
        elevationFor: (PlacedObject) -> Int
    ): List<PlacedObject> {
        if (objects.size < 2) return objects.toList()

        val nodes = objects.map { placed ->
            Node(
                placed = placed,
                projection = projection,
                elevation = elevationFor(placed)
            )
        }
        val edges = List(nodes.size) { mutableSetOf<Int>() }
        val incoming = IntArray(nodes.size)

        for (firstIndex in nodes.indices) {
            for (secondIndex in firstIndex + 1 until nodes.size) {
                val first = nodes[firstIndex]
                val second = nodes[secondIndex]
                val firstBehind = first.isBehind(second)
                val secondBehind = second.isBehind(first)

                when {
                    firstBehind && !secondBehind -> {
                        edges[firstIndex] += secondIndex
                        incoming[secondIndex]++
                    }

                    secondBehind && !firstBehind -> {
                        edges[secondIndex] += firstIndex
                        incoming[firstIndex]++
                    }
                }
            }
        }

        val available = PriorityQueue(nodeComparator)

        for (index in nodes.indices) {
            if (incoming[index] == 0) {
                available += IndexedNode(index, nodes[index])
            }
        }

        val emitted = BooleanArray(nodes.size)
        val result = ArrayList<PlacedObject>(nodes.size)

        while (result.size < nodes.size) {
            if (available.isEmpty()) {
                val fallback = nodes.indices
                    .asSequence()
                    .filterNot { emitted[it] }
                    .map { IndexedNode(it, nodes[it]) }
                    .minWith(nodeComparator)

                available += fallback
            }

            val current = available.remove()

            if (emitted[current.index]) continue

            emitted[current.index] = true
            result += current.node.placed

            for (target in edges[current.index]) {
                if (emitted[target]) continue

                incoming[target]--

                if (incoming[target] == 0) {
                    available += IndexedNode(target, nodes[target])
                }
            }
        }

        return result
    }

    private class Node(
        val placed: PlacedObject,
        projection: IsoProjection,
        elevation: Int
    ) {
        private val occupied = placed.occupiedTiles()

        val minX = occupied.minOf { it.x }
        val maxX = occupied.maxOf { it.x }
        val minY = occupied.minOf { it.y }
        val maxY = occupied.maxOf { it.y }

        val groundY = projection.surfaceAnchor(
            x = maxX,
            y = maxY,
            elevation = elevation
        ).y

        val typeName = placed.placeable::class.qualifiedName.orEmpty()

        fun isBehind(other: Node): Boolean {
            return maxX < other.minX || maxY < other.minY
        }
    }

    private data class IndexedNode(
        val index: Int,
        val node: Node
    )

    private val nodeComparator =
        compareByDescending<IndexedNode> { it.node.groundY }
            .thenBy { it.node.minY }
            .thenBy { it.node.minX }
            .thenBy { it.node.maxY }
            .thenBy { it.node.maxX }
            .thenBy { it.node.typeName }
}
