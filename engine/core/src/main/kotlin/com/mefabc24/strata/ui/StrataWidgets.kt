package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
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
    var onClick: () -> Unit
) : TextButton(text, skin, styleName) {
    init {
        programmaticChangeEvents = false
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
    var onChanged: (Boolean) -> Unit = {}
) : TextButton(text, skin, styleName) {

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

    /** Updates checked state without reporting a user change. */
    fun syncChecked(checked: Boolean) {
        val previous = programmaticChangeEvents
        programmaticChangeEvents = false
        isChecked = checked
        programmaticChangeEvents = previous
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
) : TextButton(text, skin, styleName), StrataSelectionControl {

    private val selectionBinding = StrataSelectionBinding(
        value = value,
        selectionGroup = selectionGroup,
        isChecked = { isChecked },
        setChecked = { isChecked = it }
    )

    init {
        programmaticChangeEvents = false

        addListener(
            object : ChangeListener() {
                override fun changed(
                    event: ChangeEvent,
                    actor: Actor
                ) {
                    selectionBinding.controlChanged()
                }
            }
        )
    }

    /**
     * Stops synchronizing this button with its selection group.
     */
    override fun detach() {
        selectionBinding.detach()
    }
}

/**
 * A momentary image button with a Kotlin callback.
 *
 * The drawable is referenced but not owned. Button chrome and interaction
 * states come from the caller-owned skin's [ImageButton.ImageButtonStyle].
 */
class StrataImageButton(
    drawable: Drawable,
    skin: Skin,
    styleName: String = "default",
    var onClick: () -> Unit
) : ImageButton(
    imageButtonStyle(
        skin = skin,
        styleName = styleName,
        drawable = drawable
    )
) {
    init {
        programmaticChangeEvents = false
        preventCheckedState()

        addListener(
            object : ClickListener() {
                override fun clicked(
                    event: InputEvent,
                    x: Float,
                    y: Float
                ) {
                    if (isDisabled) return

                    this@StrataImageButton.onClick()
                }
            }
        )
    }
}

/**
 * An image button bound to one value in a [StrataSelectionGroup].
 *
 * The drawable is referenced but not owned. Exactly one control can be
 * attached to each group value at a time.
 */
class StrataSelectableImageButton<T>(
    drawable: Drawable,
    val value: T,
    val selectionGroup: StrataSelectionGroup<T>,
    skin: Skin,
    styleName: String = "default"
) : ImageButton(
    imageButtonStyle(
        skin = skin,
        styleName = styleName,
        drawable = drawable
    )
), StrataSelectionControl {

    private val selectionBinding = StrataSelectionBinding(
        value = value,
        selectionGroup = selectionGroup,
        isChecked = { isChecked },
        setChecked = { isChecked = it }
    )

    init {
        programmaticChangeEvents = false

        addListener(
            object : ChangeListener() {
                override fun changed(
                    event: ChangeEvent,
                    actor: Actor
                ) {
                    selectionBinding.controlChanged()
                }
            }
        )
    }

    /** Stops synchronizing this button with its selection group. */
    override fun detach() {
        selectionBinding.detach()
    }
}

internal interface StrataSelectionControl {
    fun detach()
}

private class StrataSelectionBinding<T>(
    private val value: T,
    private val selectionGroup: StrataSelectionGroup<T>,
    private val isChecked: () -> Boolean,
    private val setChecked: (Boolean) -> Unit
) {
    private var attached = true

    private val subscription: StrataSelectionSubscription

    init {
        selectionGroup.attach(value)
        setChecked(selectionGroup.selected == value)

        subscription = selectionGroup.onSelectionChanged { selected ->
            if (attached) {
                setChecked(selected == value)
            }
        }
    }

    fun controlChanged() {
        if (!attached) return

        if (isChecked()) {
            selectionGroup.select(value)
        } else if (selectionGroup.selected == value) {
            if (!selectionGroup.clearSelection()) {
                setChecked(true)
            }
        }
    }

    fun detach() {
        if (!attached) return

        attached = false
        subscription.dispose()
        selectionGroup.detach(value)
    }
}

private fun imageButtonStyle(
    skin: Skin,
    styleName: String,
    drawable: Drawable
): ImageButton.ImageButtonStyle {
    return ImageButton.ImageButtonStyle(
        skin.get(
            styleName,
            ImageButton.ImageButtonStyle::class.java
        )
    ).apply {
        imageUp = drawable
        imageDown = null
        imageOver = null
        imageDisabled = null
        imageChecked = null
        imageCheckedDown = null
        imageCheckedOver = null
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

internal fun Actor.blockScrollInput() {
    addListener(
        object : InputListener() {
            override fun scrolled(
                event: InputEvent,
                x: Float,
                y: Float,
                amountX: Float,
                amountY: Float
            ): Boolean = true
        }
    )
}

internal fun ScrollPane.useHoverScrollFocus() {
    touchable = Touchable.enabled

    blockScrollInput()

    addListener(HoverScrollFocusListener)
}

/** Image created by Strata's resource-path UI API. */
class StrataImage internal constructor(drawable: Drawable) : Image(drawable)

private object HoverScrollFocusListener : InputListener() {
    override fun enter(
        event: InputEvent,
        x: Float,
        y: Float,
        pointer: Int,
        fromActor: Actor?
    ) {
        if (pointer == -1) updateHoverScrollFocus(event)
    }

    override fun exit(
        event: InputEvent,
        x: Float,
        y: Float,
        pointer: Int,
        toActor: Actor?
    ) {
        if (pointer == -1) updateHoverScrollFocus(event)
    }
}

private class HoverScrollFocusRouter : InputListener() {
    override fun scrolled(
        event: InputEvent,
        x: Float,
        y: Float,
        amountX: Float,
        amountY: Float
    ): Boolean {
        val stage = event.stage ?: return false
        val target = stage.hoverScrollPaneAt(event.stageX, event.stageY)
        val currentTarget = event.target

        if (target == null) {
            if (currentTarget.isHoverScrollPane()) {
                stage.setScrollFocus(null)
                event.stop()
            }
            return false
        }

        stage.setScrollFocus(target)
        if (currentTarget === target) return false

        event.stop()
        val redirected = InputEvent().apply {
            type = InputEvent.Type.scrolled
            this.stage = stage
            stageX = event.stageX
            stageY = event.stageY
            scrollAmountX = amountX
            scrollAmountY = amountY
        }
        target.fire(redirected)
        if (redirected.isHandled) event.handle()
        return redirected.isHandled
    }
}

private fun updateHoverScrollFocus(event: InputEvent) {
    val stage = event.stage ?: return
    stage.installHoverScrollFocusRouter()
    val target = stage.hoverScrollPaneAt(event.stageX, event.stageY)
    if (target != null) {
        stage.setScrollFocus(target)
    } else if (stage.scrollFocus.isHoverScrollPane()) {
        stage.setScrollFocus(null)
    }
}

private fun Stage.installHoverScrollFocusRouter() {
    if (root.captureListeners.any { it is HoverScrollFocusRouter }) return
    root.addCaptureListener(HoverScrollFocusRouter())
}

private fun Stage.hoverScrollPaneAt(stageX: Float, stageY: Float): ScrollPane? {
    var actor: Actor? = hit(stageX, stageY, true)
    while (actor != null) {
        if (actor.isHoverScrollPane()) return actor as ScrollPane
        actor = actor.parent
    }
    return null
}

private fun Actor?.isHoverScrollPane(): Boolean {
    return this is ScrollPane && listeners.contains(HoverScrollFocusListener, true)
}

/** Invokes callbacks when the pointer enters or leaves this actor. */
fun <A : Actor> A.onHover(
    entered: (A) -> Unit,
    exited: (A) -> Unit
): A {
    addListener(
        object : InputListener() {
            override fun enter(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                fromActor: Actor?
            ) {
                if (pointer == -1) entered(this@onHover)
            }

            override fun exit(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                toActor: Actor?
            ) {
                if (pointer == -1) exited(this@onHover)
            }
        }
    )
    return this
}
