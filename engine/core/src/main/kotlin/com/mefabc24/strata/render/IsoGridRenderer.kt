package com.mefabc24.strata.render

import com.mefabc24.strata.scene.DebugGridSettings
import com.badlogic.gdx.Gdx
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

        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                val position = projection.tileToWorld(x, y)
                val coordinates = TilePosition(x, y)

                drawTile(
                    x = position.x,
                    y = position.y,
                    isHovered = hoveredTile == coordinates,
                    isSelected = selectedTile == coordinates
                )
            }
        }

        shapes.end()

        Gdx.gl.glLineWidth(1f)
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
        y: Float,
        isHovered: Boolean,
        isSelected: Boolean
    ) {
        val halfWidth = projection.tileWidth / 2f
        val halfHeight = projection.tileHeight / 2f

        when {
            isHovered -> shapes.setColor(settings.hoverColor)
            isSelected -> shapes.setColor(0.3f, 0.6f, 1f, 1f)
            else -> shapes.setColor(settings.color)
        }

        shapes.line(x, y, x + halfWidth, y - halfHeight)
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