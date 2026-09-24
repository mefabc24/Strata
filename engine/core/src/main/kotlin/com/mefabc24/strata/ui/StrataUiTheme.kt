package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.utils.Drawable

/**
 * Maps Strata's semantic UI roles to styles in a game-owned Scene2D skin.
 *
 * The theme stores names and layout defaults only. It does not own fonts,
 * drawables, or any other resources from the skin.
 */
data class StrataUiTheme(
    val labelStyle: String = "default",
    val buttonStyle: String = "default",
    val toggleButtonStyle: String = buttonStyle,
    val panelStyle: String? = null,
    val separatorStyle: String? = null,
    val spacing: Float = 8f
) {

    init {
        require(labelStyle.isNotBlank()) {
            "The label style name must not be blank."
        }

        require(buttonStyle.isNotBlank()) {
            "The button style name must not be blank."
        }

        require(toggleButtonStyle.isNotBlank()) {
            "The toggle button style name must not be blank."
        }

        require(panelStyle == null || panelStyle.isNotBlank()) {
            "The panel style name must not be blank."
        }

        require(separatorStyle == null || separatorStyle.isNotBlank()) {
            "The separator style name must not be blank."
        }

        require(spacing >= 0f) {
            "UI spacing must be non-negative."
        }
    }
}

/**
 * Padding values in Scene2D's top, left, bottom, right order.
 */
data class StrataInsets(
    val top: Float,
    val left: Float,
    val bottom: Float,
    val right: Float
) {

    init {
        require(top >= 0f && left >= 0f && bottom >= 0f && right >= 0f) {
            "UI insets must be non-negative."
        }
    }

    companion object {
        val NONE = StrataInsets(0f, 0f, 0f, 0f)

        fun all(value: Float) = StrataInsets(
            top = value,
            left = value,
            bottom = value,
            right = value
        )

        fun symmetric(
            horizontal: Float,
            vertical: Float
        ) = StrataInsets(
            top = vertical,
            left = horizontal,
            bottom = vertical,
            right = horizontal
        )
    }
}

/**
 * Skin-managed visual and padding configuration for [StrataPanel].
 */
class StrataPanelStyle {
    var background: Drawable? = null
    var padTop: Float = 0f
    var padLeft: Float = 0f
    var padBottom: Float = 0f
    var padRight: Float = 0f

    constructor()

    constructor(
        background: Drawable?,
        padding: StrataInsets = StrataInsets.NONE
    ) {
        this.background = background
        padTop = padding.top
        padLeft = padding.left
        padBottom = padding.bottom
        padRight = padding.right
    }

    constructor(other: StrataPanelStyle) {
        background = other.background
        padTop = other.padTop
        padLeft = other.padLeft
        padBottom = other.padBottom
        padRight = other.padRight
    }
}

/**
 * Skin-managed drawable and thickness for [StrataSeparator].
 */
class StrataSeparatorStyle {
    var drawable: Drawable? = null
    var thickness: Float = 1f

    constructor()

    constructor(
        drawable: Drawable,
        thickness: Float = 1f
    ) {
        require(thickness > 0f) {
            "Separator thickness must be positive."
        }

        this.drawable = drawable
        this.thickness = thickness
    }

    constructor(other: StrataSeparatorStyle) {
        drawable = other.drawable
        thickness = other.thickness
    }
}

/**
 * Direction in which a separator divides adjacent content.
 */
enum class StrataSeparatorOrientation {
    HORIZONTAL,
    VERTICAL
}
