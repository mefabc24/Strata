package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

internal class StrataUiContext(
    val skin: Skin,
    val theme: StrataUiTheme
) {
    private val selectionControls = mutableListOf<StrataSelectionControl>()
    private val subscriptions = mutableListOf<StrataSelectionSubscription>()
    private val resourceTextures = linkedMapOf<String, Texture>()

    fun own(control: StrataSelectionControl) {
        selectionControls += control
    }

    fun own(subscription: StrataSelectionSubscription) {
        subscriptions += subscription
    }

    fun resourceDrawable(path: String): Drawable {
        require(path.isNotBlank()) { "UI image path must not be blank." }
        val texture = resourceTextures.getOrPut(path) {
            Texture(Gdx.files.internal(path))
        }
        return TextureRegionDrawable(TextureRegion(texture))
    }

    fun dispose() {
        for (control in selectionControls) {
            control.detach()
        }

        for (subscription in subscriptions) {
            subscription.dispose()
        }

        selectionControls.clear()
        subscriptions.clear()
        resourceTextures.values.forEach(Texture::dispose)
        resourceTextures.clear()
    }
}

/**
 * Base class for Strata's small Scene2D table layout DSL.
 *
 * Layouts are regular [Table] actors. Add custom Scene2D actors with [actor]
 * and use [cell] to configure their underlying Scene2D cells directly.
 * Layout spacing is placed between sibling cells, while padding surrounds the
 * layout's content inside its bounds.
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
        styleName: String = theme.selectableButtonStyle
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

    fun imageButton(
        drawable: Drawable,
        styleName: String = theme.imageButtonStyle,
        onClick: () -> Unit
    ): StrataImageButton {
        return actor(
            StrataImageButton(
                drawable = drawable,
                skin = context.skin,
                styleName = styleName,
                onClick = onClick
            )
        )
    }

    /** Creates an image button from a classpath resource owned by this UI. */
    fun imageButton(
        resourcePath: String,
        styleName: String = theme.imageButtonStyle,
        onClick: () -> Unit
    ): StrataImageButton = imageButton(
        drawable = context.resourceDrawable(resourcePath),
        styleName = styleName,
        onClick = onClick
    )

    /** Displays a classpath image whose texture is owned by this UI. */
    fun image(resourcePath: String): StrataImage = actor(
        StrataImage(context.resourceDrawable(resourcePath))
    )

    fun <T> selectableImageButton(
        drawable: Drawable,
        value: T,
        group: StrataSelectionGroup<T>,
        styleName: String = theme.selectableImageButtonStyle
    ): StrataSelectableImageButton<T> {
        val button = StrataSelectableImageButton(
            drawable = drawable,
            value = value,
            selectionGroup = group,
            skin = context.skin,
            styleName = styleName
        )

        context.own(button)
        return actor(button)
    }

    fun <T> selectableImageButton(
        resourcePath: String,
        value: T,
        group: StrataSelectionGroup<T>,
        styleName: String = theme.selectableImageButtonStyle
    ): StrataSelectableImageButton<T> = selectableImageButton(
        drawable = context.resourceDrawable(resourcePath),
        value = value,
        group = group,
        styleName = styleName
    )

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

    fun row(
        alignment: StrataAlignment,
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        configure: StrataRow.() -> Unit
    ): StrataRow = row(spacing, padding, alignment.scene2dValue, configure)

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

    fun column(
        alignment: StrataAlignment,
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        configure: StrataColumn.() -> Unit
    ): StrataColumn = column(spacing, padding, alignment.scene2dValue, configure)

    fun grid(
        columns: Int,
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        alignment: Int = Align.topLeft,
        configure: StrataGrid.() -> Unit
    ): StrataGrid {
        val grid = StrataGrid(
            context = context,
            columnCount = columns,
            spacing = spacing,
            padding = padding,
            alignment = alignment
        ).apply(configure)

        return actor(grid)
    }

    fun grid(
        columns: Int,
        alignment: StrataAlignment,
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        configure: StrataGrid.() -> Unit
    ): StrataGrid = grid(columns, spacing, padding, alignment.scene2dValue, configure)

    fun responsiveGrid(
        minimumItemWidth: Float = 120f,
        itemHeight: Float = 38f,
        spacing: Float = theme.spacing,
        maximumColumns: Int = 3,
        configure: StrataResponsiveGrid.() -> Unit
    ): StrataResponsiveGrid = actor(
        StrataResponsiveGrid(
            context, minimumItemWidth, itemHeight, spacing, maximumColumns
        ).apply(configure)
    )

    fun scrollColumn(
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        configure: StrataColumn.() -> Unit
    ): StrataScrollPane {
        val content = StrataColumn(
            context, spacing, padding, Align.topLeft
        ).apply(configure)
        return actor(StrataScrollPane(context, content))
    }

    fun numericStepper(
        label: String,
        value: Float,
        minimum: Float,
        maximum: Float,
        step: Float,
        decimals: Int = 2,
        onChanged: (Float) -> Unit
    ): StrataNumericStepper = actor(
        StrataNumericStepper(
            label, context.skin, context.theme.buttonStyle, value,
            minimum, maximum, step, decimals, onChanged
        )
    )

    fun stack(
        configure: StrataStack.() -> Unit
    ): StrataStack {
        return actor(
            StrataStack(context).apply(configure)
        )
    }

    fun expander(
        title: String,
        expanded: Boolean = true,
        spacing: Float = theme.spacing,
        headerHeight: Float? = null,
        contentGrowY: Boolean = false,
        expandedStyle: StrataExpanderStyle? = null,
        onExpandedChanged: (Boolean) -> Unit = {},
        headerContent: (StrataRow.() -> Unit)? = null,
        configure: StrataColumn.() -> Unit
    ): StrataExpander {
        return actor(
            StrataExpander(
                context = context,
                title = title,
                expanded = expanded,
                spacing = spacing,
                headerHeight = headerHeight,
                contentGrowY = contentGrowY,
                expandedStyle = expandedStyle,
                onExpandedChanged = onExpandedChanged,
                headerContent = headerContent,
                configure = configure
            )
        )
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
 * A fixed-column layout that places children left-to-right in insertion order.
 *
 * A new row starts automatically after [columnCount] children. The final row
 * may contain fewer children.
 */
class StrataGrid internal constructor(
    context: StrataUiContext,
    val columnCount: Int,
    spacing: Float,
    padding: StrataInsets,
    alignment: Int
) : StrataLayout(
    context = context,
    spacing = spacing,
    padding = padding,
    alignment = alignment
) {
    private var itemCount = 0

    init {
        require(columnCount > 0) {
            "Grid column count must be positive."
        }
    }

    override fun <A : Actor> place(actor: A): Cell<A> {
        val cell = add(actor)
        itemCount++

        if (itemCount % columnCount == 0) {
            row()
        }

        return cell
    }
}

/**
 * Overlays regular Scene2D actors for simple switchable UI sections.
 */
class StrataStack internal constructor(
    private val context: StrataUiContext
) : Stack() {

    /** Measures only visible layers when determining the required height. */
    override fun getPrefHeight(): Float =
        children
            .filter { it.isVisible }
            .maxOfOrNull { actor ->
                (actor as? Layout)?.prefHeight ?: actor.height
            } ?: 0f

    override fun getMinHeight(): Float =
        children
            .filter { it.isVisible }
            .maxOfOrNull { actor ->
                (actor as? Layout)?.minHeight ?: actor.height
            } ?: 0f

    /** Adds an actor as a stack layer and returns the same actor. */
    fun <A : Actor> actor(actor: A): A {
        addActor(actor)
        return actor
    }

    fun column(
        spacing: Float = context.theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        alignment: Int = Align.topLeft,
        configure: StrataColumn.() -> Unit
    ): StrataColumn {
        return actor(
            StrataColumn(
                context = context,
                spacing = spacing,
                padding = padding,
                alignment = alignment
            ).apply(configure)
        )
    }
}

/**
 * A compact header that shows or removes its content from layout on click.
 */
class StrataExpander internal constructor(
    context: StrataUiContext,
    val title: String,
    expanded: Boolean,
    private val spacing: Float,
    headerHeight: Float? = null,
    private val contentGrowY: Boolean = false,
    private val expandedStyle: StrataExpanderStyle? = null,
    private val onExpandedChanged: (Boolean) -> Unit = {},
    headerContent: (StrataRow.() -> Unit)? = null,
    configure: StrataColumn.() -> Unit
) : Table(context.skin) {

    val content = StrataColumn(
        context = context,
        spacing = context.theme.spacing,
        padding = StrataInsets.NONE,
        alignment = Align.topLeft
    ).apply(configure)

    val header = StrataButton(
        text = title,
        skin = context.skin,
        styleName = context.theme.buttonStyle,
        onClick = ::toggle
    )

    private val contentCell: Cell<StrataColumn>

    var expanded: Boolean = expanded
        set(value) {
            if (field == value) return

            field = value
            updateExpansion()
            onExpandedChanged(value)
        }

    init {
        require(spacing.isFinite() && spacing >= 0f) {
            "Expander spacing must be finite and non-negative."
        }
        require(headerHeight == null || headerHeight.isFinite() && headerHeight > 0f) {
            "Expander header height must be finite and positive."
        }

        top().left()
        val headerRow = StrataRow(
            context, context.theme.spacing, StrataInsets.NONE, Align.left
        )
        headerRow.actor(header).cell {
            growX().fillX()
            headerHeight?.let(::height)
        }
        headerContent?.let(headerRow::apply)
        add(headerRow).growX().fillX()
        row()
        contentCell = add(content).growX().fillX()
        updateExpansion()
    }

    fun toggle() {
        expanded = !expanded
    }

    private fun updateExpansion() {
        @Suppress("UsePropertyAccessSyntax")
        header.setText("$title  ${if (expanded) "v" else ">"}")
        content.isVisible = expanded

        if (expanded) {
            if (contentGrowY) {
                contentCell
                    .minHeight(0f)
                    .prefHeight(Value.prefHeight)
                    .maxHeight(Value.maxHeight)
                    .expandY()
                    .fillY()
                    .padTop(spacing)
            } else {
                contentCell.height(Value.prefHeight).padTop(spacing)
            }
        } else {
            contentCell.height(0f).padTop(0f)
        }

        applyExpandedStyle()

        invalidateHierarchy()
    }

    private fun applyExpandedStyle() {
        val style = expandedStyle.takeIf { expanded }
        background = style?.background
        pad(
            style?.padTop ?: 0f,
            style?.padLeft ?: 0f,
            style?.padBottom ?: 0f,
            style?.padRight ?: 0f
        )
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
    var blocksInput: Boolean
) : StrataLayout(
    context = context,
    spacing = spacing,
    padding = StrataInsets.NONE,
    alignment = Align.topLeft
) {

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
