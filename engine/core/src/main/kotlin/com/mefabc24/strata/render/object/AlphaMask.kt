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
            val width = pixmap.width
            val height = pixmap.height
            val alpha = ByteArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    alpha[y * width + x] =
                        (pixmap.getPixel(x, y) and 0xFF).toByte()
                }
            }

            return AlphaMask(width, height, alpha)
        }
    }
}