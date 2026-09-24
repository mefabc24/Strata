package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Scaling

/**
 * A momentary text button with a Kotlin callback.
 *
 * Its style is resolved from the caller-owned [Skin]. The underlying
 * [TextButton] API remains available for direct customization.
 */
class StrataButton(
    text: String,
    skin: Skin,
    styleName: String = "default",
    onClick: () -> Unit
) : TextButton(text, skin, styleName) {

    /** Callback invoked after a completed, enabled click. */
    var onClick: () -> Unit = onClick

    init {
        setProgrammaticChangeEvents(false)
        preventCheckedState()

        addListener(
            object : ClickListener() {
                override fun clicked(
                    event: InputEvent,
                    x: Float,
                    y: Float
                ) {
                    if (isDisabled) return

                    this@StrataButton.onClick()
                }
            }
        )
    }
}

private fun Button.preventCheckedState() {
    addCaptureListener(
        object : ChangeListener() {
            override fun changed(
                event: ChangeEvent,
                actor: Actor
            ) {
                event.cancel()
                event.stop()
            }
        }
    )
}

/**
 * A two-state text button with a checked-state callback.
 */
class StrataToggleButton(
    text: String,
    skin: Skin,
    styleName: String = "default",
    checked: Boolean = false,
    onChanged: (Boolean) -> Unit = {}
) : TextButton(text, skin, styleName) {

    /** Callback invoked whenever the checked state changes. */
    var onChanged: (Boolean) -> Unit = onChanged

    init {
        isChecked = checked

        addListener(
            object : ChangeListener() {
                override fun changed(
                    event: ChangeEvent,
                    actor: Actor
                ) {
                    this@StrataToggleButton.onChanged(isChecked)
                }
            }
        )
    }
}

/**
 * A text button bound to one value in a [StrataSelectionGroup].
 *
 * Exactly one control can be attached to each group value at a time. Call
 * [detach] when constructing this control outside [StrataUi]'s builders and
 * its lifetime is shorter than the group's lifetime.
 */
class StrataSelectableButton<T>(
    text: String,
    val value: T,
    val selectionGroup: StrataSelectionGroup<T>,
    skin: Skin,
    styleName: String = "default"
) : TextButton(text, skin, styleName) {

    private var attached = true
    private val subscription: StrataSelectionSubscription

    init {
        selectionGroup.attach(value)
        setProgrammaticChangeEvents(false)
        isChecked = selectionGroup.selected == value

        subscription = selectionGroup.onSelectionChanged { selected ->
            if (attached) {
                isChecked = selected == value
            }
        }

        addListener(
            object : ChangeListener() {
                override fun changed(
                    event: ChangeEvent,
                    actor: Actor
                ) {
                    if (!attached) return

                    if (isChecked) {
                        selectionGroup.select(value)
                    } else if (selectionGroup.selected == value) {
                        if (!selectionGroup.clearSelection()) {
                            isChecked = true
                        }
                    }
                }
            }
        )
    }

    /**
     * Stops synchronizing this button with its selection group.
     */
    fun detach() {
        if (!attached) return

        attached = false
        subscription.dispose()
        selectionGroup.detach(value)
    }
}

/**
 * A styled dividing line used by layout builders.
 */
class StrataSeparator(
    val orientation: StrataSeparatorOrientation,
    style: StrataSeparatorStyle
) : Image() {

    var style: StrataSeparatorStyle = StrataSeparatorStyle(style)
        private set

    init {
        setScaling(Scaling.stretch)
        setStyle(style)
    }

    fun setStyle(style: StrataSeparatorStyle) {
        require(style.thickness.isFinite() && style.thickness > 0f) {
            "Separator thickness must be finite and positive."
        }

        val drawable = requireNotNull(style.drawable) {
            "A separator style requires a drawable."
        }

        this.style = StrataSeparatorStyle(style)
        setDrawable(drawable)
        invalidateHierarchy()
    }

    override fun getPrefWidth(): Float {
        return if (orientation == StrataSeparatorOrientation.VERTICAL) {
            style.thickness
        } else {
            0f
        }
    }

    override fun getPrefHeight(): Float {
        return if (orientation == StrataSeparatorOrientation.HORIZONTAL) {
            style.thickness
        } else {
            0f
        }
    }
}

/**
 * A fixed-size blank layout element.
 */
class StrataSpacer(
    val spacerWidth: Float = 0f,
    val spacerHeight: Float = 0f
) : Widget() {

    init {
        require(
            spacerWidth.isFinite() && spacerWidth >= 0f &&
                spacerHeight.isFinite() && spacerHeight >= 0f
        ) {
            "Spacer dimensions must be finite and non-negative."
        }
    }

    override fun getPrefWidth() = spacerWidth

    override fun getPrefHeight() = spacerHeight
}

internal fun Actor.blockPointerInput(
    shouldBlock: () -> Boolean
) {
    addListener(
        object : InputListener() {
            override fun touchDown(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ): Boolean {
                return shouldBlock()
            }

            override fun scrolled(
                event: InputEvent,
                x: Float,
                y: Float,
                amountX: Float,
                amountY: Float
            ): Boolean {
                return shouldBlock()
            }
        }
    )
}
