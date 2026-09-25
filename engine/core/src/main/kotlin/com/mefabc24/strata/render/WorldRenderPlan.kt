package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

/** One terrain or object operation in back-to-front world rendering order. */
internal sealed interface WorldRenderItem {

    data class TerrainFill(
        val x: Int,
        val y: Int,
        val elevation: Int,
        val part: TerrainFillPart
    ) : WorldRenderItem {
        val effectiveElevation: Int
            get() = elevation - part.levelBelowSurface - 1
    }

    data class TerrainSurface(
        val x: Int,
        val y: Int,
        val elevation: Int
    ) : WorldRenderItem {
        val effectiveElevation: Int
            get() = elevation
    }

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
        val objectItems = world.getObjects()
            .map(WorldRenderItem::WorldObject)

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
        val dependencies = StableDependencyOrder(
            items = nodes,
            comparator = nodeComparator
        )

        fun addEdge(source: Node, target: Node) {
            dependencies.add(source.index, target.index)
        }

        // Each terrain column draws its deepest fill first and its surface last.
        for (index in 0 until terrainNodes.lastIndex) {
            val current = terrainNodes[index]
            val next = terrainNodes[index + 1]

            if (current.position == next.position) {
                addEdge(current, next)
            }
        }

        // Only definite footprint relationships become hard dependencies.
        for (firstIndex in objectNodes.indices) {
            for (secondIndex in firstIndex + 1 until objectNodes.size) {
                val first = objectNodes[firstIndex]
                val second = objectNodes[secondIndex]
                val firstBehind = first.isBehind(second)
                val secondBehind = second.isBehind(first)

                when {
                    firstBehind && !secondBehind -> addEdge(first, second)
                    secondBehind && !firstBehind -> addEdge(second, first)
                }
            }
        }

        for (terrain in terrainNodes) {
            for (objectNode in objectNodes) {
                if (terrain.isSupportingSurfaceFor(objectNode)) {
                    addEdge(terrain, objectNode)
                } else if (terrain.definitelyOccludes(objectNode)) {
                    addEdge(objectNode, terrain)
                }
            }
        }

        return dependencies.resolve().map(Node::item)
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

        val effectiveElevation = when (item) {
            is WorldRenderItem.TerrainFill -> item.effectiveElevation
            is WorldRenderItem.TerrainSurface -> item.effectiveElevation
            is WorldRenderItem.WorldObject -> {
                elevationFor(item.placedObject.x, item.placedObject.y) ?: 0
            }
            is WorldRenderItem.Preview -> error("Preview is not a normal world item.")
        }

        val groundY = projection.surfaceAnchor(
            x = maxX,
            y = maxY,
            elevation = effectiveElevation
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

        fun isSupportingSurfaceFor(objectNode: Node): Boolean {
            check(
                item is WorldRenderItem.TerrainFill ||
                        item is WorldRenderItem.TerrainSurface
            )
            check(objectNode.item is WorldRenderItem.WorldObject)

            return item is WorldRenderItem.TerrainSurface &&
                    position in objectNode.occupiedTiles
        }

        fun definitelyOccludes(objectNode: Node): Boolean {
            check(
                item is WorldRenderItem.TerrainFill ||
                        item is WorldRenderItem.TerrainSurface
            )
            check(objectNode.item is WorldRenderItem.WorldObject)

            if (effectiveElevation <= objectNode.effectiveElevation) {
                return false
            }

            val terrainBehind = isBehind(objectNode)
            val objectBehind = objectNode.isBehind(this)

            return objectBehind && !terrainBehind
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
