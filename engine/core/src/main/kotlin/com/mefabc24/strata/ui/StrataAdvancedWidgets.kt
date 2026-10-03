package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max

/** A compact single-choice control that keeps domain values out of Scene2D labels. */
class StrataDropdown<T>(
    options: Iterable<T>,
    selected: T,
    skin: Skin,
    styleName: String = "default",
    private val displayText: (T) -> String = { it.toString() },
    var onChanged: (T) -> Unit = {}
) : SelectBox<StrataDropdown.Option<T>>(skin, styleName) {
    data class Option<T>(val value: T, private val text: String) {
        override fun toString(): String = text
    }

    private val values = options.toList()
    private var synchronizing = false

    var value: T
        get() = selected.value
        set(value) = select(value, notify = true)

    init {
        require(values.isNotEmpty()) { "Dropdown options must not be empty." }
        require(selected in values) { "The selected dropdown value must be an option." }
        setItems(*values.map { Option(it, displayText(it)) }.toTypedArray())
        setSelected(optionFor(selected))
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (!synchronizing) onChanged(value)
            }
        })
    }

    /** Updates the authoritative value without invoking [onChanged]. */
    fun sync(value: T) = select(value, notify = false)

    private fun select(value: T, notify: Boolean) {
        require(value in values) { "The selected dropdown value must be an option." }
        if (selected.value == value) return
        synchronizing = !notify
        try {
            setSelected(optionFor(value))
        } finally {
            synchronizing = false
        }
    }

    private fun optionFor(value: T): Option<T> = items.first { it.value == value }
}

/** Returns the number of equal-width columns that fit in [availableWidth]. */
fun responsiveColumnCount(
    availableWidth: Float,
    minimumItemWidth: Float,
    spacing: Float,
    maximumColumns: Int
): Int {
    require(availableWidth.isFinite() && availableWidth >= 0f)
    require(minimumItemWidth.isFinite() && minimumItemWidth > 0f)
    require(spacing.isFinite() && spacing >= 0f)
    require(maximumColumns > 0)
    return floor((availableWidth + spacing) / (minimumItemWidth + spacing))
        .toInt().coerceIn(1, maximumColumns)
}

/** Equal-width controls that reflow as their available width changes. */
class StrataResponsiveGrid internal constructor(
    private val context: StrataUiContext,
    val minimumItemWidth: Float,
    val itemHeight: Float,
    val spacing: Float,
    val maximumColumns: Int
) : WidgetGroup() {
    init {
        responsiveColumnCount(0f, minimumItemWidth, spacing, maximumColumns)
        require(itemHeight.isFinite() && itemHeight > 0f)
        touchable = Touchable.childrenOnly
    }

    fun <A : Actor> actor(actor: A): A {
        addActor(actor)
        invalidateHierarchy()
        return actor
    }

    fun button(text: String, onClick: () -> Unit): StrataButton = actor(
        StrataButton(text, context.skin, context.theme.buttonStyle, onClick)
    )

    fun <T> selectableButton(
        text: String,
        value: T,
        group: StrataSelectionGroup<T>
    ): StrataSelectableButton<T> {
        val button = StrataSelectableButton(
            text, value, group, context.skin, context.theme.selectableButtonStyle
        )
        context.own(button)
        return actor(button)
    }

    override fun layout() {
        val columns = responsiveColumnCount(width, minimumItemWidth, spacing, maximumColumns)
        val cellWidth = ((width - spacing * (columns - 1)) / columns).coerceAtLeast(0f)
        children.forEachIndexed { index, child ->
            val column = index % columns
            val row = index / columns
            val x = column * (cellWidth + spacing)
            val y = height - itemHeight - row * (itemHeight + spacing)
            child.setBounds(x, y, cellWidth, itemHeight)
            if (child is Layout) child.validate()
        }
    }

    override fun getPrefWidth(): Float =
        minimumItemWidth * maximumColumns + spacing * (maximumColumns - 1)

    override fun getMinWidth(): Float = minimumItemWidth

    override fun getPrefHeight(): Float {
        val columns = responsiveColumnCount(
            if (width > 0f) width else prefWidth,
            minimumItemWidth,
            spacing,
            maximumColumns
        )
        val rows = (children.size + columns - 1) / columns
        return if (rows == 0) 0f else rows * itemHeight + (rows - 1) * spacing
    }
}

/** A vertically scrolling Strata column with horizontal content fitting. */
class StrataScrollPane internal constructor(
    context: StrataUiContext,
    val content: StrataColumn
) : ScrollPane(
    content,
    if (context.skin.has("default", ScrollPaneStyle::class.java)) {
        context.skin.get("default", ScrollPaneStyle::class.java)
    } else {
        ScrollPaneStyle()
    }
) {
    init {
        setScrollingDisabled(true, false)
        setOverscroll(false, false)
        setFadeScrollBars(true)
        setFlickScroll(true)
        setClamp(true)

        useHoverScrollFocus()
    }
}

/** A reusable label, decrement button, value, and increment button row. */
class StrataNumericStepper(
    label: String,
    skin: Skin,
    styleName: String = "default",
    value: Float,
    val minimum: Float,
    val maximum: Float,
    val step: Float,
    private val decimals: Int = 2,
    var onChanged: (Float) -> Unit = {}
) : Table(skin) {
    private val valueLabel = Label("", skin)
    private var storedValue = value.coerceIn(minimum, maximum)

    var value: Float
        get() = storedValue
        set(value) = setValue(value, notify = true)

    init {
        require(minimum.isFinite() && maximum.isFinite() && minimum <= maximum)
        require(step.isFinite() && step > 0f)
        require(decimals >= 0)
        align(Align.left)
        add(Label(label, skin)).growX().left()
        add(StrataButton("-", skin, styleName, ::decrement)).width(30f).height(28f)
        add(valueLabel).width(52f).padLeft(4f).padRight(4f).center()
        add(StrataButton("+", skin, styleName, ::increment)).width(30f).height(28f)
        updateLabel()
    }

    fun increment() = setValue(storedValue + step, notify = true)

    fun decrement() = setValue(storedValue - step, notify = true)

    /** Updates the displayed authoritative value without firing [onChanged]. */
    fun sync(value: Float) = setValue(value, notify = false)

    private fun setValue(value: Float, notify: Boolean) {
        require(value.isFinite())
        val clamped = value.coerceIn(minimum, maximum)
        if (clamped == storedValue) return
        storedValue = clamped
        updateLabel()
        if (notify) onChanged(clamped)
    }

    private fun updateLabel() {
        valueLabel.setText(String.format(Locale.ROOT, "%.${decimals}f", storedValue))
    }
}
