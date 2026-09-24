package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align

internal class StrataUiContext(
    val skin: Skin,
    val theme: StrataUiTheme
) {
    private val selectionButtons = mutableListOf<StrataSelectableButton<*>>()
    private val subscriptions = mutableListOf<StrataSelectionSubscription>()

    fun own(button: StrataSelectableButton<*>) {
        selectionButtons += button
    }

    fun own(subscription: StrataSelectionSubscription) {
        subscriptions += subscription
    }

    fun dispose() {
        for (button in selectionButtons) {
            button.detach()
        }

        for (subscription in subscriptions) {
            subscription.dispose()
        }

        selectionButtons.clear()
        subscriptions.clear()
    }
}

/**
 * Base class for Strata's small Scene2D table layout DSL.
 *
 * Layouts are regular [Table] actors. Add custom Scene2D actors with [actor]
 * and use [cell] to configure their underlying Scene2D cells directly.
 */
abstract class StrataLayout internal constructor(
    internal val context: StrataUiContext,
    spacing: Float,
    padding: StrataInsets,
    alignment: Int
) : Table(context.skin) {

    /** Semantic styles and spacing used to create nested controls. */
    val theme: StrataUiTheme
        get() = context.theme

    init {
        require(spacing.isFinite() && spacing >= 0f) {
            "Layout spacing must be finite and non-negative."
        }

        defaults().space(spacing)
        pad(
            padding.top,
            padding.left,
            padding.bottom,
            padding.right
        )
        align(alignment)
    }

    protected abstract fun <A : Actor> place(actor: A): Cell<A>

    /** Adds any Scene2D actor to this layout and returns the same actor. */
    fun <A : Actor> actor(actor: A): A {
        place(actor)
        return actor
    }

    fun label(
        text: String,
        styleName: String = theme.labelStyle
    ): Label {
        return actor(
            Label(text, context.skin, styleName)
        )
    }

    fun button(
        text: String,
        styleName: String = theme.buttonStyle,
        onClick: () -> Unit
    ): StrataButton {
        return actor(
            StrataButton(
                text = text,
                skin = context.skin,
                styleName = styleName,
                onClick = onClick
            )
        )
    }

    fun toggleButton(
        text: String,
        checked: Boolean = false,
        styleName: String = theme.toggleButtonStyle,
        onChanged: (Boolean) -> Unit = {}
    ): StrataToggleButton {
        return actor(
            StrataToggleButton(
                text = text,
                skin = context.skin,
                styleName = styleName,
                checked = checked,
                onChanged = onChanged
            )
        )
    }

    fun <T> selectableButton(
        text: String,
        value: T,
        group: StrataSelectionGroup<T>,
        styleName: String = theme.toggleButtonStyle
    ): StrataSelectableButton<T> {
        val button = StrataSelectableButton(
            text = text,
            value = value,
            selectionGroup = group,
            skin = context.skin,
            styleName = styleName
        )

        context.own(button)
        return actor(button)
    }

    fun row(
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        alignment: Int = Align.left,
        configure: StrataRow.() -> Unit
    ): StrataRow {
        val row = StrataRow(
            context = context,
            spacing = spacing,
            padding = padding,
            alignment = alignment
        ).apply(configure)

        return actor(row)
    }

    fun column(
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        alignment: Int = Align.topLeft,
        configure: StrataColumn.() -> Unit
    ): StrataColumn {
        val column = StrataColumn(
            context = context,
            spacing = spacing,
            padding = padding,
            alignment = alignment
        ).apply(configure)

        return actor(column)
    }

    fun panel(
        styleName: String? = theme.panelStyle,
        spacing: Float = theme.spacing,
        padding: StrataInsets? = null,
        blocksInput: Boolean = true,
        configure: StrataPanel.() -> Unit
    ): StrataPanel {
        val panel = StrataPanel(
            context = context,
            styleName = styleName,
            spacing = spacing,
            paddingOverride = padding,
            blocksInput = blocksInput
        ).apply(configure)

        return actor(panel)
    }

    fun separator(
        orientation: StrataSeparatorOrientation =
            StrataSeparatorOrientation.HORIZONTAL,
        styleName: String = requireNotNull(theme.separatorStyle) {
            "No separator style is configured in the UI theme."
        }
    ): StrataSeparator {
        val style = context.skin.get(
            styleName,
            StrataSeparatorStyle::class.java
        )

        return separator(
            style = style,
            orientation = orientation
        )
    }

    fun separator(
        style: StrataSeparatorStyle,
        orientation: StrataSeparatorOrientation =
            StrataSeparatorOrientation.HORIZONTAL
    ): StrataSeparator {
        val separator = StrataSeparator(
            orientation = orientation,
            style = style
        )

        val cell = place(separator)

        if (orientation == StrataSeparatorOrientation.HORIZONTAL) {
            cell.growX().height(separator.style.thickness)
        } else {
            cell.width(separator.style.thickness).growY()
        }

        return separator
    }

    fun spacer(
        width: Float = 0f,
        height: Float = 0f
    ): StrataSpacer {
        return actor(
            StrataSpacer(
                spacerWidth = width,
                spacerHeight = height
            )
        )
    }
}

