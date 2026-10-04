package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align

/** High-level layout alignment without Scene2D constants. */
enum class StrataAlignment(internal val scene2dValue: Int) {
    CENTER(Align.center),
    LEFT(Align.left),
    RIGHT(Align.right),
    TOP(Align.top),
    BOTTOM(Align.bottom),
    TOP_LEFT(Align.topLeft),
    TOP_RIGHT(Align.topRight),
    BOTTOM_LEFT(Align.bottomLeft),
    BOTTOM_RIGHT(Align.bottomRight)
}

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
    val selectableButtonStyle: String = toggleButtonStyle,
    val imageButtonStyle: String = buttonStyle,
    val selectableImageButtonStyle: String = imageButtonStyle,
    val dropdownStyle: String = "default",
    val panelStyle: String? = null,
    val separatorStyle: String? = null,
    val spacing: Float = 8f
) {

    companion object {
        /** Theme roles provided by Strata's engine-owned default screen skin. */
        fun default() = StrataUiTheme(
            toggleButtonStyle = "toggle",
            selectableButtonStyle = "toggle",
            panelStyle = "panel",
            separatorStyle = "separator"
        )
    }

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

        require(selectableButtonStyle.isNotBlank()) {
            "The selectable button style name must not be blank."
        }

        require(imageButtonStyle.isNotBlank()) {
            "The image button style name must not be blank."
        }

        require(selectableImageButtonStyle.isNotBlank()) {
            "The selectable image button style name must not be blank."
        }

        require(dropdownStyle.isNotBlank()) {
            "The dropdown style name must not be blank."
        }

        require(panelStyle == null || panelStyle.isNotBlank()) {
            "The panel style name must not be blank."
        }

        require(separatorStyle == null || separatorStyle.isNotBlank()) {
            "The separator style name must not be blank."
        }

        require(spacing.isFinite() && spacing >= 0f) {
            "UI spacing must be finite and non-negative."
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
        require(
            top.isFinite() && top >= 0f &&
                left.isFinite() && left >= 0f &&
                bottom.isFinite() && bottom >= 0f &&
                right.isFinite() && right >= 0f
        ) {
            "UI insets must be finite and non-negative."
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

/** Visual treatment for a [StrataExpander] header and its open content. */
class StrataExpanderStyle {
    /** Background shown behind the expander while its content is open. */
    var background: Drawable? = null
    var padTop: Float = 0f
    var padLeft: Float = 0f
    var padBottom: Float = 0f
    var padRight: Float = 0f

    /** Optional button style used by the clickable title area. */
    var headerButtonStyle: String? = null

    /** Header backgrounds for the closed and open states. */
    var headerBackground: Drawable? = null
    var expandedHeaderBackground: Drawable? = null

    /** Optional state-specific indicators placed before the title. */
    var collapsedIndicator: Drawable? = null
    var expandedIndicator: Drawable? = null
    var indicatorSize: Float = 10f
    var headerPadLeft: Float = 10f
    var headerPadRight: Float = 8f
    var indicatorSpacing: Float = 7f
    var headerActionSpacing: Float = 6f
    /** Left inset added to the content of each hierarchy level. */
    var contentIndent: Float = 0f
    /** Overrides the default spacing between content rows when present. */
    var contentSpacing: Float? = null
    /** Optional divider rendered directly below every header. */
    var headerSeparator: Drawable? = null
    var headerSeparatorThickness: Float = 1f

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
        require(thickness.isFinite() && thickness > 0f) {
            "Separator thickness must be finite and positive."
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
