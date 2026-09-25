package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import java.util.IdentityHashMap
import kotlin.math.roundToInt

internal data class TerrainTopSliceLayout(
    val sourceY: Int,
    val sourceHeight: Int
)

/** Derives the top-face texture rows from logical geometry. */
internal object TerrainTopSlices {
    fun calculate(
        textureWidth: Int,
        textureHeight: Int,
        tileWidth: Float,
        faceHeight: Float,
        logicalHeight: Float
    ): TerrainTopSliceLayout {
        require(textureWidth > 0 && textureHeight > 0) {
            "Terrain texture dimensions must be positive."
        }

        val worldUnitsPerPixel = tileWidth / textureWidth
        val logicalPixelHeight =
            (logicalHeight / worldUnitsPerPixel).roundToInt()
        val facePixelHeight =
            (faceHeight / worldUnitsPerPixel).roundToInt()
        val sourceY = textureHeight - logicalPixelHeight

        require(
            sourceY >= 0 &&
                    facePixelHeight > 0 &&
                    sourceY + facePixelHeight <= textureHeight
        ) {
            "Terrain texture cannot represent the configured logical top face."
        }

        return TerrainTopSliceLayout(
            sourceY = sourceY,
            sourceHeight = facePixelHeight
        )
    }
}

/**
 * Draws only the logical diamond-shaped top portion of a terrain sprite.
 * Vertical terrain material is rendered separately through fill sprites.
 */
class IsoTerrainRenderer(
    private val projection: IsoProjection
) {
    private val bounds = Rectangle()
    private val topRegions = IdentityHashMap<TextureRegion, TextureRegion>()

    fun render(
        batch: SpriteBatch,
        x: Int,
        y: Int,
        texture: TextureRegion,
        offsetY: Float = 0f,
        elevation: Int = 0
    ) {
        IsoTerrainTopBounds.calculate(
            projection = projection,
            x = x,
            y = y,
            elevation = elevation,
            result = bounds,
            offsetY = offsetY
        )

        batch.draw(
            topRegion(texture),
            bounds.x,
            bounds.y,
            bounds.width,
            bounds.height
        )
    }

    private fun topRegion(texture: TextureRegion): TextureRegion {
        return topRegions.getOrPut(texture) {
            val layout = TerrainTopSlices.calculate(
                textureWidth = texture.regionWidth,
                textureHeight = texture.regionHeight,
                tileWidth = projection.tileWidth,
                faceHeight = projection.tileHeight,
                logicalHeight = projection.logicalTileHeight
            )

            TextureRegion(
                texture,
                0,
                layout.sourceY,
                texture.regionWidth,
                layout.sourceHeight
            )
        }
    }
}
