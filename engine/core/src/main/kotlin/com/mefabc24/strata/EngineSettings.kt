package com.mefabc24.strata

import com.badlogic.gdx.graphics.Color

/**
 * Configures process-wide Strata rendering behavior.
 *
 * Settings are captured when [StrataEngine] is created. World, camera, and
 * scene rendering options belong to their respective scene settings.
 */
class EngineSettings {
    var backgroundColor: Color = Color(0.1f, 0.1f, 0.1f, 1f)
        set(value) {
            validateColor(value)
            field = value.cpy()
        }

    internal fun backgroundColorSnapshot(): Color {
        validateColor(backgroundColor)
        return backgroundColor.cpy()
    }

    private fun validateColor(color: Color) {
        require(
            color.r.isFinite() &&
                color.g.isFinite() &&
                color.b.isFinite() &&
                color.a.isFinite()
        ) {
            "Engine background color components must be finite."
        }
    }
}
