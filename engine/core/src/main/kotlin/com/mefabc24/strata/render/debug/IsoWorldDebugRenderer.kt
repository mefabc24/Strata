package com.mefabc24.strata.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.render.`object`.IsoObjectBounds
import com.mefabc24.strata.render.`object`.ObjectRenderingSettings
import com.mefabc24.strata.render.`object`.ResolvedObjectVisual
import com.mefabc24.strata.render.entity.IsoEntityBounds
import com.mefabc24.strata.render.entity.ResolvedEntityVisual
import com.mefabc24.strata.debug.DebugEntitySettings
import com.mefabc24.strata.debug.DebugEntityTrailRecorder
import com.mefabc24.strata.debug.DebugObjectSettings
import com.mefabc24.strata.debug.DebugVisualizationFilter
import com.mefabc24.strata.debug.DebugVisualizationFilterContext
import com.mefabc24.strata.debug.hasActiveVisuals
import com.mefabc24.strata.world.EntityDirection
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity
import kotlin.math.sqrt
import kotlin.math.floor
import java.util.Locale

internal class IsoWorldDebugRenderer(
    private val projection: IsoProjection,
    private val objectSettings: DebugObjectSettings,
    private val entitySettings: DebugEntitySettings,
    private val objectRenderingSettings: ObjectRenderingSettings,
    private val filterContext: DebugVisualizationFilterContext =
        DebugVisualizationFilterContext(),
    private val trailRecorder: DebugEntityTrailRecorder? = null,
    private val shapes: ShapeRenderer = ShapeRenderer()
) {
    private val objectBounds = Rectangle()
    private val entityBounds = Rectangle()
    private val pointA = Vector2()
    private val pointB = Vector2()
    private val speedLabels = lazy { EntitySpeedLabelRenderer(projection) }
    private var disposed = false

    fun render(
        world: World,
        camera: OrthographicCamera,
        animationTime: Float,
        objectVisualFor: (PlacedObject, Float) -> ResolvedObjectVisual?,
        entityVisualFor: (WorldEntity, Float) -> ResolvedEntityVisual?,
        filter: DebugVisualizationFilter = DebugVisualizationFilter.ALL
    ) {
        val drawObjects = objectSettings.hasActiveVisuals
        val drawEntityShapes = (
            entitySettings.showCurrentTile ||
                entitySettings.showPosition ||
                entitySettings.showPath ||
                entitySettings.showDirection ||
                entitySettings.showSpriteBounds ||
                entitySettings.showMovementTrail ||
                entitySettings.showMovementVector ||
                entitySettings.showNextWaypoint ||
                entitySettings.showPositionTileOffset ||
                entitySettings.showCurrentTileFill
            )
        val drawEntityLabels = entitySettings.showMovementSpeed
        if (!drawObjects && !drawEntityShapes && !drawEntityLabels) return

        if (drawObjects || drawEntityShapes) {
            shapes.projectionMatrix = camera.combined
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

            drawFills(world, camera, drawObjects, drawEntityShapes, filter)
            drawLines(
                world = world,
                camera = camera,
                animationTime = animationTime,
                drawObjects = drawObjects,
                drawEntities = drawEntityShapes,
                objectVisualFor = objectVisualFor,
                entityVisualFor = entityVisualFor,
                filter = filter
            )

            Gdx.gl.glLineWidth(1f)
            Gdx.gl.glDisable(GL20.GL_BLEND)
        }
        if (drawEntityLabels) {
            speedLabels.value.render(world, camera, filter, filterContext)
        }
    }

    private fun drawFills(
        world: World,
        camera: OrthographicCamera,
        drawObjects: Boolean,
        drawEntities: Boolean,
        filter: DebugVisualizationFilter
    ) {
        val objectFill = objectSettings.occupiedTileFillColor
        val entityFill = entitySettings.currentTileFillColor
        val hasObjectFill = drawObjects && objectSettings.showOccupiedTileFill
        val hasEntityFill = drawEntities && entitySettings.showCurrentTileFill
        val hasPositionMarkers = drawEntities && entitySettings.showPosition
        val hasNextWaypointMarkers = drawEntities && entitySettings.showNextWaypoint
        if (!hasObjectFill && !hasEntityFill && !hasPositionMarkers &&
            !hasNextWaypointMarkers
        ) return

        shapes.begin(ShapeRenderer.ShapeType.Filled)

        if (hasObjectFill) {
            shapes.color = objectFill
            for (placed in world.getObjects()) {
                if (!filterContext.matches(filter, placed)) continue
                for (tile in placed.occupiedTiles()) {
                    drawTileFill(tile)
                }
            }
        }

        if (hasEntityFill) {
            shapes.color = entityFill
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                drawTileFill(entity.currentTile)
            }
        }

        if (hasPositionMarkers) {
            shapes.color = entitySettings.positionColor
            val radius = POSITION_MARKER_RADIUS * camera.zoom
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                val point = projection.tileToWorld(
                    entity.position.x,
                    entity.position.y
                )
                shapes.circle(point.x, point.y, radius, MARKER_SEGMENTS)
            }
        }

        if (hasNextWaypointMarkers) {
            shapes.color = entitySettings.nextWaypointColor
            val radius = NEXT_WAYPOINT_MARKER_RADIUS * camera.zoom
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                val waypoint = entity.nextWaypoint ?: continue
                projection.tileToWorld(waypoint.x, waypoint.y, pointA)
                shapes.circle(pointA.x, pointA.y, radius, MARKER_SEGMENTS)
            }
        }

        shapes.end()
    }

    private fun drawLines(
        world: World,
        camera: OrthographicCamera,
        animationTime: Float,
        drawObjects: Boolean,
        drawEntities: Boolean,
        objectVisualFor: (PlacedObject, Float) -> ResolvedObjectVisual?,
        entityVisualFor: (WorldEntity, Float) -> ResolvedEntityVisual?,
        filter: DebugVisualizationFilter
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Line)

        if (drawObjects) {
            Gdx.gl.glLineWidth(
                (objectSettings.lineWidth / camera.zoom).coerceAtLeast(1f)
            )
            drawObjectLines(world, animationTime, objectVisualFor, filter)
        }

        if (drawEntities) {
            Gdx.gl.glLineWidth(
                (entitySettings.lineWidth / camera.zoom).coerceAtLeast(1f)
            )
            drawEntityLines(world, camera, animationTime, entityVisualFor, filter)
        }

        shapes.end()
    }

    private fun drawObjectLines(
        world: World,
        animationTime: Float,
        visualFor: (PlacedObject, Float) -> ResolvedObjectVisual?,
        filter: DebugVisualizationFilter
    ) {
        if (objectSettings.showOccupiedTiles) {
            shapes.color = objectSettings.occupiedTileColor
            for (placed in world.getObjects()) {
                if (!filterContext.matches(filter, placed)) continue
                for (tile in placed.occupiedTiles()) {
                    drawTileOutline(tile)
                }
            }
        }

        if (objectSettings.showOriginTile) {
            shapes.color = objectSettings.originTileColor
            for (placed in world.getObjects()) {
                if (!filterContext.matches(filter, placed)) continue
                drawTileOutline(objectDebugOrigin(placed))
            }
        }

        if (objectSettings.showSpriteBounds) {
            shapes.color = objectSettings.spriteBoundsColor
            for (placed in world.getObjects()) {
                if (!filterContext.matches(filter, placed)) continue
                val visual = visualFor(placed, animationTime) ?: continue
                IsoObjectBounds.calculate(
                    projection = projection,
                    placed = placed,
                    visual = visual,
                    result = objectBounds,
                    objectSettings = objectRenderingSettings
                )
                shapes.rect(
                    objectBounds.x,
                    objectBounds.y,
                    objectBounds.width,
                    objectBounds.height
                )
            }
        }
    }

    private fun drawEntityLines(
        world: World,
        camera: OrthographicCamera,
        animationTime: Float,
        visualFor: (WorldEntity, Float) -> ResolvedEntityVisual?,
        filter: DebugVisualizationFilter
    ) {
        if (entitySettings.showMovementTrail) {
            val color = entitySettings.trailColor
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                val trail = trailRecorder?.trail(entity) ?: continue
                val segmentCount = trail.size - 1
                for (index in 0 until segmentCount) {
                    projection.tileToWorld(trail.xAt(index), trail.yAt(index), pointA)
                    projection.tileToWorld(
                        trail.xAt(index + 1),
                        trail.yAt(index + 1),
                        pointB
                    )
                    color.a = entitySettings.trailOpacity *
                        (index + 1).toFloat() / segmentCount
                    shapes.color = color
                    shapes.line(pointA, pointB)
                }
            }
        }

        if (entitySettings.showCurrentTile) {
            shapes.color = entitySettings.currentTileColor
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                drawTileOutline(entity.currentTile)
            }
        }

        if (entitySettings.showPath) {
            shapes.color = entitySettings.pathColor
            val markerRadius = WAYPOINT_MARKER_RADIUS * camera.zoom
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                forEachEntityDebugPathSegment(entity) { from, to ->
                    val fromWorld = projection.tileToWorld(from.x, from.y)
                    val toWorld = projection.tileToWorld(to.x, to.y)
                    shapes.line(fromWorld.x, fromWorld.y, toWorld.x, toWorld.y)
                    shapes.circle(
                        toWorld.x,
                        toWorld.y,
                        markerRadius,
                        MARKER_SEGMENTS
                    )
                }
            }
        }

        if (entitySettings.showDirection) {
            shapes.color = entitySettings.directionColor
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                val target = entityDebugDirectionTarget(entity)
                val start = projection.tileToWorld(
                    entity.position.x,
                    entity.position.y
                )
                val end = projection.tileToWorld(target.x, target.y)
                shapes.line(start.x, start.y, end.x, end.y)
            }
        }

        if (entitySettings.showMovementVector) {
            shapes.color = entitySettings.movementVectorColor
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                withEntityDebugMovementVector(entity) { velocityX, velocityY ->
                    val position = entity.position
                    projection.tileToWorld(position.x, position.y, pointA)
                    projection.tileToWorld(
                        position.x + velocityX * entitySettings.movementVectorScaleSeconds,
                        position.y + velocityY * entitySettings.movementVectorScaleSeconds,
                        pointB
                    )
                    drawArrow(pointA, pointB, camera.zoom)
                }
            }
        }

        if (entitySettings.showPositionTileOffset) {
            shapes.color = entitySettings.positionTileOffsetColor
            val radius = POSITION_OFFSET_MARKER_RADIUS * camera.zoom
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                val position = entity.position
                projection.tileToWorld(
                    floor(position.x) + 0.5f,
                    floor(position.y) + 0.5f,
                    pointA
                )
                projection.tileToWorld(position.x, position.y, pointB)
                shapes.line(pointA, pointB)
                shapes.circle(pointA.x, pointA.y, radius, MARKER_SEGMENTS)
                shapes.circle(
                    pointB.x,
                    pointB.y,
                    radius * 0.65f,
                    MARKER_SEGMENTS
                )
            }
        }

        if (entitySettings.showSpriteBounds) {
            shapes.color = entitySettings.spriteBoundsColor
            for (entity in world.getEntities()) {
                if (!filterContext.matches(filter, entity)) continue
                val visual = visualFor(entity, animationTime) ?: continue
                IsoEntityBounds.calculate(
                    projection = projection,
                    entity = entity,
                    visual = visual,
                    result = entityBounds
                )
                shapes.rect(
                    entityBounds.x,
                    entityBounds.y,
                    entityBounds.width,
                    entityBounds.height
                )
            }
        }
    }

    private fun drawTileFill(tile: TilePosition) {
        val top = projection.tileToWorld(tile.x, tile.y)
        shapes.drawIsoTileFill(projection, top.x, top.y)
    }

    private fun drawTileOutline(tile: TilePosition) {
        val top = projection.tileToWorld(tile.x, tile.y)
        shapes.drawIsoTileOutline(projection, top.x, top.y)
    }

    private fun drawArrow(start: Vector2, end: Vector2, cameraZoom: Float) {
        shapes.line(start, end)
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy)
        if (length <= 0f) return
        val arrowLength = MOVEMENT_ARROW_HEAD_LENGTH * cameraZoom
        val unitX = dx / length
        val unitY = dy / length
        val sideX = -unitY * arrowLength * 0.55f
        val sideY = unitX * arrowLength * 0.55f
        val baseX = end.x - unitX * arrowLength
        val baseY = end.y - unitY * arrowLength
        shapes.line(end.x, end.y, baseX + sideX, baseY + sideY)
        shapes.line(end.x, end.y, baseX - sideX, baseY - sideY)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        if (speedLabels.isInitialized()) speedLabels.value.dispose()
        shapes.dispose()
    }

    private companion object {
        const val POSITION_MARKER_RADIUS = 3f
        const val WAYPOINT_MARKER_RADIUS = 2f
        const val NEXT_WAYPOINT_MARKER_RADIUS = 4f
        const val POSITION_OFFSET_MARKER_RADIUS = 3f
        const val MOVEMENT_ARROW_HEAD_LENGTH = 7f
        const val MARKER_SEGMENTS = 12
    }
}

