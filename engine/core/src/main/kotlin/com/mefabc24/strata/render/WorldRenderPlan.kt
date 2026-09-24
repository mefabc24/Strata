package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import java.util.PriorityQueue

/** One terrain or object operation in back-to-front world rendering order. */
internal sealed interface WorldRenderItem {

    data class TerrainFill(
        val x: Int,
        val y: Int,
        val elevation: Int,
        val part: TerrainFillPart
    ) : WorldRenderItem

    data class TerrainSurface(
        val x: Int,
        val y: Int,
        val elevation: Int
    ) : WorldRenderItem

    data class WorldObject(
        val placedObject: PlacedObject
    ) : WorldRenderItem

    data class Preview(
        val preview: PlacementPreview
    ) : WorldRenderItem
}

/** Builds deterministic spatial ordering for normal world visuals. */
internal object WorldRenderPlan {

    fun create(
        world: World,
        projection: IsoProjection
    ): List<WorldRenderItem> {
        val terrainItems = terrainItems(world)
        val objectItems = IsoObjectOrdering.backToFront(
            objects = world.getObjects(),
            projection = projection,
            elevationFor = { placed ->
                world.getHeight(placed.x, placed.y) ?: 0
            }
        ).map(WorldRenderItem::WorldObject)

        if (objectItems.isEmpty()) return terrainItems

        val terrainNodes = terrainItems.mapIndexed { index, item ->
            Node(index, item, projection, world::getHeight)
        }
        val objectNodes = objectItems.mapIndexed { index, item ->
            Node(
                index = terrainItems.size + index,
                item = item,
                projection = projection,
                elevationFor = world::getHeight
            )
        }

        return order(terrainNodes, objectNodes)
    }

    /** Appends the placement preview after every normal world visual. */
    fun withPreview(
        normalItems: List<WorldRenderItem>,
        preview: PlacementPreview?
    ): List<WorldRenderItem> {
        if (preview == null) return normalItems

        return normalItems + WorldRenderItem.Preview(preview)
    }

    private fun terrainItems(world: World): List<WorldRenderItem> {
        return buildList {
            for (depth in 0 until world.width + world.height - 1) {
                val minX = maxOf(0, depth - world.height + 1)
                val maxX = minOf(world.width - 1, depth)

                for (x in minX..maxX) {
                    val y = depth - x
                    val elevation = world.getHeight(x, y) ?: continue

                    for (part in TerrainFillPlan.create(world, x, y)) {
                        add(
                            WorldRenderItem.TerrainFill(
                                x = x,
                                y = y,
                                elevation = elevation,
                                part = part
                            )
                        )
                    }

                    add(
                        WorldRenderItem.TerrainSurface(
                            x = x,
                            y = y,
                            elevation = elevation
                        )
                    )
                }
            }
        }
    }

    private fun order(
        terrainNodes: List<Node>,
        objectNodes: List<Node>
    ): List<WorldRenderItem> {
        val nodes = terrainNodes + objectNodes
        val edges = List(nodes.size) { mutableSetOf<Int>() }
        val incoming = IntArray(nodes.size)

        fun addEdge(source: Node, target: Node) {
            if (source.index == target.index) return

            if (edges[source.index].add(target.index)) {
                incoming[target.index]++
            }
        }

        // Each terrain column draws its deepest fill first and its surface last.
        for (index in 0 until terrainNodes.lastIndex) {
            val current = terrainNodes[index]
            val next = terrainNodes[index + 1]

            if (current.position == next.position) {
                addEdge(current, next)
            }
        }

        // IsoObjectOrdering already establishes footprint-aware object order.
        for (index in 0 until objectNodes.lastIndex) {
            addEdge(objectNodes[index], objectNodes[index + 1])
        }

        for (terrain in terrainNodes) {
            for (objectNode in objectNodes) {
                if (terrain.mustRenderBefore(objectNode)) {
                    addEdge(terrain, objectNode)
                } else {
                    addEdge(objectNode, terrain)
                }
            }
        }

        val available = PriorityQueue(nodeComparator)

        for (node in nodes) {
            if (incoming[node.index] == 0) {
                available += node
            }
        }

        val result = ArrayList<WorldRenderItem>(nodes.size)

        while (available.isNotEmpty()) {
            val current = available.remove()
            result += current.item

            for (target in edges[current.index]) {
                incoming[target]--

                if (incoming[target] == 0) {
                    available += nodes[target]
                }
            }
        }

        check(result.size == nodes.size) {
            "World render dependencies must not contain a cycle."
        }

        return result
    }

