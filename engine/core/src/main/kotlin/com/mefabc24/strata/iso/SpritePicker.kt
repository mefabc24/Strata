package com.mefabc24.strata.iso

import com.badlogic.gdx.math.Rectangle
import com.mefabc24.strata.render.`object`.AlphaMask

/**
 * Finds the frontmost item whose sprite bounds and alpha mask contain the
 * supplied world position.
 */
internal fun <T, V> pickFrontmost(
    items: List<T>,
    worldX: Float,
    worldY: Float,
    bounds: Rectangle,
    visualFor: (T) -> V?,
    calculateBounds: (T, V, Rectangle) -> Unit,
    alphaMaskFor: (V) -> AlphaMask?
): T? {
    for (item in items.asReversed()) {
        val visual = visualFor(item) ?: continue

        calculateBounds(item, visual, bounds)

        if (!bounds.contains(worldX, worldY)) {
            continue
        }

        val u = (worldX - bounds.x) / bounds.width
        val v = (worldY - bounds.y) / bounds.height
        val alphaMask = alphaMaskFor(visual)

        if (alphaMask == null || alphaMask.isSolid(u, v)) {
            return item
        }
    }

    return null
}