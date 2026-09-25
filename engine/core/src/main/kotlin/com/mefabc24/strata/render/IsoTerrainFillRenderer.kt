package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.iso.IsoProjection
import java.util.IdentityHashMap

internal data class TerrainFillSliceLayout(
    val leftWidth: Int,
    val rightWidth: Int
) {
    val leftRatio: Float
        get() = leftWidth.toFloat() / (leftWidth + rightWidth)
}

internal object TerrainFillSlices {
    fun calculate(textureWidth: Int): TerrainFillSliceLayout {
        require(textureWidth >= 2) {
            "Terrain fill texture width must be at least 2 pixels."
        }

        val leftWidth = textureWidth / 2

        return TerrainFillSliceLayout(
            leftWidth = leftWidth,
            rightWidth = textureWidth - leftWidth
        )
    }
}

/** Draws one atomic face sliced from a shared elevation-fill sprite. */
internal class IsoTerrainFillRenderer(
    private val projection: IsoProjection
) {
    private data class Slices(
        val left: TextureRegion,
        val right: TextureRegion
    )

    private val bounds = Rectangle()
    private val slicesByTexture = IdentityHashMap<TextureRegion, Slices>()

    fun bounds(
        x: Int,
        y: Int,
        elevation: Int,
        levelBelowSurface: Int,
        face: TerrainFace,
        texture: TextureRegion,
        result: Rectangle,
        offsetY: Float = 0f
    ): Rectangle {
        IsoTerrainFillBounds.calculate(
            projection = projection,
            x = x,
            y = y,
            elevation = elevation,
            levelBelowSurface = levelBelowSurface,
            texture = texture,
            result = result,
            offsetY = offsetY
        )

        val layout = TerrainFillSlices.calculate(texture.regionWidth)
        val leftWidth = result.width * layout.leftRatio

        if (face == TerrainFace.RIGHT) {
            result.x += leftWidth
        }

        result.width = if (face == TerrainFace.LEFT) {
            leftWidth
        } else {
            result.width - leftWidth
        }

        return result
    }

    fun render(
        batch: SpriteBatch,
        x: Int,
        y: Int,
        elevation: Int,
        levelBelowSurface: Int,
        face: TerrainFace,
        texture: TextureRegion,
        offsetY: Float = 0f
    ) {
        bounds(
            x = x,
            y = y,
            elevation = elevation,
            levelBelowSurface = levelBelowSurface,
            face = face,
            texture = texture,
            result = bounds,
            offsetY = offsetY
        )

        val slices = slicesByTexture.getOrPut(texture) {
            createSlices(texture)
        }
        val region = if (face == TerrainFace.LEFT) {
            slices.left
        } else {
            slices.right
        }

        batch.draw(region, bounds.x, bounds.y, bounds.width, bounds.height)
    }

    private fun createSlices(texture: TextureRegion): Slices {
        require(texture.regionWidth >= 2 && texture.regionHeight > 0) {
            "Terrain fill texture dimensions must be at least 2 by 1 pixels."
        }

        val layout = TerrainFillSlices.calculate(texture.regionWidth)

        return Slices(
            left = TextureRegion(
                texture,
                0,
                0,
                layout.leftWidth,
                texture.regionHeight
            ),
            right = TextureRegion(
                texture,
                layout.leftWidth,
                0,
                layout.rightWidth,
                texture.regionHeight
            )
        )
    }
}
