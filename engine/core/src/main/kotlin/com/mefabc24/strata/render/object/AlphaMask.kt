package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.Pixmap

/**
 * Stores sprite alpha values for pixel-accurate picking.
 */
class AlphaMask private constructor(
    private val width: Int,
    private val height: Int,
    private val alpha: ByteArray
) {

    /**
     * Checks normalized sprite coordinates with the origin at the bottom-left.
     */
    fun isSolid(u: Float, v: Float): Boolean {
        if (u !in 0f..<1f || v < 0f || v >= 1f) {
            return false
        }

        val x = (u * width).toInt()
        val y = height - 1 - (v * height).toInt()

        val pixelAlpha = alpha[y * width + x].toInt() and 0xFF

        return pixelAlpha >= 16
    }

    companion object {

        /**
         * Copies alpha values from a Pixmap.
         */
        fun fromPixmap(pixmap: Pixmap): AlphaMask {
            return fromPixmap(
                pixmap = pixmap,
                x = 0,
                y = 0,
                width = pixmap.width,
                height = pixmap.height
            )
        }

        /** Copies alpha values from a rectangular Pixmap region. */
        fun fromPixmap(
            pixmap: Pixmap,
            x: Int,
            y: Int,
            width: Int,
            height: Int
        ): AlphaMask {
            require(
                x >= 0 && y >= 0 &&
                    width > 0 && height > 0 &&
                    x + width <= pixmap.width &&
                    y + height <= pixmap.height
            ) {
                "Alpha-mask region must be inside the Pixmap."
            }

            val alpha = ByteArray(width * height)

            for (localY in 0 until height) {
                for (localX in 0 until width) {
                    alpha[localY * width + localX] =
                        (pixmap.getPixel(x + localX, y + localY) and 0xFF).toByte()
                }
            }

            return AlphaMask(width, height, alpha)
        }
    }
}
