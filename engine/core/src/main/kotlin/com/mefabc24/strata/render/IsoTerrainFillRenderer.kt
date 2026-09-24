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

/** Draws full or internally clipped faces from one elevation-fill sprite. */
internal class IsoTerrainFillRenderer(
    private val projection: IsoProjection
) {
    private data class Slices(
        val left: TextureRegion,
        val right: TextureRegion,
        val leftRatio: Float
    )

    private val bounds = Rectangle()
    private val slicesByTexture = IdentityHashMap<TextureRegion, Slices>()

    fun bounds(
        x: Int,
        y: Int,
        elevation: Int,
        part: TerrainFillPart,
        texture: TextureRegion,
        result: Rectangle,
        offsetY: Float = 0f
    ): Rectangle {
        return IsoTerrainFillBounds.calculate(
            projection = projection,
            x = x,
            y = y,
            elevation = elevation,
            levelBelowSurface = part.levelBelowSurface,
            texture = texture,
            result = result,
            offsetY = offsetY
        )
    }

    fun render(
        batch: SpriteBatch,
        x: Int,
        y: Int,
        elevation: Int,
        part: TerrainFillPart,
        texture: TextureRegion,
        offsetY: Float = 0f
    ) {
        bounds(
            x = x,
            y = y,
            elevation = elevation,
            part = part,
            texture = texture,
            result = bounds,
            offsetY = offsetY
        )

        if (part.leftExposed && part.rightExposed) {
            batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height)
            return
        }

        val slices = slicesByTexture.getOrPut(texture) {
            createSlices(texture)
        }
        val leftWidth = bounds.width * slices.leftRatio

        if (part.leftExposed) {
            batch.draw(
                slices.left,
                bounds.x,
                bounds.y,
                leftWidth,
                bounds.height
            )
        }

        if (part.rightExposed) {
            batch.draw(
                slices.right,
                bounds.x + leftWidth,
                bounds.y,
                bounds.width - leftWidth,
                bounds.height
            )
        }
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
            ),
            leftRatio = layout.leftRatio
        )
    }
}
