package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.debug.DebugEventMonitor
import com.mefabc24.strata.debug.DebugEventMonitorSettings
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi

/** Bounded Event Bus Monitor content for the shared diagnostic overlay stack. */
internal class DebugEventMonitorOverlay(
    private val ui: StrataUi,
    private val monitor: DebugEventMonitor,
    private val settings: DebugEventMonitorSettings
) {
    private val records = Table(ui.skin).apply { top().left() }
    val panel = Table(ui.skin).apply {
        background = ui.skin.get(
            requireNotNull(ui.theme.panelStyle),
            StrataPanelStyle::class.java
        ).background
        pad(10f)
        add(Label("EVENT BUS MONITOR", ui.skin, "title").apply {
            setAlignment(Align.left)
        }).growX().fillX().left().padBottom(6f)
        row()
        add(ScrollPane(records, ui.skin).apply {
            setFadeScrollBars(false)
            touchable = Touchable.enabled
        }).height(260f).growX().fillX()
    }
    private var renderedKey: Any? = null

    fun sync() {
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
                background = ui.skin.get("debug-event-card", StrataPanelStyle::class.java).background
                pad(8f)
                top().left()
                add(Label("#${presentation.sequence}  ${presentation.eventType}", ui.skin, "title").apply {
                    setWrap(true)
                }).growX().fillX().left().padBottom(3f)
                row()
                add(DebugDiagnosticTable(ui.skin, wrapValues = true).apply {
                    show(presentation.fields)
                }).growX().fillX().left()
            }).growX().fillX().left().padBottom(6f)
            records.row()
        }
    }
}