private class EntitySpeedLabelRenderer(
    private val projection: IsoProjection
) {
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val point = Vector2()

    fun render(
        world: World,
        camera: OrthographicCamera,
        filter: DebugVisualizationFilter,
        filterContext: DebugVisualizationFilterContext
    ) {
        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = Color.WHITE
        for (entity in world.getEntities()) {
            if (!filterContext.matches(filter, entity)) continue
            projection.tileToWorld(entity.position.x, entity.position.y, point)
            val speed = entity.movementSpeed ?: 0f
            font.draw(
                batch,
                String.format(Locale.ROOT, "%.2f tiles/s", speed),
                point.x,
                point.y + SPEED_LABEL_OFFSET * camera.zoom
            )
        }
        batch.end()
    }

    fun dispose() {
        batch.dispose()
        font.dispose()
    }

    private companion object {
        const val SPEED_LABEL_OFFSET = 14f
    }
}

internal fun objectDebugOrigin(placed: PlacedObject): TilePosition {
    return TilePosition(placed.x, placed.y)
}

internal inline fun forEachEntityDebugPathSegment(
    entity: WorldEntity,
    action: (from: EntityPosition, to: EntityPosition) -> Unit
) {
    var from = entity.position
    for (waypoint in entity.remainingWaypoints) {
        action(from, waypoint)
        from = waypoint
    }
}