/** A layout that places every child on a new row. */
class StrataColumn internal constructor(
    context: StrataUiContext,
    spacing: Float,
    padding: StrataInsets,
    alignment: Int
) : StrataLayout(
    context = context,
    spacing = spacing,
    padding = padding,
    alignment = alignment
) {

    override fun <A : Actor> place(actor: A): Cell<A> {
        return add(actor).also {
            row()
        }
    }
}

/** A layout that places children from left to right. */
class StrataRow internal constructor(
    context: StrataUiContext,
    spacing: Float,
    padding: StrataInsets,
    alignment: Int
) : StrataLayout(
    context = context,
    spacing = spacing,
    padding = padding,
    alignment = alignment
) {

    override fun <A : Actor> place(actor: A): Cell<A> {
        return add(actor)
    }
}

/**
 * A vertical, skin-styled layout that consumes pointer input by default.
 *
 * Input blocking covers the whole panel, including empty padding, so world
 * controls cannot activate behind it. Set [blocksInput] to false for a purely
 * decorative panel.
 */
class StrataPanel internal constructor(
    context: StrataUiContext,
    styleName: String?,
    spacing: Float,
    private val paddingOverride: StrataInsets?,
    blocksInput: Boolean
) : StrataLayout(
    context = context,
    spacing = spacing,
    padding = StrataInsets.NONE,
    alignment = Align.topLeft
) {

    var blocksInput: Boolean = blocksInput

    var style: StrataPanelStyle = StrataPanelStyle()
        private set

    init {
        if (styleName != null) {
            setStyle(styleName)
        } else {
            applyStyle(StrataPanelStyle())
        }

        blockPointerInput {
            this.blocksInput
        }
    }

    override fun <A : Actor> place(actor: A): Cell<A> {
        return add(actor).also {
            row()
        }
    }

    fun setStyle(styleName: String) {
        applyStyle(
            context.skin.get(
                styleName,
                StrataPanelStyle::class.java
            )
        )
    }

    fun setStyle(style: StrataPanelStyle) {
        applyStyle(style)
    }

    private fun applyStyle(style: StrataPanelStyle) {
        require(
            style.padTop.isFinite() && style.padTop >= 0f &&
                style.padLeft.isFinite() && style.padLeft >= 0f &&
                style.padBottom.isFinite() && style.padBottom >= 0f &&
                style.padRight.isFinite() && style.padRight >= 0f
        ) {
            "Panel padding must be finite and non-negative."
        }

        this.style = StrataPanelStyle(style)
        background = style.background

        val padding = paddingOverride ?: StrataInsets(
            top = style.padTop,
            left = style.padLeft,
            bottom = style.padBottom,
            right = style.padRight
        )

        pad(
            padding.top,
            padding.left,
            padding.bottom,
            padding.right
        )
        invalidateHierarchy()
    }
}

/**
 * Configures the Scene2D [Cell] containing this actor.
 *
 * Call this immediately after adding the actor through a Strata layout.
 */
fun <A : Actor> A.cell(
    configure: Cell<A>.() -> Unit
): A {
    val table = parent as? Table
        ?: error("The actor is not attached to a Table.")

    val cell = table.getCell(this)
        ?: error("The actor is not managed by its parent Table.")

    cell.configure()
    return this
}
