package com.mefabc24.strata.render.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.render.`object`.IsoObjectBounds
import com.mefabc24.strata.render.`object`.ObjectRenderingSettings
import com.mefabc24.strata.render.`object`.ResolvedObjectVisual
import com.mefabc24.strata.render.entity.IsoEntityBounds
import com.mefabc24.strata.render.entity.ResolvedEntityVisual
import com.mefabc24.strata.debug.DebugEntitySettings
import com.mefabc24.strata.debug.DebugObjectSettings
import com.mefabc24.strata.world.EntityDirection
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

internal class IsoWorldDebugRenderer(
    private val projection: IsoProjection,
    private val objectSettings: DebugObjectSettings,
    private val entitySettings: DebugEntitySettings,
    private val objectRenderingSettings: ObjectRenderingSettings,
    private val shapes: ShapeRenderer = ShapeRenderer()
) {
    private val objectBounds = Rectangle()
    private val entityBounds = Rectangle()
    private var disposed = false

    fun render(
        world: World,
        camera: OrthographicCamera,
        animationTime: Float,
        objectVisualFor: (PlacedObject, Float) -> ResolvedObjectVisual?,
        entityVisualFor: (WorldEntity, Float) -> ResolvedEntityVisual?
    ) {
        val drawObjects = objectSettings.enabled && (
            objectSettings.showOccupiedTiles ||
                objectSettings.showOriginTile ||
                objectSettings.showSpriteBounds
            )
        val drawEntities = entitySettings.enabled && (
            entitySettings.showCurrentTile ||
                entitySettings.showPosition ||
                entitySettings.showPath ||
                entitySettings.showDirection ||
                entitySettings.showSpriteBounds
            )
        if (!drawObjects && !drawEntities) return

        shapes.projectionMatrix = camera.combined
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        drawFills(world, camera, drawObjects, drawEntities)
        drawLines(
            world = world,
            camera = camera,
            animationTime = animationTime,
            drawObjects = drawObjects,
            drawEntities = drawEntities,
            objectVisualFor = objectVisualFor,
            entityVisualFor = entityVisualFor
        )

        Gdx.gl.glLineWidth(1f)
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun drawFills(
        world: World,
        camera: OrthographicCamera,
        drawObjects: Boolean,
        drawEntities: Boolean
    ) {
        val objectFill = objectSettings.occupiedTileFillColor
        val entityFill = entitySettings.currentTileFillColor
        val hasObjectFill = drawObjects &&
            objectSettings.showOccupiedTiles && objectFill != null
        val hasEntityFill = drawEntities &&
            entitySettings.showCurrentTile && entityFill != null
        val hasPositionMarkers = drawEntities && entitySettings.showPosition
        if (!hasObjectFill && !hasEntityFill && !hasPositionMarkers) return

        shapes.begin(ShapeRenderer.ShapeType.Filled)

        if (hasObjectFill) {
            shapes.color = objectFill
            for (placed in world.getObjects()) {
                for (tile in placed.occupiedTiles()) {
                    drawTileFill(tile)
                }
            }
        }

        if (hasEntityFill) {
            shapes.color = entityFill
            for (entity in world.getEntities()) {
                drawTileFill(entity.currentTile)
            }
        }

        if (hasPositionMarkers) {
            shapes.color = entitySettings.positionColor
            val radius = POSITION_MARKER_RADIUS * camera.zoom
            for (entity in world.getEntities()) {
                val point = projection.tileToWorld(
                    entity.position.x,
                    entity.position.y
                )
                shapes.circle(point.x, point.y, radius, MARKER_SEGMENTS)
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
        entityVisualFor: (WorldEntity, Float) -> ResolvedEntityVisual?
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Line)

        if (drawObjects) {
            Gdx.gl.glLineWidth(
                (objectSettings.lineWidth / camera.zoom).coerceAtLeast(1f)
            )
            drawObjectLines(world, animationTime, objectVisualFor)
        }

        if (drawEntities) {
            Gdx.gl.glLineWidth(
                (entitySettings.lineWidth / camera.zoom).coerceAtLeast(1f)
            )
            drawEntityLines(world, camera, animationTime, entityVisualFor)
        }

        shapes.end()
    }

    private fun drawObjectLines(
        world: World,
        animationTime: Float,
        visualFor: (PlacedObject, Float) -> ResolvedObjectVisual?
    ) {
        if (objectSettings.showOccupiedTiles) {
            shapes.color = objectSettings.occupiedTileColor
            for (placed in world.getObjects()) {
                for (tile in placed.occupiedTiles()) {
                    drawTileOutline(tile)
                }
            }
        }

        if (objectSettings.showOriginTile) {
            shapes.color = objectSettings.originTileColor
            for (placed in world.getObjects()) {
                drawTileOutline(objectDebugOrigin(placed))
            }
        }

        if (objectSettings.showSpriteBounds) {
            shapes.color = objectSettings.spriteBoundsColor
            for (placed in world.getObjects()) {
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
        visualFor: (WorldEntity, Float) -> ResolvedEntityVisual?
    ) {
        if (entitySettings.showCurrentTile) {
            shapes.color = entitySettings.currentTileColor
            for (entity in world.getEntities()) {
                drawTileOutline(entity.currentTile)
            }
        }

        if (entitySettings.showPath) {
            shapes.color = entitySettings.pathColor
            val markerRadius = WAYPOINT_MARKER_RADIUS * camera.zoom
            for (entity in world.getEntities()) {
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
                val target = entityDebugDirectionTarget(entity)
                val start = projection.tileToWorld(
                    entity.position.x,
                    entity.position.y
                )
                val end = projection.tileToWorld(target.x, target.y)
                shapes.line(start.x, start.y, end.x, end.y)
            }
        }

        if (entitySettings.showSpriteBounds) {
            shapes.color = entitySettings.spriteBoundsColor
            for (entity in world.getEntities()) {
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

    fun dispose() {
        if (disposed) return
        disposed = true
        shapes.dispose()
    }

    private companion object {
        const val POSITION_MARKER_RADIUS = 3f
        const val WAYPOINT_MARKER_RADIUS = 2f
        const val MARKER_SEGMENTS = 12
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

internal fun entityDebugDirectionTarget(
    entity: WorldEntity
): EntityPosition {
    val (dx, dy) = when (entity.direction) {
        EntityDirection.NORTH_EAST -> 0f to -DIRECTION_LENGTH
        EntityDirection.SOUTH_EAST -> DIRECTION_LENGTH to 0f
        EntityDirection.SOUTH_WEST -> 0f to DIRECTION_LENGTH
        EntityDirection.NORTH_WEST -> -DIRECTION_LENGTH to 0f
    }
    return EntityPosition(
        x = entity.position.x + dx,
        y = entity.position.y + dy
    )
}

private const val DIRECTION_LENGTH = 0.45f