internal inline fun withEntityDebugMovementVector(
    entity: WorldEntity,
    action: (velocityX: Float, velocityY: Float) -> Unit
): Boolean {
    val target = entity.nextWaypoint ?: return false
    val speed = entity.movementSpeed ?: return false
    val dx = target.x - entity.position.x
    val dy = target.y - entity.position.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance <= 0f) return false
    action(dx / distance * speed, dy / distance * speed)
    return true
}

internal fun entityDebugDirectionTarget(
    entity: WorldEntity
): EntityPosition {
    val (dx, dy) = when (entity.direction) {
        EntityDirection.NORTH -> -DIRECTION_DIAGONAL_COMPONENT to -DIRECTION_DIAGONAL_COMPONENT
        EntityDirection.NORTH_EAST -> 0f to -DIRECTION_LENGTH
        EntityDirection.EAST -> DIRECTION_DIAGONAL_COMPONENT to -DIRECTION_DIAGONAL_COMPONENT
        EntityDirection.SOUTH_EAST -> DIRECTION_LENGTH to 0f
        EntityDirection.SOUTH -> DIRECTION_DIAGONAL_COMPONENT to DIRECTION_DIAGONAL_COMPONENT
        EntityDirection.SOUTH_WEST -> 0f to DIRECTION_LENGTH
        EntityDirection.WEST -> -DIRECTION_DIAGONAL_COMPONENT to DIRECTION_DIAGONAL_COMPONENT
        EntityDirection.NORTH_WEST -> -DIRECTION_LENGTH to 0f
    }
    return EntityPosition(
        x = entity.position.x + dx,
        y = entity.position.y + dy
    )
}

private const val DIRECTION_LENGTH = 0.45f
private val DIRECTION_DIAGONAL_COMPONENT = DIRECTION_LENGTH / sqrt(2f)
