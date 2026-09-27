package com.mefabc24.strata.render.debug

import com.mefabc24.strata.scene.DebugGridSettings
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.scene.DebugGridExtent
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

internal data class DebugGridTileRange(
    val x: IntRange,
    val y: IntRange
) {
    operator fun contains(position: TilePosition): Boolean {
        return position.x in x && position.y in y
    }
}

internal fun visibleDebugGridTileRange(
    projection: IsoProjection,
    minWorldX: Float,
    minWorldY: Float,
    maxWorldX: Float,
    maxWorldY: Float,
    margin: Int = 1
): DebugGridTileRange {
    require(
        minWorldX.isFinite() && minWorldY.isFinite() &&
            maxWorldX.isFinite() && maxWorldY.isFinite()
    )
    require(minWorldX <= maxWorldX && minWorldY <= maxWorldY)
    require(margin >= 0)

    val corners = listOf(
        projection.worldToTile(minWorldX, minWorldY),
        projection.worldToTile(minWorldX, maxWorldY),
        projection.worldToTile(maxWorldX, minWorldY),
        projection.worldToTile(maxWorldX, maxWorldY)
    )

    return DebugGridTileRange(
        x = (corners.minOf { it.x } - margin)..
            (corners.maxOf { it.x } + margin),
        y = (corners.minOf { it.y } - margin)..
            (corners.maxOf { it.y } + margin)
    )
}

class IsoGridRenderer(
    private val projection: IsoProjection,
    private val settings: DebugGridSettings
) {
    private val shapes = ShapeRenderer()

    fun render(
        world: World,
        camera: OrthographicCamera,
        hoveredTile: TilePosition? = null,
        selectedTile: TilePosition? = null
    ) {
        if (!settings.enabled) return

        shapes.projectionMatrix = camera.combined
        val tileRange = tileRange(world, camera)

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        )

        settings.backgroundColor?.let { color ->
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.color = color

            forEachTile(tileRange) { x, y ->
                val position = projection.tileToWorld(x, y)

                drawFilledTile(
                    x = position.x,
                    y = position.y
                )
            }

            shapes.end()
        }

        if (
            hoveredTile != null &&
            hoveredTile in tileRange &&
            settings.hoverBackgroundColor != null
        ) {
            val position = projection.tileToWorld(
                hoveredTile.x,
                hoveredTile.y
            )

            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.setColor(settings.hoverBackgroundColor)

            drawFilledTile(
                x = position.x,
                y = position.y
            )

            shapes.end()
        }

        val effectiveLineWidth =
            (settings.lineWidth / camera.zoom)
                .coerceAtLeast(1f)

        Gdx.gl.glLineWidth(effectiveLineWidth)

        shapes.begin(ShapeRenderer.ShapeType.Line)

        // Draw the regular grid first.
        shapes.color = settings.color

        forEachTile(tileRange) { x, y ->
            val position = projection.tileToWorld(x, y)

            drawTile(
                x = position.x,
                y = position.y
            )
        }

        // Draw selected tile again so neighboring cells cannot cover its edges.
        selectedTile?.takeIf { it in tileRange }?.let { tile ->
            shapes.setColor(0.3f, 0.6f, 1f, 1f)

            val position = projection.tileToWorld(
                tile.x,
                tile.y
            )

            drawTile(
                x = position.x,
                y = position.y
            )
        }

        // Hover is drawn last and therefore always remains fully visible.
        hoveredTile?.takeIf { it in tileRange }?.let { tile ->
            shapes.color = settings.hoverColor

            val position = projection.tileToWorld(
                tile.x,
                tile.y
            )

            drawTile(
                x = position.x,
                y = position.y
            )
        }

        shapes.end()

        Gdx.gl.glLineWidth(1f)
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    private fun tileRange(
        world: World,
        camera: OrthographicCamera
    ): DebugGridTileRange {
        return when (settings.extent) {
            DebugGridExtent.WORLD -> DebugGridTileRange(
                x = 0 until world.width,
                y = 0 until world.height
            )

            DebugGridExtent.VISIBLE -> {
                val halfWidth = camera.viewportWidth * camera.zoom / 2f
                val halfHeight = camera.viewportHeight * camera.zoom / 2f

                visibleDebugGridTileRange(
                    projection = projection,
                    minWorldX = camera.position.x - halfWidth,
                    minWorldY = camera.position.y - halfHeight,
                    maxWorldX = camera.position.x + halfWidth,
                    maxWorldY = camera.position.y + halfHeight
                )
            }
        }
    }

    private inline fun forEachTile(
        range: DebugGridTileRange,
        action: (x: Int, y: Int) -> Unit
    ) {
        for (y in range.y) {
            for (x in range.x) {
                action(x, y)
            }
        }
    }

    private fun drawFilledTile(
        x: Float,
        y: Float
    ) {
        shapes.drawIsoTileFill(projection, x, y)
    }

    private fun drawTile(
        x: Float,
        y: Float
    ) {
        shapes.drawIsoTileOutline(projection, x, y)
    }

    fun dispose() {
        shapes.dispose()
    }
}
