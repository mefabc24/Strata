package com.mefabc24.strata.render.order

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

/** One operation in the complete world rendering sequence. */
internal sealed interface WorldRenderItem

/** A normal world visual ordered through the shared isometric depth model. */
internal sealed interface WorldRenderPrimitive :
    WorldRenderItem,
    IsoSortable

/** One complete authored terrain sprite at its logical world position. */
internal data class TerrainCell(
    val x: Int,
    val y: Int
) : WorldRenderPrimitive {
    override val sortVolume = IsoSortVolume(
        minX = x,
        maxX = x + 1,
        minY = y,
        maxY = y + 1
    )

    override val sortKind: Int = 0
    override val stableSortKey: String = ""
}

/** A placed object represented by its complete footprint and support level. */
internal data class WorldObjectPrimitive(
    val placedObject: PlacedObject
) : WorldRenderPrimitive {
    val occupiedTiles: Set<TilePosition> = placedObject.occupiedTiles()

    override val sortVolume = IsoSortVolume(
        minX = occupiedTiles.minOf(TilePosition::x),
        maxX = occupiedTiles.maxOf(TilePosition::x) + 1,
        minY = occupiedTiles.minOf(TilePosition::y),
        maxY = occupiedTiles.maxOf(TilePosition::y) + 1
    )

    override val sortKind: Int = 2
    override val stableSortKey: String =
        placedObject.placeable::class.qualifiedName.orEmpty()
}

/** A point-like entity at its continuous ground position. */
internal data class WorldEntityPrimitive(
    val worldEntity: WorldEntity
) : WorldRenderPrimitive {
    override val sortVolume: IsoSortVolume
        get() = worldEntity.position.let { position ->
            IsoSortVolume(
                minX = position.x,
                maxX = position.x,
                minY = position.y,
                maxY = position.y
            )
        }

    override val sortKind: Int = 1
    override val stableSortKey: String =
        worldEntity.entity::class.qualifiedName.orEmpty()
}

/** Placement previews intentionally remain outside normal depth ordering. */
internal data class PreviewRenderItem(
    val preview: PlacementPreview
) : WorldRenderItem

/** Builds a grid-aware, deterministic world rendering sequence. */
internal object WorldRenderPlan {

    fun create(
        world: World,
        projection: IsoProjection,
        metrics: IsoRenderOrderMetrics? = null
    ): List<WorldRenderPrimitive> {
        val primitives = mutableListOf<WorldRenderPrimitive>()
        val terrainIndexByCell = IntArray(world.width * world.height) { -1 }
        val explicitDependencies = mutableListOf<IsoRenderDependency>()

        for (depth in 0 until world.width + world.height - 1) {
            val minX = maxOf(0, depth - world.height + 1)
            val maxX = minOf(world.width - 1, depth)

            for (x in minX..maxX) {
                val y = depth - x
                world.getTile(x, y) ?: continue

                val cellIndex = primitives.size
                primitives += TerrainCell(x, y)
                terrainIndexByCell[y * world.width + x] = cellIndex
            }
        }

        val depthSortedIndices = mutableListOf<Int>()

        for (placed in world.getObjects()) {
            val objectIndex = primitives.size
            val primitive = WorldObjectPrimitive(placed)

            primitives += primitive
            depthSortedIndices += objectIndex

            for (position in primitive.occupiedTiles) {
                val cellIndex = position.y * world.width + position.x
                val terrainIndex = terrainIndexByCell[cellIndex]

                if (terrainIndex >= 0) {
                    explicitDependencies += IsoRenderDependency(
                        before = terrainIndex,
                        after = objectIndex
                    )
                }
            }
        }

        for (entity in world.getEntities()) {
            val entityIndex = primitives.size
            primitives += WorldEntityPrimitive(entity)
            depthSortedIndices += entityIndex

            val tile = entity.currentTile
            if (tile.x in 0 until world.width && tile.y in 0 until world.height) {
                val terrainIndex = terrainIndexByCell[
                    tile.y * world.width + tile.x
                ]

                if (terrainIndex >= 0) {
                    explicitDependencies += IsoRenderDependency(
                        before = terrainIndex,
                        after = entityIndex
                    )
                }
            }
        }

        val depthCandidates = buildList {
            for (first in depthSortedIndices.indices) {
                for (second in first + 1 until depthSortedIndices.size) {
                    add(
                        IsoRenderCandidate(
                            first = depthSortedIndices[first],
                            second = depthSortedIndices[second]
                        )
                    )
                }
            }
        }

        return IsoRenderOrder.backToFront(
            items = primitives,
            projection = projection,
            relationCandidates = depthCandidates,
            explicitDependencies = explicitDependencies,
            metrics = metrics
        )
    }

    /** Appends placement previews after every normal world primitive. */
    fun withPreviews(
        normalItems: List<WorldRenderPrimitive>,
        previews: List<PlacementPreview>
    ): List<WorldRenderItem> {
        if (previews.isEmpty()) return normalItems

        return normalItems + previews.map(::PreviewRenderItem)
    }
}
