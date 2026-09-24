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

        return merge(terrainNodes, objectNodes)
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

    private fun merge(
        terrainNodes: List<Node>,
        objectNodes: List<Node>
    ): List<WorldRenderItem> {
        var terrainIndex = 0
        var objectIndex = 0
        val result = ArrayList<WorldRenderItem>(
            terrainNodes.size + objectNodes.size
        )

        while (
            terrainIndex < terrainNodes.size &&
            objectIndex < objectNodes.size
        ) {
            val terrain = terrainNodes[terrainIndex]
            val objectNode = objectNodes[objectIndex]
            val terrainBehind = terrain.isBehind(objectNode)
            val objectBehind = objectNode.isBehind(terrain)

            val emitTerrain = when {
                terrain.position in objectNode.occupiedTiles -> true
                terrainBehind && !objectBehind -> true
                objectBehind && !terrainBehind -> false
                else -> nodeComparator.compare(terrain, objectNode) <= 0
            }

            if (emitTerrain) {
                result += terrain.item
                terrainIndex++
            } else {
                result += objectNode.item
                objectIndex++
            }
        }

        while (terrainIndex < terrainNodes.size) {
            result += terrainNodes[terrainIndex++].item
        }

        while (objectIndex < objectNodes.size) {
            result += objectNodes[objectIndex++].item
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

        private val elevation = when (item) {
            is WorldRenderItem.TerrainFill -> {
                item.elevation - item.part.levelBelowSurface
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
