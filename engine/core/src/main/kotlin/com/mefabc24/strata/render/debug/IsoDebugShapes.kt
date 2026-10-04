package com.mefabc24.strata.render.debug

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mefabc24.strata.iso.IsoProjection

internal fun ShapeRenderer.drawIsoTileOutline(
    projection: IsoProjection,
    topX: Float,
    topY: Float
) {
    val halfWidth = projection.tileWidth / 2f
    val halfHeight = projection.tileHeight / 2f

    line(topX, topY, topX + halfWidth, topY - halfHeight)
    line(topX + halfWidth, topY - halfHeight, topX, topY - projection.tileHeight)
    line(topX, topY - projection.tileHeight, topX - halfWidth, topY - halfHeight)
    line(topX - halfWidth, topY - halfHeight, topX, topY)
}

internal fun ShapeRenderer.drawIsoTileFill(
    projection: IsoProjection,
    topX: Float,
    topY: Float
) {
    val halfWidth = projection.tileWidth / 2f
    val halfHeight = projection.tileHeight / 2f
    val centerY = topY - halfHeight
    val bottomY = topY - projection.tileHeight

    triangle(topX, topY, topX + halfWidth, centerY, topX, bottomY)
    triangle(topX, topY, topX, bottomY, topX - halfWidth, centerY)
}
