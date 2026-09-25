package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

/** One operation in the complete world rendering sequence. */
internal sealed interface WorldRenderItem

/** A normal world visual that participates in shared isometric ordering. */
internal sealed interface WorldRenderPrimitive :
    WorldRenderItem,
    IsoSortable

/** The logical diamond-shaped top face of one terrain cell. */
internal data class TerrainTop(
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

    override val sortKind: Int = 1
    override val stableSortKey: String = ""
}

/** One exposed terrain face spanning exactly one elevation interval. */
internal data class TerrainSide(
    val x: Int,
    val y: Int,
    val surfaceElevation: Int,
    val levelBelowSurface: Int,
    val face: TerrainFace
) : WorldRenderPrimitive {
    val bottomElevation: Int
        get() = surfaceElevation - levelBelowSurface - 1

    override val sortVolume: IsoSortVolume
        get() = when (face) {
            TerrainFace.LEFT -> IsoSortVolume(
                minX = x,
                maxX = x + 1,
                minY = y + 1,
                maxY = y + 1,
                minZ = bottomElevation,
                maxZ = bottomElevation + 1
            )

            TerrainFace.RIGHT -> IsoSortVolume(
                minX = x + 1,
                maxX = x + 1,
                minY = y,
                maxY = y + 1,
                minZ = bottomElevation,
                maxZ = bottomElevation + 1
            )
        }

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

/** Builds the atomic, deterministic world rendering sequence. */
internal object WorldRenderPlan {

    fun create(
        world: World,
        projection: IsoProjection,
        hasFillFor: (Tile) -> Boolean = { true }
    ): List<WorldRenderPrimitive> {
        val primitives = mutableListOf<WorldRenderPrimitive>()
        val topIndexByPosition = mutableMapOf<TilePosition, Int>()
        val explicitDependencies = mutableListOf<IsoRenderDependency>()

        for (depth in 0 until world.width + world.height - 1) {
            val minX = maxOf(0, depth - world.height + 1)
            val maxX = minOf(world.width - 1, depth)

            for (x in minX..maxX) {
                val y = depth - x
                val elevation = world.getHeight(x, y) ?: continue
                val tile = world.getTile(x, y) ?: continue
                val sideIndices = mutableListOf<Int>()

                if (hasFillFor(tile)) {
                    for (part in TerrainSidePlan.create(world, x, y)) {
                        sideIndices += primitives.size
                        primitives += TerrainSide(
                            x = x,
                            y = y,
                            surfaceElevation = elevation,
                            levelBelowSurface = part.levelBelowSurface,
                            face = part.face
                        )
                    }
                }

                topIndexByPosition[TilePosition(x, y)] = primitives.size
                primitives += TerrainTop(x, y, elevation)
                val topIndex = primitives.lastIndex

                for (sideIndex in sideIndices) {
                    explicitDependencies += IsoRenderDependency(
                        before = sideIndex,
                        after = topIndex
                    )
                }

                for (first in sideIndices) {
                    for (second in sideIndices) {
                        val firstSide = primitives[first] as TerrainSide
                        val secondSide = primitives[second] as TerrainSide

                        if (
                            firstSide.bottomElevation <
                            secondSide.bottomElevation
                        ) {
                            explicitDependencies += IsoRenderDependency(
                                before = first,
                                after = second
                            )
                        }
                    }
                }
            }
        }

        for (placed in world.getObjects()) {
            val objectIndex = primitives.size
            val primitive = WorldObjectPrimitive(
                placedObject = placed,
                supportElevation = world.getHeight(placed.x, placed.y) ?: 0
            )

            primitives += primitive

            for (position in primitive.occupiedTiles) {
                val topIndex = topIndexByPosition[position] ?: continue
                explicitDependencies += IsoRenderDependency(
                    before = topIndex,
                    after = objectIndex
                )
            }
        }

        return IsoRenderOrder.backToFront(
            items = primitives,
            projection = projection,
            explicitDependencies = explicitDependencies
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
