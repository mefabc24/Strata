package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.world.World

class IsoGridRenderer(
    private val projection: IsoProjection
) {
    private val shapes = ShapeRenderer()

    fun render(world: World, camera: OrthographicCamera) {
        shapes.projectionMatrix = camera.combined

        shapes.begin(ShapeRenderer.ShapeType.Line)

        for (y in 0 until world.height) {
            for (x in 0 until world.width) {
                val position = projection.tileToWorld(x, y)

                drawTile(position.x, position.y)
            }
        }
        shapes.end()
    }

    private fun drawTile(x: Float, y: Float) {
        val halfWidth = projection.tileWidth / 2f
        val halfHeight = projection.tileHeight / 2f

        shapes.setColor(0.4f, 0.8f, 0.5f, 1f)

        shapes.line(x, y, x + halfWidth, y - halfHeight)
        shapes.line(x + halfWidth, y - halfHeight, x, y - projection.tileHeight)
        shapes.line(x, y - projection.tileHeight, x - halfWidth, y - halfHeight)
        shapes.line(x - halfWidth, y - halfHeight, x, y)
    }

    fun dispose() {
        shapes.dispose()
    }
}