    private class Node(
        val index: Int,
        val item: WorldRenderItem,
        projection: IsoProjection,
        elevationFor: (Int, Int) -> Int?
    ) {
        val occupiedTiles: Set<TilePosition> = when (item) {
            is WorldRenderItem.WorldObject -> item.placedObject.occupiedTiles()
            is WorldRenderItem.TerrainFill -> setOf(TilePosition(item.x, item.y))
            is WorldRenderItem.TerrainSurface -> setOf(TilePosition(item.x, item.y))
            is WorldRenderItem.Preview -> error("Preview is not a normal world item.")
        }

        val minX = occupiedTiles.minOf(TilePosition::x)
        val maxX = occupiedTiles.maxOf(TilePosition::x)
        val minY = occupiedTiles.minOf(TilePosition::y)
        val maxY = occupiedTiles.maxOf(TilePosition::y)

        val position = when (item) {
            is WorldRenderItem.TerrainFill -> TilePosition(item.x, item.y)
            is WorldRenderItem.TerrainSurface -> TilePosition(item.x, item.y)
            is WorldRenderItem.WorldObject -> TilePosition(
                item.placedObject.x,
                item.placedObject.y
            )
            is WorldRenderItem.Preview -> error("Preview is not a normal world item.")
        }

        val elevation = when (item) {
            is WorldRenderItem.TerrainFill -> {
                item.elevation - item.part.levelBelowSurface - 1
            }
            is WorldRenderItem.TerrainSurface -> item.elevation
            is WorldRenderItem.WorldObject -> {
                elevationFor(item.placedObject.x, item.placedObject.y) ?: 0
            }
            is WorldRenderItem.Preview -> error("Preview is not a normal world item.")
        }

        val groundY = projection.surfaceAnchor(
            x = maxX,
            y = maxY,
            elevation = elevation
        ).y

        val kindOrder = when (item) {
            is WorldRenderItem.TerrainFill -> 0
            is WorldRenderItem.TerrainSurface -> 1
            is WorldRenderItem.WorldObject -> 2
            is WorldRenderItem.Preview -> error("Preview is not a normal world item.")
        }

        val stableName = when (item) {
            is WorldRenderItem.WorldObject -> {
                item.placedObject.placeable::class.qualifiedName.orEmpty()
            }
            else -> ""
        }

        fun isBehind(other: Node): Boolean {
            return maxX < other.minX || maxY < other.minY
        }

        fun mustRenderBefore(objectNode: Node): Boolean {
            check(item !is WorldRenderItem.WorldObject)
            check(objectNode.item is WorldRenderItem.WorldObject)

            if (position in objectNode.occupiedTiles) return true

            // Terrain at or below the object's supporting level cannot cover
            // an object that stands on that level.
            if (columnElevation <= objectNode.elevation) return true

            val terrainBehind = isBehind(objectNode)
            val objectBehind = objectNode.isBehind(this)

            // Only a strictly higher terrain column that is definitely in
            // front of the object is allowed to occlude it.
            return !(objectBehind && !terrainBehind)
        }

        private val columnElevation = when (item) {
            is WorldRenderItem.TerrainFill -> item.elevation
            is WorldRenderItem.TerrainSurface -> item.elevation
            is WorldRenderItem.WorldObject -> elevation
            is WorldRenderItem.Preview -> error("Preview is not a normal world item.")
        }
    }

    private val nodeComparator =
        compareByDescending<Node> { it.groundY }
            .thenBy { it.minY }
            .thenBy { it.minX }
            .thenBy { it.maxY }
            .thenBy { it.maxX }
            .thenBy { it.kindOrder }
            .thenBy { it.stableName }
            .thenBy { it.index }
}
