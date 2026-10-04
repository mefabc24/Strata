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

/** A world visual that belongs to a strict render-priority group. */
internal sealed interface PrioritizedWorldRenderPrimitive :
    WorldRenderPrimitive {
    val renderPriority: Int
}

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
    val placedObject: PlacedObject,
    override val renderPriority: Int = 0
) : PrioritizedWorldRenderPrimitive {
    val occupiedTiles: Set<TilePosition> = placedObject.occupiedTiles()

    val isSingleTile: Boolean =
        occupiedTiles.size == 1

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
    val worldEntity: WorldEntity,
    override val renderPriority: Int = 0
) : PrioritizedWorldRenderPrimitive {
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
        metrics: IsoRenderOrderMetrics? = null,
        objectPriorityFor: (PlacedObject) -> Int = { 0 }
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
            val primitive = WorldObjectPrimitive(
                placedObject = placed,
                renderPriority = objectPriorityFor(placed)
            )

            items += primitive
            objectIndices += objectIndex

            for ((x, y) in primitive.occupiedTiles) {
                val cellIndex =
                    y * world.width + x

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

        val objectCandidates =
            buildObjectRelationCandidates(
                items = items,
                objectIndices = objectIndices
            )

        val objectDependencies =
            IsoRenderOrder.dependenciesFor(
                items = items,
                relationCandidates = objectCandidates,
                metrics = metrics
            )

        val dependencies =
            explicitDependencies + objectDependencies

        val spatiallyOrderedItems =
            IsoRenderOrder.backToFront(
                items = items,
                projection = projection,
                explicitDependencies = dependencies
            )

        val orderedItems = orderPriorityGroups(spatiallyOrderedItems)

        return buildStaticPlan(
            world = world,
            orderedItems = orderedItems,
            objects = world.getObjects().toSet()
        )
    }

    private fun buildObjectRelationCandidates(
        items: List<WorldRenderPrimitive>,
        objectIndices: List<Int>
    ): List<IsoRenderCandidate> {
        val complexPositions = objectIndices.indices.filterNot { position ->
            (items[objectIndices[position]] as WorldObjectPrimitive).isSingleTile
        }

        if (complexPositions.isEmpty()) {
            return emptyList()
        }

        return buildList {
            for (firstPosition in complexPositions) {
                val firstIndex = objectIndices[firstPosition]

                for (secondPosition in objectIndices.indices) {
                    if (secondPosition == firstPosition) {
                        continue
                    }

                    val secondIndex = objectIndices[secondPosition]
                    val second =
                        items[secondIndex] as WorldObjectPrimitive

                    if (
                        second.renderPriority !=
                        (items[firstIndex] as WorldObjectPrimitive).renderPriority
                    ) {
                        continue
                    }

                    /*
                     * Complex-to-complex pairs are emitted only once.
                     * Single-to-single pairs need no explicit relation.
                     */
                    if (
                        !second.isSingleTile &&
                        secondPosition < firstPosition
                    ) {
                        continue
                    }

                    add(
                        IsoRenderCandidate(
                            first = firstIndex,
                            second = secondIndex
                        )
                    )
                }
            }
        }
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
        metrics: IsoRenderOrderMetrics? = null,
        objectPriorityFor: (PlacedObject) -> Int = { 0 }
    ): StaticWorldRenderPlan {
        metrics?.relationChecks = 0

        val currentObjects = world.getObjects()

        val removedObjects =
            previous.objects.filterNot(currentObjects::contains)

        val addedObjects =
            currentObjects.filterNot(previous.objects::contains)

        if (
            removedObjects.isEmpty() &&
            addedObjects.isEmpty()
        ) {
            return previous
        }

        val removedSet = removedObjects.toSet()

        val baseItems = previous.orderedItems
            .filterNot { item ->
                item is WorldObjectPrimitive &&
                        item.placedObject in removedSet
            }

        /*
         * Removal only needs the previous order with the removed objects filtered
         * out. No spatial relationships need to be recalculated.
         */
        if (addedObjects.isEmpty()) {
            return buildStaticPlan(
                world = world,
                orderedItems = baseItems,
                objects = currentObjects.toSet()
            )
        }

        val baseTerrainIndexByCell =
            IntArray(world.width * world.height) { -1 }

        val baseObjectIndices =
            mutableListOf<Int>()

        val baseComplexObjectIndices =
            mutableListOf<Int>()

        for ((index, item) in baseItems.withIndex()) {
            when (item) {
                is TerrainCell -> {
                    baseTerrainIndexByCell[
                        item.y * world.width + item.x
                    ] = index
                }

                is WorldObjectPrimitive -> {
                    baseObjectIndices += index

                    if (!item.isSingleTile) {
                        baseComplexObjectIndices += index
                    }
                }

                is WorldEntityPrimitive -> {
                    error(
                        "Static world render plans must not contain entities."
                    )
                }
            }
        }

        val addedPrimitives = addedObjects.map { placed ->
            WorldObjectPrimitive(
                placedObject = placed,
                renderPriority = objectPriorityFor(placed)
            )
        }

        val items = ArrayList<WorldRenderPrimitive>(
            baseItems.size + addedPrimitives.size
        )

        items.addAll(baseItems)
        items.addAll(addedPrimitives)

        val order = StableDependencyOrder(
            items = items,
            comparator =
                IsoRenderOrder.comparator(
                    projection
                )
        )

        /*
         * The previous static plan is already valid.
         *
         * Preserve its complete order with a linear dependency chain instead of
         * reconstructing relationships between existing objects.
         */
        for (index in 0 until baseItems.lastIndex) {
            order.add(
                before = index,
                after = index + 1
            )
        }

        /*
         * Relate every new object to the already ordered static world.
         *
         * Because the base items form a total chain, only the nearest required
         * predecessor and successor need to become explicit dependencies.
         */
        for ((offset, primitive) in addedPrimitives.withIndex()) {
            val primitiveIndex =
                baseItems.size + offset

            var lastRequiredBefore = -1
            var firstRequiredAfter = baseItems.size

            for ((x, y) in primitive.occupiedTiles) {
                val cellIndex =
                    y * world.width + x

                val terrainIndex =
                    baseTerrainIndexByCell[cellIndex]

                if (terrainIndex >= 0) {
                    lastRequiredBefore =
                        maxOf(
                            lastRequiredBefore,
                            terrainIndex
                        )
                }
            }

            val relationIndices =
                if (primitive.isSingleTile) {
                    baseComplexObjectIndices
                } else {
                    baseObjectIndices
                }

            for (objectIndex in relationIndices) {
                val existing =
                    items[objectIndex] as WorldObjectPrimitive

                if (existing.renderPriority != primitive.renderPriority) {
                    continue
                }

                metrics?.let {
                    it.relationChecks++
                }

                when (
                    existing.sortVolume.relationTo(
                        primitive.sortVolume
                    )
                ) {
                    IsoSpatialRelation.BEHIND -> {
                        lastRequiredBefore =
                            maxOf(
                                lastRequiredBefore,
                                objectIndex
                            )
                    }

                    IsoSpatialRelation.IN_FRONT -> {
                        firstRequiredAfter =
                            minOf(
                                firstRequiredAfter,
                                objectIndex
                            )
                    }

                    IsoSpatialRelation.AMBIGUOUS -> Unit
                }
            }

            /*
             * The new object would require the already valid base order to change.
             * Preserve correctness by falling back to a complete rebuild.
             */
            if (
                lastRequiredBefore >=
                firstRequiredAfter
            ) {
                return prepareStatic(
                    world = world,
                    projection = projection,
                    metrics = metrics,
                    objectPriorityFor = objectPriorityFor
                )
            }

            if (lastRequiredBefore >= 0) {
                order.add(
                    before = lastRequiredBefore,
                    after = primitiveIndex
                )
            }

            if (firstRequiredAfter < baseItems.size) {
                order.add(
                    before = primitiveIndex,
                    after = firstRequiredAfter
                )
            }
        }

        val complexAddedPositions =
            addedPrimitives.indices.filter { position ->
                !addedPrimitives[position].isSingleTile
            }

        for (firstPosition in complexAddedPositions) {
            val firstPrimitive =
                addedPrimitives[firstPosition]

            val firstIndex =
                baseItems.size + firstPosition

            for (secondPosition in addedPrimitives.indices) {
                if (secondPosition == firstPosition) {
                    continue
                }

                val secondPrimitive =
                    addedPrimitives[secondPosition]

                if (
                    secondPrimitive.renderPriority !=
                    firstPrimitive.renderPriority
                ) {
                    continue
                }

                /*
                 * Complex-to-complex pairs are evaluated only once.
                 * Single-to-single pairs rely on the normal deterministic comparator.
                 */
                if (
                    !secondPrimitive.isSingleTile &&
                    secondPosition < firstPosition
                ) {
                    continue
                }

                val secondIndex =
                    baseItems.size + secondPosition

                metrics?.let {
                    it.relationChecks++
                }

                when (
                    firstPrimitive.sortVolume.relationTo(
                        secondPrimitive.sortVolume
                    )
                ) {
                    IsoSpatialRelation.BEHIND -> {
                        order.add(
                            before = firstIndex,
                            after = secondIndex
                        )
                    }

                    IsoSpatialRelation.IN_FRONT -> {
                        order.add(
                            before = secondIndex,
                            after = firstIndex
                        )
                    }

                    IsoSpatialRelation.AMBIGUOUS -> Unit
                }
            }
        }

        val orderedItems = orderPriorityGroups(order.resolve())

        return buildStaticPlan(
            world = world,
            orderedItems = orderedItems,
            objects = currentObjects.toSet()
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
        metrics: IsoRenderOrderMetrics? = null,
        entityPriorityFor: (WorldEntity) -> Int = { 0 }
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

            val entityPrimitive = WorldEntityPrimitive(
                worldEntity = entity,
                renderPriority = entityPriorityFor(entity)
            )

            items += entityPrimitive

            /*
             * Only movable entities need fresh spatial comparisons.
             * Static objects are already ordered relative to each other.
             */
            for (objectIndex in staticPlan.objectIndices) {
                val objectPrimitive =
                    staticItems[objectIndex] as WorldObjectPrimitive

                if (
                    objectPrimitive.renderPriority !=
                    entityPrimitive.renderPriority
                ) {
                    continue
                }

                dynamicCandidates += IsoRenderCandidate(
                    first = objectIndex,
                    second = entityIndex
                )
            }

            for (otherEntityIndex in entityIndices) {
                val otherEntity =
                    items[otherEntityIndex] as WorldEntityPrimitive

                if (
                    otherEntity.renderPriority !=
                    entityPrimitive.renderPriority
                ) {
                    continue
                }

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

        return orderPriorityGroups(
            IsoRenderOrder.backToFront(
                items = items,
                projection = projection,
                relationCandidates = dynamicCandidates,
                explicitDependencies = dependencies,
                metrics = metrics
            )
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
        metrics: IsoRenderOrderMetrics? = null,
        objectPriorityFor: (PlacedObject) -> Int = { 0 },
        entityPriorityFor: (WorldEntity) -> Int = { 0 }
    ): List<WorldRenderPrimitive> {
        val staticMetrics =
            metrics?.let { IsoRenderOrderMetrics() }

        val staticPlan = prepareStatic(
            world = world,
            projection = projection,
            metrics = staticMetrics,
            objectPriorityFor = objectPriorityFor
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
            metrics = dynamicMetrics,
            entityPriorityFor = entityPriorityFor
        )

        metrics?.relationChecks =
            (staticMetrics?.relationChecks ?: 0) +
                    (dynamicMetrics?.relationChecks ?: 0)

        return result
    }

    /**
     * Keeps terrain in its existing structural positions while making the
     * object/entity subsequence strictly ordered by priority. The stable group
     * ordering preserves the already-resolved spatial order inside each group.
     */
    private fun orderPriorityGroups(
        items: List<WorldRenderPrimitive>
    ): List<WorldRenderPrimitive> {
        val visuals = items.filterIsInstance<PrioritizedWorldRenderPrimitive>()

        if (
            visuals.size < 2 ||
            visuals.all { it.renderPriority == visuals.first().renderPriority }
        ) {
            return items
        }

        val orderedVisuals = visuals
            .groupBy(PrioritizedWorldRenderPrimitive::renderPriority)
            .toSortedMap()
            .values
            .flatten()
            .iterator()

        return items.map { item ->
            if (item is PrioritizedWorldRenderPrimitive) {
                orderedVisuals.next()
            } else {
                item
            }
        }
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
