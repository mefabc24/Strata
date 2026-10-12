package com.mefabc24.strata.iso

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.mefabc24.strata.camera.CameraBounds
import com.mefabc24.strata.camera.CameraController
import com.mefabc24.strata.camera.CameraViewport
import com.badlogic.gdx.InputMultiplexer
import com.mefabc24.strata.input.WorldInputProcessor
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.render.IsoWorldRenderer
import com.mefabc24.strata.lighting.Lighting
import com.mefabc24.strata.render.debug.IsoGridRenderer
import com.mefabc24.strata.render.debug.IsoWorldDebugRenderer
import com.mefabc24.strata.render.debug.IsoAdvancedDebugRenderer
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.camera.ZoomMode
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.render.`object`.IsoObjectBounds
import com.mefabc24.strata.render.`object`.ResolvedObjectVisual
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.entity.IsoEntityBounds
import com.mefabc24.strata.render.entity.ResolvedEntityVisual
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.preview.EntityPreview
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity
import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.ControlsSettings
import com.mefabc24.strata.render.RenderingSettings
import com.mefabc24.strata.render.order.WorldEntityPrimitive
import com.mefabc24.strata.render.order.WorldObjectPrimitive
import com.mefabc24.strata.render.order.WorldRenderPrimitive
import com.mefabc24.strata.debug.DebugGridSettings
import com.mefabc24.strata.debug.DebugObjectSettings
import com.mefabc24.strata.debug.DebugEntitySettings
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.debug.DebugGridExtent
import com.mefabc24.strata.debug.DebugGridRenderLayer
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.debug.DebugVisualizationFilter
import com.mefabc24.strata.debug.DebugVisualizationFilterContext
import com.mefabc24.strata.debug.hasActiveVisuals
import com.mefabc24.strata.render.RenderDebugSnapshot

/**
 * Determines how placed objects are picked.
 */
enum class ObjectPickingMode {

    /**
     * Picks objects only through their occupied ground tiles.
     */
    FOOTPRINT,

    /**
     * Picks objects through their visible sprite pixels.
     */
    SPRITE_ALPHA,

    /**
     * Picks sprite pixels first, then falls back to occupied ground tiles.
     */
    SPRITE_OR_FOOTPRINT,

    /**
     * Disables object picking.
     */
    NONE
}

/** Determines how movable world entities are picked. */
enum class EntityPickingMode {
    SPRITE_ALPHA,
    NONE
}

/**
 * Manages the camera and viewport for an isometric world.
 */
