package com.mefabc24.strata.render

/**
 * Configures scene-wide visual adjustments for rendered objects.
 *
 * These offsets are added to each [ObjectVisual]'s asset-specific offsets
 * after the object has been anchored to the logical terrain surface.
 */
class ObjectRenderingSettings(
    offsetX: Float = 0f,
    offsetY: Float = 0f
) {

    var offsetX: Float = offsetX
        set(value) {
            require(value.isFinite()) {
                "Global object offset X must be finite."
            }

            field = value
        }

    var offsetY: Float = offsetY
        set(value) {
            require(value.isFinite()) {
                "Global object offset Y must be finite."
            }

            field = value
        }

    init {
        validate()
    }

    fun copy(): ObjectRenderingSettings {
        return ObjectRenderingSettings(
            offsetX = offsetX,
            offsetY = offsetY
        )
    }

    internal fun validate() {
        require(offsetX.isFinite()) {
            "Global object offset X must be finite."
        }

        require(offsetY.isFinite()) {
            "Global object offset Y must be finite."
        }
    }
}
