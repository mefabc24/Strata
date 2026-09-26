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
    override val sortVolume = worldEntity.position.let { position ->
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

/**
 * Cached world-render data that changes only when static world objects change.
 */
internal class StaticWorldRenderPlan(
    val items: List<WorldRenderPrimitive>,
    val dependencies: List<IsoRenderDependency>,
    val terrainIndexByCell: IntArray,
    val objectIndices: IntArray,
    val orderedItems: List<WorldRenderPrimitive>
)

/** Builds a grid-aware, deterministic world rendering sequence. */
internal object WorldRenderPlan {

    /**
     * Prepares terrain and placed-object ordering.
     *
     * Object-to-object spatial relationships are intentionally calculated here
     * so they can be reused while movable entities change position.
     */
    fun prepareStatic(
        world: World,
        projection: IsoProjection,
        metrics: IsoRenderOrderMetrics? = null
    ): StaticWorldRenderPlan {
        val items = mutableListOf<WorldRenderPrimitive>()
        val terrainIndexByCell =
            IntArray(world.width * world.height) { -1 }

        val explicitDependencies =
            mutableListOf<IsoRenderDependency>()

        for (depth in 0 until world.width + world.height - 1) {
            val minX = maxOf(0, depth - world.height + 1)
            val maxX = minOf(world.width - 1, depth)

            for (x in minX..maxX) {
                val y = depth - x

                world.getTile(x, y) ?: continue

                val cellIndex = items.size

                items += TerrainCell(
                    x = x,
                    y = y
                )

                terrainIndexByCell[
                    y * world.width + x
                ] = cellIndex
            }
        }

        val objectIndices = mutableListOf<Int>()

        for (placed in world.getObjects()) {
            val objectIndex = items.size
            val primitive = WorldObjectPrimitive(placed)

            items += primitive
            objectIndices += objectIndex

            for (position in primitive.occupiedTiles) {
                val cellIndex =
                    position.y * world.width + position.x

                val terrainIndex =
                    terrainIndexByCell[cellIndex]

                if (terrainIndex >= 0) {
                    explicitDependencies +=
                        IsoRenderDependency(
                            before = terrainIndex,
                            after = objectIndex
                        )
                }
            }
        }

        val objectCandidates = buildList {
            for (first in objectIndices.indices) {
                for (
                second in first + 1 until objectIndices.size
                ) {
                    add(
                        IsoRenderCandidate(
                            first = objectIndices[first],
                            second = objectIndices[second]
                        )
                    )
                }
            }
        }

        val objectDependencies =
            IsoRenderOrder.dependenciesFor(
                items = items,
                relationCandidates = objectCandidates,
                metrics = metrics
            )

        val dependencies =
            explicitDependencies + objectDependencies

        val orderedItems =
            IsoRenderOrder.backToFront(
                items = items,
                projection = projection,
                explicitDependencies = dependencies
            )

        return StaticWorldRenderPlan(
            items = items,
            dependencies = dependencies,
            terrainIndexByCell = terrainIndexByCell,
            objectIndices = objectIndices.toIntArray(),
            orderedItems = orderedItems
        )
    }

    /**
     * Adds the current movable entities to a prepared static world plan.
     *
     * Only entity-to-object and entity-to-entity spatial relationships are
     * recalculated.
     */
    fun withEntities(
        staticPlan: StaticWorldRenderPlan,
        world: World,
        projection: IsoProjection,
        metrics: IsoRenderOrderMetrics? = null
    ): List<WorldRenderPrimitive> {
        val entities = world.getEntities().toList()

        if (entities.isEmpty()) {
            metrics?.relationChecks = 0
            return staticPlan.orderedItems
        }

        val items = ArrayList<WorldRenderPrimitive>(
            staticPlan.items.size + entities.size
        )

        items.addAll(staticPlan.items)

        val dependencies =
            ArrayList<IsoRenderDependency>(
                staticPlan.dependencies.size + entities.size
            )

        dependencies.addAll(staticPlan.dependencies)

        val dynamicCandidates =
            mutableListOf<IsoRenderCandidate>()

        val entityIndices =
            mutableListOf<Int>()

        for (entity in entities) {
            val entityIndex = items.size

            items += WorldEntityPrimitive(entity)

            for (objectIndex in staticPlan.objectIndices) {
                dynamicCandidates += IsoRenderCandidate(
                    first = objectIndex,
                    second = entityIndex
                )
            }

            for (otherEntityIndex in entityIndices) {
                dynamicCandidates += IsoRenderCandidate(
                    first = otherEntityIndex,
                    second = entityIndex
                )
            }

            entityIndices += entityIndex

            val tile = entity.currentTile

            if (
                tile.x in 0 until world.width &&
                tile.y in 0 until world.height
            ) {
                val terrainIndex =
                    staticPlan.terrainIndexByCell[
                        tile.y * world.width + tile.x
                    ]

                if (terrainIndex >= 0) {
                    dependencies += IsoRenderDependency(
                        before = terrainIndex,
                        after = entityIndex
                    )
                }
            }
        }

        return IsoRenderOrder.backToFront(
            items = items,
            projection = projection,
            relationCandidates = dynamicCandidates,
            explicitDependencies = dependencies,
            metrics = metrics
        )
    }

    /**
     * Builds a complete plan without caching.
     *
     * This remains useful for isolated operations such as picking and tests.
     */
    fun create(
        world: World,
        projection: IsoProjection,
        metrics: IsoRenderOrderMetrics? = null
    ): List<WorldRenderPrimitive> {
        val staticMetrics =
            metrics?.let { IsoRenderOrderMetrics() }

        val staticPlan = prepareStatic(
            world = world,
            projection = projection,
            metrics = staticMetrics
        )

        if (world.getEntities().isEmpty()) {
            metrics?.relationChecks =
                staticMetrics?.relationChecks ?: 0

            return staticPlan.orderedItems
        }

        val dynamicMetrics =
            metrics?.let { IsoRenderOrderMetrics() }

        val result = withEntities(
            staticPlan = staticPlan,
            world = world,
            projection = projection,
            metrics = dynamicMetrics
        )

        metrics?.relationChecks =
            (staticMetrics?.relationChecks ?: 0) +
                    (dynamicMetrics?.relationChecks ?: 0)

        return result
    }

    /** Appends placement previews after every normal world primitive. */
    fun withPreviews(
        normalItems: List<WorldRenderPrimitive>,
        previews: List<PlacementPreview>
    ): List<WorldRenderItem> {
        if (previews.isEmpty()) return normalItems

        return normalItems +
                previews.map(::PreviewRenderItem)
    }
}