@Suppress("unused")
class IsoWorldView(
    private val world: World,
    private val textureFor: (Tile, Float) -> TextureRegion?,
    private val objectVisualFor: (PlacedObject) -> ObjectVisual? = { null },
    private val entityVisualFor: (WorldEntity) -> EntityVisual? = { null },

    cameraSettings: CameraSettings = CameraSettings(),
    controls: ControlsSettings = ControlsSettings(),
    renderingSettings: RenderingSettings = RenderingSettings(),
    lighting: Lighting = Lighting(),
    debugGridSettings: DebugGridSettings = DebugGridSettings(),
    private val resolvedObjectVisualFor: (
        (PlacedObject, Float) -> ResolvedObjectVisual?
    )? = null,
    private val resolvedEntityVisualFor: (
        (WorldEntity, Float) -> ResolvedEntityVisual?
    )? = null,
    private val resolvedRepresentativeEntityVisualFor: (
        (WorldEntity, Float) -> ResolvedEntityVisual?
    )? = null,
    private val objectPriorityFor: (PlacedObject) -> Int = { 0 },
    private val entityPriorityFor: (WorldEntity) -> Int = { 0 },
    debugObjectSettings: DebugObjectSettings = DebugObjectSettings(),
    debugEntitySettings: DebugEntitySettings = DebugEntitySettings(),
    private val debugSettings: DebugSettings? = null,
    private val terrainIdFor: ((Tile) -> TerrainId)? = null
) {

    private val cameraConfig = cameraSettings.copy().also {
        it.validate()
    }

    private val renderingConfig = renderingSettings.copy().also {
        it.validate()
    }

    private val maxTerrainSpriteHeight =
        renderingConfig.maxTerrainSpriteHeight ?: Float.POSITIVE_INFINITY

    private val boundsSpriteHeight =
        if (maxTerrainSpriteHeight.isFinite()) {
            maxTerrainSpriteHeight
        } else {
            renderingConfig.tileGeometry.height
        }

    val camera = OrthographicCamera()

    private val projection = IsoProjection(
        geometry = renderingConfig.tileGeometry
    )

    var hoveredTile: TilePosition? = null
        private set

    internal var hoveredGridPosition: TilePosition? = null
        private set

    private var viewportValid =
        Gdx.graphics.width > 0 && Gdx.graphics.height > 0

    internal val pickingAvailable: Boolean
        get() = viewportValid &&
            Gdx.graphics.width > 0 && Gdx.graphics.height > 0

    private var requestedWorldInputEnabled = true

    var animationTime: Float = 0f
        private set

    private val worldBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = 0f,
        maxSpriteHeight = boundsSpriteHeight
    )

    private val worldRenderer = IsoWorldRenderer(
        projection = projection,
        objectSettings = renderingConfig.objects,
        lighting = lighting,
        collectDebugSnapshot = {
            debugSettings?.let { settings ->
                settings.visuals.picking.hasActiveVisuals ||
                        settings.visuals.renderOrder.hasActiveVisuals ||
                        settings.visuals.culling.hasActiveVisuals ||
                        settings.visuals.filter == DebugVisualizationFilter.VISIBLE &&
                        (settings.visuals.objects.hasActiveVisuals || settings.visuals.entities.hasActiveVisuals) ||
                        settings.worldState.inspectionHighlightVisible
            } ?: false
        },
        objectPriorityFor = objectPriorityFor,
        entityPriorityFor = entityPriorityFor,
        visibility = debugSettings?.visuals?.worldVisibility
            ?: com.mefabc24.strata.debug.DebugWorldVisibilitySettings()
    )

    private val debugGridConfig = debugGridSettings
    private val debugFilterContext = DebugVisualizationFilterContext()

    private val gridRenderer = IsoGridRenderer(
        projection = projection,
        settings = debugGridConfig
    )

    private val worldDebugRenderer = IsoWorldDebugRenderer(
        projection = projection,
        objectSettings = debugObjectSettings,
        entitySettings = debugEntitySettings,
        objectRenderingSettings = renderingConfig.objects,
        filterContext = debugFilterContext,
        trailRecorder = debugSettings?.worldState?.entityTrails
    )

    private val advancedDebugRenderer = lazy {
        IsoAdvancedDebugRenderer(
            projection = projection,
            settings = checkNotNull(debugSettings),
            state = checkNotNull(debugSettings).worldState,
            filterContext = debugFilterContext
        )
    }

    private val debugObjectVisualFor =
        { placed: PlacedObject, time: Float ->
            resolvedObjectVisualFor?.invoke(placed, time)
                ?: objectVisualFor(placed)?.let { visual ->
                    ResolvedObjectVisual(visual, time, null)
                }
        }

    private val effectiveEntityVisualFor =
        { entity: WorldEntity, time: Float ->
            val resolve = { effectiveTime: Float ->
                val waitingForDebugPath = debugSettings?.worldState?.let { state ->
                    state.pathfindingEntityWaiting && state.pathfindingEntity === entity
                } == true
                if (waitingForDebugPath) {
                    resolvedRepresentativeEntityVisualFor?.invoke(entity, effectiveTime)
                } else {
                    null
                } ?: resolvedEntityVisualFor?.invoke(entity, effectiveTime)
                    ?: entityVisualFor(entity)?.let { visual ->
                        ResolvedEntityVisual(visual, effectiveTime, null)
                    }
            }
            debugSettings?.entityFreezeState?.resolveAnimation(
                entity = entity,
                animationTime = time,
                freezeAnimation = debugSettings.tools.inspect.freezeEntityAnimation,
                resolve = resolve
            ) ?: resolve(time)
        }

    private val debugEntityVisualFor =
        { entity: WorldEntity, time: Float ->
            effectiveEntityVisualFor(entity, time)
        }

    /**
     * Rendering statistics from the most recent frame.
     */
    val renderStats: RenderStats
        get() = worldRenderer.stats

    val renderDebugSnapshot: RenderDebugSnapshot?
        get() = worldRenderer.debugSnapshot

    private val cameraBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = cameraConfig.cameraPadding,
        maxSpriteHeight = boundsSpriteHeight
    ).let {
        CameraBounds(
            minX = it.x,
            minY = it.y,
            maxX = it.x + it.width,
            maxY = it.y + it.height
        )
    }

    private val zoomBounds = projection.worldBounds(
        width = world.width,
        height = world.height,
        padding = cameraConfig.cameraPadding,
        maxSpriteHeight = boundsSpriteHeight
    ).let {
        CameraBounds(
            minX = it.x,
            minY = it.y,
            maxX = it.x + it.width,
            maxY = it.y + it.height,
            edgeAllowance = cameraConfig.zoomEdgeAllowance
        )
    }

    private val viewport = CameraViewport(
        camera = camera,
        mode = cameraConfig.viewportMode,
        virtualHeight = cameraConfig.virtualHeight,
        bounds = zoomBounds
    )

    val cameraController = CameraController(
        camera = camera,
        settings = cameraConfig,
        bounds = cameraBounds,
        zoomBounds = zoomBounds,
        worldZoomBounds = worldBounds,
        controls = controls.camera
    )

    private val tilePicker = TilePicker(
        camera = camera,
        projection = projection,
        world = world
    )

    private val objectPicker = ObjectPicker(
        camera = camera,
        projection = projection,
        world = world,
        visualFor = objectVisualFor,
        animationTime = { animationTime },
        orderedObjects = {
            worldRenderer.currentRenderPlan(world)
                .mapNotNull { primitive ->
                    (primitive as? WorldObjectPrimitive)?.placedObject
                }
        },
        objectSettings = renderingConfig.objects,
        resolvedVisualFor = resolvedObjectVisualFor
    )

    private val entityPicker = EntityPicker(
        camera = camera,
        projection = projection,
        world = world,
        visualFor = entityVisualFor,
        animationTime = { animationTime },
        orderedEntities = {
            worldRenderer.currentRenderPlan(world)
                .mapNotNull { primitive ->
                    (primitive as? WorldEntityPrimitive)?.worldEntity
                }
        },
        resolvedVisualFor = effectiveEntityVisualFor
    )

    private val worldInputProcessor = WorldInputProcessor(
        bindings = controls.gameplay.bindings,
        pickTile = { screenX, screenY ->
            tilePicker.pick(screenX, screenY)
        },
        pickObject = { screenX, screenY, mode ->
            pickObject(screenX, screenY, mode)
        },
        pickEntity = { screenX, screenY, mode ->
            pickEntity(screenX, screenY, mode)
        },
        pickGrid = { screenX, screenY ->
            tilePicker.pickGrid(screenX, screenY)
        }
    )

    /**
     * Controls whether world input bindings are processed.
     */
    var worldInputEnabled: Boolean
        get() = requestedWorldInputEnabled
        set(value) {
            requestedWorldInputEnabled = value
            syncWorldInputAvailability()
        }

    val inputProcessor = InputMultiplexer(
        worldInputProcessor,
        cameraController.inputProcessor
    )

    init {
        viewport.resize(
            Gdx.graphics.width,
            Gdx.graphics.height
        )

        if (cameraConfig.zoomMode == ZoomMode.WORLD_BASED) {
            cameraController.fitWorld()
        } else {
            camera.position.set(
                worldBounds.x + worldBounds.width / 2f,
                worldBounds.y + worldBounds.height / 2f,
                0f
            )

            cameraController.refreshZoomBounds()
        }
    }

    /**
     * Updates camera and hover state in real time while advancing world
     * animations with simulation time.
     */
    fun update(
        realDelta: Float,
        simulationDelta: Float = realDelta
    ) {
        require(realDelta.isFinite() && realDelta >= 0f) {
            "Real frame delta must be finite and non-negative."
        }
        require(simulationDelta.isFinite() && simulationDelta >= 0f) {
            "Simulation delta must be finite and non-negative."
        }

        val freezeVisualAnimations = debugSettings?.operations?.simulation?.let {
            it.enabled && it.freezeVisualAnimations
        } == true
        if (!freezeVisualAnimations) {
            animationTime += simulationDelta
        }
        cameraController.update(realDelta)

        syncWorldInputAvailability()
        if (pickingAvailable) {
            hoveredGridPosition = tilePicker.pickGrid(
                Gdx.input.x.toFloat(),
                Gdx.input.y.toFloat()
            )
            hoveredTile = hoveredGridPosition?.takeIf { position ->
                world.getTile(position) != null
            }
        } else {
            clearHoverState()
        }
    }

    /**
     * Renders terrain, world objects, and placement previews.
     */
    fun render(previews: List<PlacementPreview> = emptyList()) {
        val debugMovePreview = debugSettings?.worldState?.movePreview?.objectPreview
        val visiblePreviews = if (debugMovePreview == null) {
            previews
        } else {
            previews + debugMovePreview
        }
        val entityPreviews = debugSettings?.worldState?.let { state ->
            listOfNotNull(
                state.spawnPreview,
                state.movePreview?.entityPreview
            )
        }.orEmpty()
        worldRenderer.render(
            world = world,
            camera = camera,
            textureFor = textureFor,
            objectVisualFor = objectVisualFor,
            entityVisualFor = entityVisualFor,
            resolvedObjectVisualFor = resolvedObjectVisualFor,
            resolvedEntityVisualFor = effectiveEntityVisualFor,
            previews = visiblePreviews,
            entityPreviews = entityPreviews,
            animationTime = animationTime,
            maxTerrainSpriteHeight = maxTerrainSpriteHeight
        )

        if (debugGridConfig.enabled) {
            gridRenderer.render(
                world = world,
                camera = camera,
                hoveredTile = when (debugGridConfig.extent) {
                    DebugGridExtent.WORLD -> hoveredTile
                    DebugGridExtent.VISIBLE -> hoveredGridPosition
                }
            )

            if (
                debugGridConfig.renderLayer ==
                DebugGridRenderLayer.BELOW_OBJECTS
            ) {
                worldRenderer.renderWorldOverlay(
                    camera = camera,
                    objectVisualFor = objectVisualFor,
                    entityVisualFor = entityVisualFor,
                    previews = visiblePreviews,
                    entityPreviews = entityPreviews,
                    animationTime = animationTime,
                    resolvedObjectVisualFor = resolvedObjectVisualFor,
                    resolvedEntityVisualFor = effectiveEntityVisualFor
                )
            }
        }

        val visualizationFilter = debugSettings?.visuals?.filter
            ?: DebugVisualizationFilter.ALL
        debugFilterContext.update(
            inspection = debugSettings?.worldState?.inspection,
            selectedTarget = debugSettings?.worldState?.pickingSelection?.lockedTarget,
            hoveredTarget = debugSettings?.worldState?.hoveredTarget,
            renderSnapshot = worldRenderer.debugSnapshot
        )

        worldDebugRenderer.render(
            world = world,
            camera = camera,
            animationTime = animationTime,
            objectVisualFor = debugObjectVisualFor,
            entityVisualFor = debugEntityVisualFor,
            filter = visualizationFilter
        )

        debugSettings?.let { settings ->
            val state = settings.worldState
            val inspectionActive = state.inspectionHighlightVisible &&
                state.inspection != null
            val pathActive = settings.tools.pathfinding.enabled &&
                (state.pathfindingWaypoints.isNotEmpty() || state.pathfinding != null)
            val active = inspectionActive || pathActive ||
                settings.visuals.picking.hasActiveVisuals ||
                settings.visuals.renderOrder.hasActiveVisuals || settings.visuals.culling.hasActiveVisuals ||
                settings.visuals.camera.hasActiveVisuals || settings.visuals.worldInfo.hasActiveVisuals ||
                state.movePreview?.visible == true
                || state.brushPreview != null
            if (active) {
                advancedDebugRenderer.value.render(
                    camera = camera,
                    cameraSnapshot = cameraDebugSnapshot(),
                    renderSnapshot = worldRenderer.debugSnapshot,
                    world = world,
                    animationTime = animationTime,
                    terrainVisualFor = textureFor,
                    terrainIdFor = terrainIdFor
                )
            }
        }
    }

    fun resize(width: Int, height: Int) {
        viewportValid = width > 0 && height > 0
        if (!viewportValid) {
            clearHoverState()
            syncWorldInputAvailability()
            return
        }

        viewport.resize(width, height)
        cameraController.refreshZoomBounds()
        syncWorldInputAvailability()
    }

    private fun clearHoverState() {
        hoveredGridPosition = null
        hoveredTile = null
    }

    private fun syncWorldInputAvailability() {
        val enabled = requestedWorldInputEnabled && pickingAvailable
        if (worldInputProcessor.enabled != enabled) {
            worldInputProcessor.enabled = enabled
        }
    }

    internal fun setDebugCameraRestrictionsDisabled(disabled: Boolean) {
        viewport.boundsEnabled = !disabled
        cameraController.unrestricted = disabled
    }

    /** Returns the world tile at the given screen position, or null. */
    fun pickTile(screenX: Float, screenY: Float): TilePosition? {
        if (!pickingAvailable) return null
        return tilePicker.pick(screenX, screenY)
    }

    /** Returns the logical grid position at the given screen position. */
    fun pickGrid(screenX: Float, screenY: Float): TilePosition {
        check(pickingAvailable) {
            "Grid picking is unavailable while the viewport is invalid."
        }
        return tilePicker.pickGrid(screenX, screenY)
    }

    /**
     * Returns an object according to the selected picking mode.
     */
    fun pickObject(
        screenX: Float,
        screenY: Float,
        mode: ObjectPickingMode = ObjectPickingMode.SPRITE_ALPHA
    ): PlacedObject? {
        if (!pickingAvailable) return null
        return when (mode) {
            ObjectPickingMode.FOOTPRINT -> {
                val tile = tilePicker.pick(screenX, screenY)

                tile?.let { (x, y) ->
                    world.getObjectAt(x, y)
                }
            }

            ObjectPickingMode.SPRITE_ALPHA -> {
                objectPicker.pick(screenX, screenY)
            }

            ObjectPickingMode.SPRITE_OR_FOOTPRINT -> {
                objectPicker.pick(screenX, screenY)
                    ?: tilePicker.pick(screenX, screenY)?.let { (x, y) ->
                        world.getObjectAt(x, y)
                    }
            }

            ObjectPickingMode.NONE -> null
        }
    }

    /** Returns an entity according to the selected picking mode. */
    fun pickEntity(
        screenX: Float,
        screenY: Float,
        mode: EntityPickingMode = EntityPickingMode.SPRITE_ALPHA
    ): WorldEntity? {
        if (!pickingAvailable) return null
        return when (mode) {
            EntityPickingMode.SPRITE_ALPHA -> entityPicker.pick(screenX, screenY)
            EntityPickingMode.NONE -> null
        }
    }

    /**
     * Releases resources owned by this world view.
     */
    fun dispose() {
        try {
            if (advancedDebugRenderer.isInitialized()) {
                advancedDebugRenderer.value.dispose()
            }
        } finally {
            try {
                worldDebugRenderer.dispose()
            } finally {
                try {
                    gridRenderer.dispose()
                } finally {
                    worldRenderer.dispose()
                }
            }
        }
    }

    /** Runs the real object and entity pickers with pixel diagnostics. */
    fun pickingDebugSnapshot(
        screenX: Float,
        screenY: Float
    ): PickingDebugSnapshot {
        check(pickingAvailable) {
            "Picking diagnostics are unavailable while the viewport is invalid."
        }
        val objectResult = objectPicker.diagnose(screenX, screenY)
        val entityResult = entityPicker.diagnose(screenX, screenY)
        val picked = pickingTarget(
            frontmostPickedSprite(
                worldRenderer.currentRenderPlan(world),
                objectResult,
                entityResult
            ),
            tilePicker.pick(screenX, screenY)
        )
        return PickingDebugSnapshot(objectResult, entityResult, picked)
    }

    /** Projects screen coordinates into the renderer's world plane. */
    fun screenToWorld(screenX: Float, screenY: Float): Vector2 {
        check(pickingAvailable) {
            "Screen projection is unavailable while the viewport is invalid."
        }
        val point = camera.unproject(Vector3(screenX, screenY, 0f))
        return Vector2(point.x, point.y)
    }

    /** Projects a world-plane position into current screen coordinates. */
    internal fun worldToScreen(world: Vector2): Vector2 {
        val point = camera.project(Vector3(world.x, world.y, 0f))
        return Vector2(point.x, point.y)
    }

    internal fun tileCenterWorld(position: TilePosition): Vector2 =
        projection.tileToWorld(position.x + 0.5f, position.y + 0.5f)

    internal fun objectOriginWorld(placed: PlacedObject): Vector2 =
        projection.surfaceAnchor(placed.x, placed.y)

    internal fun entityWorld(entity: WorldEntity): Vector2 =
        projection.tileToWorld(entity.position.x, entity.position.y)

    /** Returns the active object sprite bounds, when a visual is registered. */
    fun objectSpriteBounds(placed: PlacedObject): Rectangle? {
        val visual = debugObjectVisualFor(placed, animationTime) ?: return null
        return IsoObjectBounds.calculate(
            projection = projection,
            placed = placed,
            visual = visual,
            result = Rectangle(),
            objectSettings = renderingConfig.objects
        )
    }

    /** Returns the active entity sprite bounds, when a visual is registered. */
    fun entitySpriteBounds(entity: WorldEntity): Rectangle? {
        val visual = debugEntityVisualFor(entity, animationTime) ?: return null
        return IsoEntityBounds.calculate(
            projection = projection,
            entity = entity,
            visual = visual,
            result = Rectangle()
        )
    }

    internal fun resolvedEntityVisual(entity: WorldEntity): ResolvedEntityVisual? =
        effectiveEntityVisualFor(entity, animationTime)

    /** Refreshes the live bounds for a previously picked object or entity. */
    internal fun refreshPickedTarget(
        target: PickedTarget
    ): PickedTarget = when (target) {
        is PickedTarget.Object -> target.copy(
            bounds = objectSpriteBounds(target.placedObject)
        )
        is PickedTarget.Entity -> target.copy(
            bounds = entitySpriteBounds(target.worldEntity)
        )
        is PickedTarget.Tile -> target
    }

    /** Current camera and authoritative renderer bounds for diagnostics. */
    fun cameraDebugSnapshot(): CameraDebugSnapshot {
        val width = camera.viewportWidth * camera.zoom
        val height = camera.viewportHeight * camera.zoom
        return CameraDebugSnapshot(
            x = camera.position.x,
            y = camera.position.y,
            zoom = camera.zoom,
            viewportWidth = camera.viewportWidth,
            viewportHeight = camera.viewportHeight,
            visibleArea = Rectangle(
                camera.position.x - width / 2f,
                camera.position.y - height / 2f,
                width,
                height
            ),
            worldBounds = Rectangle(worldBounds),
            clampBounds = Rectangle(
                cameraBounds.minX,
                cameraBounds.minY,
                cameraBounds.maxX - cameraBounds.minX,
                cameraBounds.maxY - cameraBounds.minY
            )
        )
    }
}

