package com.mefabc24.strata.ui

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Provides a Scene2D UI layer and a small Kotlin construction API.
 *
 * Strata owns and disposes [stage]. The supplied [skin] and all resources in
 * it remain owned by the caller. A scene normally updates, renders, resizes,
 * and disposes this instance automatically.
 */
class StrataUi(
    val skin: Skin,
    val theme: StrataUiTheme = StrataUiTheme()
) : Disposable {

    private val context = StrataUiContext(
        skin = skin,
        theme = theme
    )

    private var disposed = false

    val stage = Stage(
        ScreenViewport()
    )

    /**
     * Root vertical layout for game-specific UI.
     *
     * It fills the stage and aligns content to the top left. It intentionally
     * does not consume input outside its child controls and panels.
     */
    val root = StrataColumn(
        context = context,
        spacing = theme.spacing,
        padding = StrataInsets.NONE,
        alignment = Align.topLeft
    ).apply {
        setFillParent(true)
    }

    /**
     * Input processor used by the UI stage.
     */
    val inputProcessor: InputProcessor
        get() = stage

    init {
        stage.addActor(root)
    }

    fun <A : Actor> actor(actor: A): A = root.actor(actor)

    fun label(
        text: String,
        styleName: String = theme.labelStyle
    ): Label = root.label(text, styleName)

    fun button(
        text: String,
        styleName: String = theme.buttonStyle,
        onClick: () -> Unit
    ): StrataButton = root.button(text, styleName, onClick)

    fun toggleButton(
        text: String,
        checked: Boolean = false,
        styleName: String = theme.toggleButtonStyle,
        onChanged: (Boolean) -> Unit = {}
    ): StrataToggleButton = root.toggleButton(
        text = text,
        checked = checked,
        styleName = styleName,
        onChanged = onChanged
    )

    fun <T> selectableButton(
        text: String,
        value: T,
        group: StrataSelectionGroup<T>,
        styleName: String = theme.toggleButtonStyle
    ): StrataSelectableButton<T> = root.selectableButton(
        text = text,
        value = value,
        group = group,
        styleName = styleName
    )

    fun imageButton(
        drawable: Drawable,
        styleName: String = theme.imageButtonStyle,
        onClick: () -> Unit
    ): StrataImageButton = root.imageButton(
        drawable = drawable,
        styleName = styleName,
        onClick = onClick
    )

    fun <T> selectableImageButton(
        drawable: Drawable,
        value: T,
        group: StrataSelectionGroup<T>,
        styleName: String = theme.selectableImageButtonStyle
    ): StrataSelectableImageButton<T> = root.selectableImageButton(
        drawable = drawable,
        value = value,
        group = group,
        styleName = styleName
    )

    fun row(
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        alignment: Int = Align.left,
        configure: StrataRow.() -> Unit
    ): StrataRow = root.row(
        spacing = spacing,
        padding = padding,
        alignment = alignment,
        configure = configure
    )

    fun column(
        spacing: Float = theme.spacing,
        padding: StrataInsets = StrataInsets.NONE,
        alignment: Int = Align.topLeft,
        configure: StrataColumn.() -> Unit
    ): StrataColumn = root.column(
        spacing = spacing,
        padding = padding,
        alignment = alignment,
        configure = configure
    )

    fun panel(
        styleName: String? = theme.panelStyle,
        spacing: Float = theme.spacing,
        padding: StrataInsets? = null,
        blocksInput: Boolean = true,
        configure: StrataPanel.() -> Unit
    ): StrataPanel = root.panel(
        styleName = styleName,
        spacing = spacing,
        padding = padding,
        blocksInput = blocksInput,
        configure = configure
    )

    fun separator(
        orientation: StrataSeparatorOrientation =
            StrataSeparatorOrientation.HORIZONTAL,
        styleName: String = requireNotNull(theme.separatorStyle) {
            "No separator style is configured in the UI theme."
        }
    ): StrataSeparator = root.separator(
        orientation = orientation,
        styleName = styleName
    )

    fun spacer(
        width: Float = 0f,
        height: Float = 0f
    ): StrataSpacer = root.spacer(width, height)

    /**
     * Creates a required value-based selection group owned by game code.
     *
     * Controls built through this UI are detached when the UI is disposed.
     */
    fun <T> selectionGroup(
        options: Iterable<T>,
        initialSelection: T? = null,
        onSelectionChanged: ((T) -> Unit)? = null
    ): StrataSelectionGroup<T> {
        checkActive()

        val group = StrataSelectionGroup(
            options = options,
            initialSelection = initialSelection,
            selectionRequired = true
        )

        if (onSelectionChanged != null) {
            context.own(
                group.onSelectionChanged { selected ->
                    onSelectionChanged(
                        requireNotNull(selected) {
                            "A required selection group has no selection."
                        }
                    )
                }
            )
        }

        return group
    }

    /**
     * Creates a value-based selection group that can be cleared.
     *
     * The callback receives null when the selection is cleared. Controls
     * built through this UI are detached when the UI is disposed.
     */
    fun <T> optionalSelectionGroup(
        options: Iterable<T>,
        initialSelection: T? = null,
        onSelectionChanged: ((T?) -> Unit)? = null
    ): StrataSelectionGroup<T> {
        checkActive()

        val group = StrataSelectionGroup(
            options = options,
            initialSelection = initialSelection,
            selectionRequired = false
        )

        if (onSelectionChanged != null) {
            context.own(
                group.onSelectionChanged(onSelectionChanged)
            )
        }

        return group
    }

    /**
     * Updates UI actions and actors.
     */
    fun update(delta: Float) {
        checkActive()

        require(delta.isFinite() && delta >= 0f) {
            "UI delta time must be finite and non-negative."
        }

        stage.act(delta)
    }

    /**
     * Renders the UI.
     */
    fun render() {
        checkActive()
        stage.draw()
    }

    /**
     * Updates the UI viewport.
     */
    fun resize(
        width: Int,
        height: Int
    ) {
        checkActive()

        if (width <= 0 || height <= 0) return

        stage.viewport.update(
            width,
            height,
            true
        )
    }

    /**
     * Releases resources owned by the UI layer.
     *
     * The supplied skin remains owned by the caller.
     */
    override fun dispose() {
        if (disposed) return

        disposed = true
        context.dispose()
        stage.dispose()
    }

    private fun checkActive() {
        check(!disposed) {
            "StrataUi has already been disposed."
        }
    }
}
