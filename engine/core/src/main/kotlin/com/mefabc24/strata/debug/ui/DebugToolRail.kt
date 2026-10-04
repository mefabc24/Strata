package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.mefabc24.strata.debug.DebugToolMode

internal fun availableDebugToolModes(
    buildAvailable: Boolean,
    paintAvailable: Boolean,
    spawnAvailable: Boolean
): List<DebugToolMode> = buildList {
    add(DebugToolMode.NONE)
    add(DebugToolMode.INSPECT)
    add(DebugToolMode.MOVE)
    if (buildAvailable) add(DebugToolMode.BUILD)
    add(DebugToolMode.DELETE)
    if (paintAvailable) add(DebugToolMode.PAINT)
    if (spawnAvailable) add(DebugToolMode.SPAWN)
    add(DebugToolMode.PATHFINDING)
    add(DebugToolMode.FREE_CAMERA)
}

internal fun debugToolRailLabel(mode: DebugToolMode): String = when (mode) {
    DebugToolMode.FREE_CAMERA -> "Camera"
    else -> mode.displayName
}

internal fun debugToolFlyoutSectionTitles(mode: DebugToolMode): List<String> = when (mode) {
    DebugToolMode.NONE -> emptyList()
    DebugToolMode.BUILD -> listOf("Selection", "Preview", "Status")
    DebugToolMode.DELETE -> listOf("Brush", "Status")
    DebugToolMode.PAINT -> listOf("Brush", "Terrain", "Target", "Status")
    DebugToolMode.SPAWN -> listOf("Selection", "Preview", "Status")
    DebugToolMode.INSPECT -> listOf("Selection", "Entity control", "Visualization", "Status")
    DebugToolMode.MOVE -> listOf("Status")
    DebugToolMode.PATHFINDING ->
        listOf("Search", "Visualization", "Diagnostic search", "Traversal", "Status")
    DebugToolMode.FREE_CAMERA -> listOf("Movement", "Visualization", "Status")
}

internal class DebugToolRailState(
    private val configurableModes: Set<DebugToolMode>
) {
    var settingsMode: DebugToolMode? = null
        private set

    fun toggleSettings(mode: DebugToolMode): DebugToolMode? {
        settingsMode = when {
            mode !in configurableModes -> null
            settingsMode == mode -> null
            else -> mode
        }
        return settingsMode
    }

    fun closeSettings() {
        settingsMode = null
    }
}

internal class DebugToolRailButtonStyle(
    val normal: Drawable,
    val hovered: Drawable,
    val pressed: Drawable,
    val selected: Drawable,
    val selectedHovered: Drawable
)

internal class DebugToolRailButton(
    val mode: DebugToolMode,
    label: String,
    icon: Drawable,
    skin: Skin,
    private val style: DebugToolRailButtonStyle,
    private val onSelected: (DebugToolMode) -> Unit,
    private val onSettingsRequested: (DebugToolMode) -> Unit
) : Table(skin) {
    private var hovered = false
    private var pressedButton = NO_BUTTON

    var selected: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            updateBackground()
        }

    init {
        touchable = Touchable.enabled
        background = style.normal
        pad(5f, 3f, 4f, 3f)
        add(Image(icon).apply { setScaling(Scaling.fit) })
            .size(28f)
            .center()
        row()
        add(Label(label, skin, "debug-tool-label").apply {
            setAlignment(Align.center)
            setEllipsis(true)
        }).growX().minWidth(0f).height(18f).center()

        addListener(object : InputListener() {
            override fun touchDown(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ): Boolean {
                if (pointer != 0 || button !in SUPPORTED_BUTTONS) return false
                pressedButton = button
                updateBackground()
                event.stop()
                return true
            }

            override fun touchUp(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ) {
                val accepted = pointer == 0 && button == pressedButton &&
                    hit(x, y, true) != null
                pressedButton = NO_BUTTON
                updateBackground()
                event.stop()
                if (!accepted) return
                when (button) {
                    Input.Buttons.LEFT -> onSelected(mode)
                    Input.Buttons.RIGHT -> onSettingsRequested(mode)
                }
            }

            override fun enter(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                fromActor: Actor?
            ) {
                hovered = true
                updateBackground()
            }

            override fun exit(
                event: InputEvent,
                x: Float,
                y: Float,
                pointer: Int,
                toActor: Actor?
            ) {
                hovered = false
                if (pointer == -1) pressedButton = NO_BUTTON
                updateBackground()
            }
        })
    }

    private fun updateBackground() {
        background = when {
            pressedButton != NO_BUTTON -> style.pressed
            selected && hovered -> style.selectedHovered
            selected -> style.selected
            hovered -> style.hovered
            else -> style.normal
        }
    }

    private companion object {
        const val NO_BUTTON = -1
        val SUPPORTED_BUTTONS = setOf(Input.Buttons.LEFT, Input.Buttons.RIGHT)
    }
}

internal data class DebugToolFlyoutBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

internal fun debugToolFlyoutBounds(
    viewportWidth: Float,
    viewportHeight: Float,
    railRight: Float,
    anchorTop: Float,
    preferredWidth: Float,
    preferredHeight: Float,
    rightInset: Float = 0f,
    gap: Float = 8f,
    screenPadding: Float = 8f
): DebugToolFlyoutBounds {
    require(viewportWidth.isFinite() && viewportWidth >= 0f)
    require(viewportHeight.isFinite() && viewportHeight >= 0f)
    require(railRight.isFinite() && anchorTop.isFinite())
    require(preferredWidth.isFinite() && preferredWidth >= 0f)
    require(preferredHeight.isFinite() && preferredHeight >= 0f)
    require(rightInset.isFinite() && rightInset >= 0f)

    val x = (railRight + gap).coerceAtMost(viewportWidth)
    val maximumRight = (viewportWidth - rightInset - screenPadding).coerceAtLeast(x)
    val width = preferredWidth.coerceAtMost(maximumRight - x)
    val maximumHeight = (viewportHeight - screenPadding * 2f).coerceAtLeast(0f)
    val height = preferredHeight.coerceAtMost(maximumHeight)
    val maximumY = (viewportHeight - height - screenPadding).coerceAtLeast(0f)
    val minimumY = screenPadding.coerceAtMost(maximumY)
    val y = (anchorTop - height).coerceIn(minimumY, maximumY)
    return DebugToolFlyoutBounds(x, y, width, height)
}

internal fun Actor.topInStage(result: Vector2 = Vector2()): Vector2 =
    localToStageCoordinates(result.set(0f, height))
