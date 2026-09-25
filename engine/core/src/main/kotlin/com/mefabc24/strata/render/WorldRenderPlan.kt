package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

/** One operation in the complete world rendering sequence. */
internal sealed interface WorldRenderItem

/** A normal world visual ordered through the shared isometric depth model. */
internal sealed interface WorldRenderPrimitive :
    WorldRenderItem,
    IsoSortable

/** One complete authored terrain sprite at its logical world position. */
internal data class TerrainCell(
    val x: Int,
    val y: Int,
    val elevation: Int
) : WorldRenderPrimitive {
    override val sortVolume = IsoSortVolume(
        minX = x,
        maxX = x + 1,
        minY = y,
        maxY = y + 1,
        minZ = elevation,
        maxZ = elevation
    )

    override val sortKind: Int = 0
    override val stableSortKey: String = ""
}

/** A placed object represented by its complete footprint and support level. */
internal data class WorldObjectPrimitive(
    val placedObject: PlacedObject,
    val supportElevation: Int
) : WorldRenderPrimitive {
    val occupiedTiles: Set<TilePosition> = placedObject.occupiedTiles()

    override val sortVolume = IsoSortVolume(
        minX = occupiedTiles.minOf(TilePosition::x),
        maxX = occupiedTiles.maxOf(TilePosition::x) + 1,
        minY = occupiedTiles.minOf(TilePosition::y),
        maxY = occupiedTiles.maxOf(TilePosition::y) + 1,
        minZ = supportElevation,
        maxZ = supportElevation + 1
    )

    override val sortKind: Int = 2
    override val stableSortKey: String =
        placedObject.placeable::class.qualifiedName.orEmpty()
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
                val elevation = world.getHeight(x, y) ?: continue
                world.getTile(x, y) ?: continue

                val cellIndex = primitives.size
                primitives += TerrainCell(x, y, elevation)
                terrainIndexByCell[y * world.width + x] = cellIndex
            }
        }

        val objectIndices = mutableListOf<Int>()

        for (placed in world.getObjects()) {
            val objectIndex = primitives.size
            val primitive = WorldObjectPrimitive(
                placedObject = placed,
                supportElevation = world.getHeight(placed.x, placed.y) ?: 0
            )

            primitives += primitive
            objectIndices += objectIndex

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

        val objectCandidates = buildList {
            for (first in objectIndices.indices) {
                for (second in first + 1 until objectIndices.size) {
                    add(
                        IsoRenderCandidate(
                            first = objectIndices[first],
                            second = objectIndices[second]
                        )
                    )
                }
            }
        }

        return IsoRenderOrder.backToFront(
            items = primitives,
            projection = projection,
            relationCandidates = objectCandidates,
            explicitDependencies = explicitDependencies,
            metrics = metrics
        )
    }

    /** Appends the placement preview after every normal world primitive. */
    fun withPreview(
        normalItems: List<WorldRenderPrimitive>,
        preview: PlacementPreview?
    ): List<WorldRenderItem> {
        if (preview == null) return normalItems

        return normalItems + PreviewRenderItem(preview)
    }
}
