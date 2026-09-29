package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.debug.DebugEventMonitor
import com.mefabc24.strata.debug.DebugEventMonitorSettings
import com.mefabc24.strata.debug.DebugEventOverlayPosition
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi

/** Dedicated bounded Event Bus diagnostics window. */
internal class DebugEventBusOverlay(
    private val ui: StrataUi,
    private val monitor: DebugEventMonitor,
    private val settings: DebugEventMonitorSettings
) {
    private val records = Table(ui.skin).apply { top().left() }
    private val panel = Table(ui.skin).apply {
        background = ui.skin.get(
            requireNotNull(ui.theme.panelStyle),
            StrataPanelStyle::class.java
        ).background
        pad(10f)
        add(Label("EVENT BUS", ui.skin, "title").apply {
            setAlignment(Align.left)
        }).growX().fillX().left().padBottom(6f)
        row()
        add(ScrollPane(records, ui.skin).apply {
            setFadeScrollBars(false)
            touchable = Touchable.enabled
        }).width(380f).height(480f).grow().fill()
    }
    private val root = Table().apply {
        setFillParent(true)
        pad(16f)
        touchable = Touchable.childrenOnly
        add(panel).width(400f).maxHeight(520f)
    }
    private var renderedKey: Any? = null

    init {
        ui.stage.addActor(root)
    }

    fun sync() {
        root.isVisible = settings.enabled && settings.overlayVisible
        applyPosition()
        if (!root.isVisible) return
        val ordered = if (settings.newestFirst) {
            monitor.records.asReversed()
        } else {
            monitor.records
        }.take(settings.maximumVisibleRecords)
        val key = listOf(
            ordered.map { it.sequence },
            settings.newestFirst,
            settings.maximumVisibleRecords
        )
        if (key == renderedKey) return
        renderedKey = key
        records.clearChildren()
        if (ordered.isEmpty()) {
            records.add(Label("No captured events", ui.skin)).growX().left()
            return
        }
        ordered.forEach { record ->
            val presentation = presentDebugEvent(record)
            records.add(Table(ui.skin).apply {
                top().left()
                add(Label("#${presentation.sequence}  ${presentation.eventType}", ui.skin, "title").apply {
                    setWrap(true)
                }).growX().fillX().left().padBottom(3f)
                row()
                add(DebugDiagnosticTable(ui.skin, wrapValues = true).apply {
                    show(presentation.fields)
                }).growX().fillX().left()
            }).growX().fillX().left().padBottom(10f)
            records.row()
        }
    }

    private fun applyPosition() {
        root.clearChildren()
        when (settings.overlayPosition) {
            DebugEventOverlayPosition.TOP_LEFT -> root.top().left()
            DebugEventOverlayPosition.TOP_RIGHT -> root.top().right()
            DebugEventOverlayPosition.BOTTOM_LEFT -> root.bottom().left()
            DebugEventOverlayPosition.BOTTOM_RIGHT -> root.bottom().right()
        }
        root.add(panel).width(400f).maxHeight(520f)
    }
}
