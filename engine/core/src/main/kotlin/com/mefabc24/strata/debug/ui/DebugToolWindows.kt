package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.ui.*

internal data class DebugToolRailUi(
    val actor: StrataPanel,
    val buttons: Map<DebugToolMode, DebugToolRailButton>
)

internal data class DebugToolFlyoutUi(
    val actor: StrataPanel,
    val scroll: StrataScrollPane,
    val header: Label
)

/** Builds Tool Rail and flyout actors while the panel owns their runtime state. */
internal class DebugToolWindowBuilder(
    private val ui: StrataUi,
    private val modes: List<DebugToolMode>,
    private val onToolSelected: (DebugToolMode) -> Unit,
    private val onSettingsRequested: (DebugToolMode) -> Unit
) {
    fun buildRail(): DebugToolRailUi {
        val buttons = linkedMapOf<DebugToolMode, DebugToolRailButton>()
        val style = ui.skin.get(
            "debug-tool-rail-button",
            DebugToolRailButtonStyle::class.java
        )
        val actor = ui.panel(
            styleName = null,
            spacing = 0f,
            padding = StrataInsets.NONE
        ) {
            scrollColumn(spacing = 0f, padding = StrataInsets.NONE) {
                defaults().fillAvailableX()
                modes.forEachIndexed { index, mode ->
                    val button = DebugToolRailButton(
                        mode = mode,
                        label = debugToolRailLabel(mode),
                        icon = ui.skin.getDrawable(debugToolIconName(mode)),
                        skin = ui.skin,
                        style = style,
                        onSelected = onToolSelected,
                        onSettingsRequested = onSettingsRequested
                    )
                    buttons[mode] = actor(button).cell {
                        width(DebugWindowLayout.TOOL_RAIL_WIDTH)
                        height(DebugWindowLayout.TOOL_BUTTON_HEIGHT)
                    }
                    if (index != modes.lastIndex) separator()
                }
            }.cell { grow(); minHeight(0f) }
        }
        actor.remove()
        return DebugToolRailUi(actor, buttons)
    }

    fun buildFlyout(
        configure: StrataColumn.() -> Unit
    ): DebugToolFlyoutUi {
        lateinit var header: Label
        lateinit var scroll: StrataScrollPane
        val actor = ui.panel(
            styleName = "debug-panel",
            spacing = 0f,
            padding = StrataInsets.NONE
        ) {
            defaults().fillAvailableX()
            header = label("", "title").cell {
                height(34f)
                padLeft(10f)
                padRight(10f)
                left()
            }
            separator()
            scroll = scrollColumn(
                spacing = 0f,
                padding = StrataInsets.NONE,
                configure = configure
            ).cell {
                growX()
                fillX()
                minHeight(0f)
                prefHeight(Value.prefHeight)
            }
        }
        actor.remove()
        actor.isVisible = false
        ui.stage.addActor(actor)
        return DebugToolFlyoutUi(actor, scroll, header)
    }
}
