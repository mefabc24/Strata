package com.mefabc24.strata.render

import com.mefabc24.strata.scene.DebugGridSettings
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

class IsoGridRenderer(
    private val projection: IsoProjection,
    settings: DebugGridSettings
) {
    private val settings = settings.copy()

    private val shapes = ShapeRenderer()

    fun render(
        world: World,
        camera: OrthographicCamera,
        hoveredTile: TilePosition? = null,
        selectedTile: TilePosition? = null
    ) {
        shapes.projectionMatrix = camera.combined

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        )

        settings.backgroundColor?.let { color ->
            shapes.begin(ShapeRenderer.ShapeType.Filled)
            shapes.setColor(color)

            for (y in 0 until world.height) {
                for (x in 0 until world.width) {
                    val position = projection.tileToWorld(x, y)

                    drawFilledTile(
                        x = position.x,
                        y = position.y
                    )
                }
            }

            shapes.end()
        }

        Gdx.gl.glLineWidth(settings.lineWidth)

        shapes.begin(ShapeRenderer.ShapeType.Line)

        // Draw the regular grid first.
        shapes.setColor(settings.color)

        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                val position = projection.tileToWorld(x, y)

                drawTile(
                    x = position.x,
                    y = position.y
                )
            }
        }

        // Draw selected tile again so neighboring cells cannot cover its edges.
        selectedTile?.let { tile ->
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
        hoveredTile?.let { tile ->
            shapes.setColor(settings.hoverColor)

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

    private fun drawFilledTile(
        x: Float,
        y: Float
    ) {
        val halfWidth = projection.tileWidth / 2f
        val halfHeight = projection.tileHeight / 2f

        val rightX = x + halfWidth
        val middleY = y - halfHeight
        val bottomY = y - projection.tileHeight
        val leftX = x - halfWidth

        shapes.triangle(
            x, y,
            rightX, middleY,
            x, bottomY
        )

        shapes.triangle(
            x, y,
            x, bottomY,
            leftX, middleY
        )
    }

    private fun drawTile(
        x: Float,
        y: Float
    ) {
        val halfWidth = projection.tileWidth / 2f
        val halfHeight = projection.tileHeight / 2f

        shapes.line(
            x,
            y,
            x + halfWidth,
            y - halfHeight
        )

        shapes.line(
            x + halfWidth,
            y - halfHeight,
            x,
            y - projection.tileHeight
        )

        shapes.line(
            x,
            y - projection.tileHeight,
            x - halfWidth,
            y - halfHeight
        )

        shapes.line(
            x - halfWidth,
            y - halfHeight,
            x,
            y
        )
    }

    fun dispose() {
        shapes.dispose()
    }
}