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
    val orderedItems: List<WorldRenderPrimitive>,
    val terrainIndexByCell: IntArray,
    val objectIndices: IntArray,
    val objects: Set<PlacedObject>
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

        return buildStaticPlan(
            world = world,
            orderedItems = orderedItems,
            objects = world.getObjects().toSet()
        )
    }

    /**
     * Updates an existing static plan after world objects changed.
     *
     * Existing static ordering is preserved whenever new objects can be inserted
     * without reordering previous objects. If that is not possible, a complete
     * rebuild is used as a correctness fallback.
     */
    fun updateStatic(
        previous: StaticWorldRenderPlan,
        world: World,
        projection: IsoProjection,
        metrics: IsoRenderOrderMetrics? = null
    ): StaticWorldRenderPlan {
        metrics?.relationChecks = 0

        val currentObjects = world.getObjects()
        val removedObjects = previous.objects.filterNot(currentObjects::contains)
        val addedObjects = currentObjects.filterNot(previous.objects::contains)

        if (removedObjects.isEmpty() && addedObjects.isEmpty()) {
            return previous
        }

        val removedSet = removedObjects.toSet()

        val orderedItems = previous.orderedItems
            .filterNot { item ->
                item is WorldObjectPrimitive &&
                        item.placedObject in removedSet
            }
            .toMutableList()

        for (placedObject in addedObjects) {
            val primitive = WorldObjectPrimitive(placedObject)

            val insertionIndex = findInsertionIndex(
                items = orderedItems,
                primitive = primitive,
                worldWidth = world.width,
                projection = projection,
                metrics = metrics
            )

            /*
             * A new object can occasionally require previously ambiguous objects
             * to change their relative order. In that case the incremental path
             * cannot preserve the existing total order safely.
             */
            if (insertionIndex == null) {
                return prepareStatic(
                    world = world,
                    projection = projection,
                    metrics = metrics
                )
            }

            orderedItems.add(
                index = insertionIndex,
                element = primitive
            )
        }

        return buildStaticPlan(
            world = world,
            orderedItems = orderedItems,
            objects = currentObjects.toSet()
        )
    }

    private fun findInsertionIndex(
        items: List<WorldRenderPrimitive>,
        primitive: WorldObjectPrimitive,
        worldWidth: Int,
        projection: IsoProjection,
        metrics: IsoRenderOrderMetrics?
    ): Int? {
        var lastRequiredBefore = -1
        var firstRequiredAfter = items.size

        val supportCells = primitive.occupiedTiles
            .mapTo(mutableSetOf()) { position ->
                position.y * worldWidth + position.x
            }

        for ((index, item) in items.withIndex()) {
            when (item) {
                is TerrainCell -> {
                    val cell = item.y * worldWidth + item.x

                    if (cell in supportCells) {
                        lastRequiredBefore =
                            maxOf(lastRequiredBefore, index)
                    }
                }

                is WorldObjectPrimitive -> {
                    metrics?.let {
                        it.relationChecks++
                    }

                    when (
                        item.sortVolume.relationTo(
                            primitive.sortVolume
                        )
                    ) {
                        IsoSpatialRelation.BEHIND -> {
                            lastRequiredBefore =
                                maxOf(lastRequiredBefore, index)
                        }

                        IsoSpatialRelation.IN_FRONT -> {
                            firstRequiredAfter =
                                minOf(firstRequiredAfter, index)
                        }

                        IsoSpatialRelation.AMBIGUOUS -> Unit
                    }
                }

                is WorldEntityPrimitive -> {
                    error(
                        "Static world render plans must not contain entities."
                    )
                }
            }
        }

        /*
         * Existing static items would have to change their relative order.
         * Let the full sorter handle that uncommon case.
         */
        if (lastRequiredBefore >= firstRequiredAfter) {
            return null
        }

        val comparator =
            IsoRenderOrder.comparator<WorldRenderPrimitive>(
                projection
            )

        /*
         * Hard spatial dependencies define the valid insertion interval.
         * Inside that interval the normal deterministic fallback comparator
         * chooses the preferred position.
         */
        for (
        index in
        lastRequiredBefore + 1 until firstRequiredAfter
        ) {
            if (
                comparator.compare(
                    primitive,
                    items[index]
                ) < 0
            ) {
                return index
            }
        }

        return firstRequiredAfter
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

        val staticItems = staticPlan.orderedItems

        val items = ArrayList<WorldRenderPrimitive>(
            staticItems.size + entities.size
        )

        items.addAll(staticItems)

        val dependencies = ArrayList<IsoRenderDependency>(
            staticItems.size + entities.size
        )

        /*
         * The static world has already been fully ordered.
         *
         * Preserve that order with a linear chain instead of rebuilding the
         * original object dependency graph every frame.
         */
        for (index in 0 until staticItems.lastIndex) {
            dependencies += IsoRenderDependency(
                before = index,
                after = index + 1
            )
        }

        val dynamicCandidates =
            mutableListOf<IsoRenderCandidate>()

        val entityIndices =
            mutableListOf<Int>()

        for (entity in entities) {
            val entityIndex = items.size

            items += WorldEntityPrimitive(entity)

            /*
             * Only movable entities need fresh spatial comparisons.
             * Static objects are already ordered relative to each other.
             */
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

    private fun buildStaticPlan(
        world: World,
        orderedItems: List<WorldRenderPrimitive>,
        objects: Set<PlacedObject>
    ): StaticWorldRenderPlan {
        val terrainIndexByCell =
            IntArray(world.width * world.height) { -1 }

        val objectIndices = mutableListOf<Int>()

        for ((index, item) in orderedItems.withIndex()) {
            when (item) {
                is TerrainCell -> {
                    terrainIndexByCell[
                        item.y * world.width + item.x
                    ] = index
                }

                is WorldObjectPrimitive -> {
                    objectIndices += index
                }

                is WorldEntityPrimitive -> {
                    error(
                        "Static world render plans must not contain entities."
                    )
                }
            }
        }

        return StaticWorldRenderPlan(
            orderedItems = orderedItems,
            terrainIndexByCell = terrainIndexByCell,
            objectIndices = objectIndices.toIntArray(),
            objects = objects
        )
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