data class CameraDebugSnapshot(
    val x: Float,
    val y: Float,
    val zoom: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val visibleArea: Rectangle,
    val worldBounds: Rectangle,
    val clampBounds: Rectangle
)

data class PickingDebugSnapshot(
    val objectResult: SpritePickDiagnostic<PlacedObject>,
    val entityResult: SpritePickDiagnostic<WorldEntity>,
    val picked: PickedTarget? = null
)

sealed interface PickedTarget {
    val bounds: Rectangle?
    val alphaAccepted: Boolean?

    data class Object(
        val placedObject: PlacedObject,
        override val bounds: Rectangle?,
        override val alphaAccepted: Boolean? = null
    ) : PickedTarget

    data class Entity(
        val worldEntity: WorldEntity,
        override val bounds: Rectangle?,
        override val alphaAccepted: Boolean? = null
    ) : PickedTarget

    data class Tile(val position: TilePosition) : PickedTarget {
        override val bounds: Rectangle? = null
        override val alphaAccepted: Boolean? = null
    }
}

internal fun pickingTarget(
    spriteTarget: PickedTarget?,
    tile: TilePosition?
): PickedTarget? = spriteTarget ?: tile?.let(PickedTarget::Tile)

internal fun frontmostPickedSprite(
    renderPlan: List<WorldRenderPrimitive>,
    objectResult: SpritePickDiagnostic<PlacedObject>,
    entityResult: SpritePickDiagnostic<WorldEntity>
): PickedTarget? {
    val pickedObject = objectResult.picked
    val pickedEntity = entityResult.picked
    if (pickedObject == null && pickedEntity == null) return null
    for (primitive in renderPlan.asReversed()) {
        when {
            primitive is WorldObjectPrimitive &&
                primitive.placedObject === pickedObject -> {
                return PickedTarget.Object(
                    primitive.placedObject,
                    objectResult.pickedBounds,
                    objectResult.pickedAlphaAccepted
                )
            }
            primitive is WorldEntityPrimitive &&
                primitive.worldEntity === pickedEntity -> {
                return PickedTarget.Entity(
                    primitive.worldEntity,
                    entityResult.pickedBounds,
                    entityResult.pickedAlphaAccepted
                )
            }
        }
    }
    return null
}
