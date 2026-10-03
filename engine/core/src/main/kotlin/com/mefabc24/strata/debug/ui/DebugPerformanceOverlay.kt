package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.debug.DebugEventMonitor
import com.mefabc24.strata.debug.DebugEventMonitorSettings
import com.mefabc24.strata.debug.DebugPerformanceSettings
import com.mefabc24.strata.debug.performanceSparkline
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.world.World
import java.util.Locale

internal class DebugStatsOverlay(
    ui: StrataUi,
    private val stats: () -> RenderStats,
    private val performanceSettings: DebugPerformanceSettings,
    private val performanceEnabled: () -> Boolean,
    private val worldStatsEnabled: () -> Boolean,
    private val world: World,
    private val placement: PlacementController?,
    private val eventMonitor: DebugEventMonitor,
    private val eventSettings: DebugEventMonitorSettings,
    private val framesPerSecond: () -> Int = { Gdx.graphics.framesPerSecond }
) {
    private val performanceState = DebugPerformanceOverlayState()
    private val state = DebugStatsOverlayState()
    private val performanceRows = statsRows(ui)
    private val worldRows = statsRows(ui)
    private val performanceGraph = Label("No captured samples", ui.skin).apply {
        setAlignment(Align.left)
        touchable = Touchable.disabled
    }
    private val performancePanel = statsPanel(
        "PERFORMANCE", performanceRows, ui, performanceGraph
    )
    private val worldPanel = statsPanel("WORLD", worldRows, ui)
    private val eventMonitorPanel = DebugEventMonitorOverlay(ui, eventMonitor, eventSettings)
    private val performanceCell: Cell<Table>
    private val worldCell: Cell<Table>
    private val eventMonitorCell: Cell<Table>
    private var worldElapsed = 0f
    private val root = Table()
    private var topPadding = Float.NaN
    private var leftPadding = Float.NaN

    init {
        root.apply {
            setFillParent(true)
            top().left()
            pad(16f)
            touchable = Touchable.childrenOnly
            isVisible = false
            performanceCell = add(performancePanel)
                .minWidth(250f).prefWidth(290f).maxWidth(320f)
                .fillX().left()
            row()
            worldCell = add(worldPanel)
                .minWidth(250f).prefWidth(290f).maxWidth(320f)
                .fillX().left()
            row()
            eventMonitorCell = add(eventMonitorPanel.panel)
                .minWidth(250f).prefWidth(290f).maxWidth(320f)
                .fillX().left()
        }
        ui.stage.addActor(root)
    }

    /** Keeps the left overlay stack below a visible tools window. */
    fun setTopPadding(padding: Float) {
        require(padding.isFinite() && padding >= 0f)
        if (padding == topPadding) return
        topPadding = padding
        root.padTop(padding)
        root.invalidateHierarchy()
    }

    /** Keeps the left overlay stack clear of the tool rail and its flyout. */
    fun setLeftPadding(padding: Float) {
        require(padding.isFinite() && padding >= 0f)
        if (padding == leftPadding) return
        leftPadding = padding
        root.padLeft(padding)
        root.invalidateHierarchy()
    }

    fun update(delta: Float) {
        val performance = performanceEnabled()
        val worldStats = worldStatsEnabled()
        val eventMonitor = eventSettings.enabled
        state.sync(performance, worldStats, eventMonitor)
        root.isVisible = state.visible
        performancePanel.isVisible = state.performanceVisible
        worldPanel.isVisible = state.worldVisible
        eventMonitorPanel.panel.isVisible = state.eventMonitorVisible
        if (performance) performanceCell.height(Value.prefHeight)
        else performanceCell.height(0f)
        if (worldStats) worldCell.height(Value.prefHeight)
        else worldCell.height(0f)
        if (eventMonitor) eventMonitorCell.height(Value.prefHeight)
        else eventMonitorCell.height(0f)
        worldCell.padTop(if (performance && worldStats) 8f else 0f)
        eventMonitorCell.padTop(if (eventMonitor && (performance || worldStats)) 8f else 0f)
        root.invalidateHierarchy()

        val average = performanceState.update(performance, delta)
        if (average != null) {
            val summary = performanceSettings.history.summary(performanceSettings.historyMetric)
            performanceRows.show(
                buildList {
                    addAll(DebugPerformanceSnapshot.from(stats(), framesPerSecond(), average).rows())
                    add(DebugDiagnosticRow(
                        "Capture",
                        if (performanceSettings.historyRecording) "Recording" else "Stopped"
                    ))
                    add(DebugDiagnosticRow(
                        "History metric",
                        performanceSettings.historyMetric.name.replace('_', ' ').lowercase()
                    ))
                    add(DebugDiagnosticRow("Samples", performanceSettings.history.size.toString()))
                    summary?.let {
                        add(DebugDiagnosticRow("History avg", "${ms(it.averageMs)} ms"))
                        add(DebugDiagnosticRow("History min", "${ms(it.minimumMs)} ms"))
                        add(DebugDiagnosticRow("History max", "${ms(it.maximumMs)} ms"))
                    }
                }
            )
            performanceGraph.setText(
                performanceSparkline(
                    performanceSettings.history.samples(
                        performanceSettings.historyMetric,
                        maximumSamples = 48
                    )
                )
            )
        }
        if (!performance) {
            performanceRows.show(emptyList())
            performanceGraph.setText("")
        }

        if (!worldStats) {
            worldElapsed = 0f
            worldRows.show(emptyList())
        } else {
            worldElapsed += delta
            if (state.worldBecameVisible || worldElapsed >= 0.25f) {
                worldElapsed = 0f
                worldRows.show(
                    DebugWorldStatsSnapshot.from(world, placement, stats()).rows()
                )
            }
        }
        if (eventMonitor) eventMonitorPanel.sync()
    }
}

private fun statsRows(ui: StrataUi): DebugDiagnosticTable = DebugDiagnosticTable(
    skin = ui.skin,
    wrapValues = false,
    keyMinimumWidth = 112f,
    valueMinimumWidth = 96f
)

private fun statsPanel(
    title: String,
    rows: DebugDiagnosticTable,
    ui: StrataUi,
    footer: com.badlogic.gdx.scenes.scene2d.Actor? = null
): Table = Table(ui.skin).apply {
    background = ui.skin.get(
        requireNotNull(ui.theme.panelStyle),
        StrataPanelStyle::class.java
    ).background
    pad(10f)
    touchable = Touchable.disabled
    add(Label(title, ui.skin, "title").apply {
        setAlignment(Align.left)
        touchable = Touchable.disabled
    }).growX().fillX().left().padBottom(6f)
    row()
    add(rows).growX().fillX().left()
    footer?.let {
        row()
        add(it).growX().fillX().left().padTop(6f)
    }
}

private fun ms(value: Double) = String.format(Locale.ROOT, "%.2f", value)

/** Visibility model for independently enabled sections in the shared overlay. */
class DebugStatsOverlayState {
    var performanceVisible: Boolean = false
        private set
    var worldVisible: Boolean = false
        private set
    var worldBecameVisible: Boolean = false
        private set
    var eventMonitorVisible: Boolean = false
        private set
    val visible: Boolean get() = performanceVisible || worldVisible || eventMonitorVisible
    val visibleSections: List<DebugStatsSection>
        get() = buildList {
            if (performanceVisible) add(DebugStatsSection.PERFORMANCE)
            if (worldVisible) add(DebugStatsSection.WORLD)
            if (eventMonitorVisible) add(DebugStatsSection.EVENT_BUS_MONITOR)
        }

    fun sync(
        performanceEnabled: Boolean,
        worldEnabled: Boolean,
        eventMonitorEnabled: Boolean
    ) {
        worldBecameVisible = worldEnabled && !worldVisible
        performanceVisible = performanceEnabled
        worldVisible = worldEnabled
        eventMonitorVisible = eventMonitorEnabled
    }

    fun sync(performanceEnabled: Boolean, worldEnabled: Boolean) {
        sync(performanceEnabled, worldEnabled, eventMonitorEnabled = false)
    }
}

enum class DebugStatsSection { PERFORMANCE, WORLD, EVENT_BUS_MONITOR }

data class DebugWorldStatsSnapshot(
    val width: Int,
    val height: Int,
    val groundTiles: Int,
    val overlayTiles: Int,
    val overlayLayers: Int,
    val objects: Int,
    val objectsDrawn: Int,
    val entities: Int,
    val entitiesDrawn: Int,
    val movingEntities: Int,
    val activePaths: Int,
    val placementPreviews: Int
) {
    fun rows(): List<DebugDiagnosticRow> = diagnosticRows(
        "World" to "$width x $height",
        "Ground" to groundTiles.toString(),
        "Overlays" to "$overlayTiles ($overlayLayers layers)",
        "Objects" to "$objects ($objectsDrawn drawn)",
        "Entities" to "$entities ($entitiesDrawn drawn)",
        "Moving" to movingEntities.toString(),
        "Active paths" to activePaths.toString(),
        "Placement previews" to placementPreviews.toString()
    )

    fun format(): String = rows().joinToString("\n") { "${it.key}: ${it.value}" }

    companion object {
        fun from(
            world: World,
            placement: PlacementController?,
            stats: RenderStats
        ): DebugWorldStatsSnapshot {
            val entities = world.getEntities()
            return DebugWorldStatsSnapshot(
                width = world.width,
                height = world.height,
                groundTiles = world.groundTileCount,
                overlayTiles = world.overlayTileCount,
                overlayLayers = world.overlayLayerIds.size,
                objects = world.placedObjectCount,
                objectsDrawn = stats.objectsDrawn,
                entities = world.entityCount,
                entitiesDrawn = stats.entitiesDrawn,
                movingEntities = entities.count { it.isMoving },
                activePaths = entities.count { it.remainingPath.isNotEmpty() },
                placementPreviews = placement?.previews?.size ?: 0
            )
        }
    }
}

class DebugPerformanceOverlayState(private val refreshInterval: Float = 0.25f) {
    var visible: Boolean = false
        private set
    private var elapsed = 0f
    private var accumulatedFrameMs = 0.0
    private var sampledFrames = 0

    init {
        require(refreshInterval.isFinite() && refreshInterval > 0f) {
            "Refresh interval must be finite and greater than zero."
        }
    }

    fun update(enabled: Boolean, delta: Float): Double? {
        if (!enabled) {
            visible = false
            reset()
            return null
        }
        require(delta.isFinite() && delta >= 0f) {
            "Frame delta must be finite and non-negative."
        }
        val becameVisible = !visible
        visible = true
        elapsed += delta
        accumulatedFrameMs += delta * 1000.0
        sampledFrames++
        if (!becameVisible && elapsed < refreshInterval) return null
        return (accumulatedFrameMs / sampledFrames).also { reset() }
    }

    private fun reset() {
        elapsed = 0f
        accumulatedFrameMs = 0.0
        sampledFrames = 0
    }
}

data class DebugPerformanceSnapshot(
    val framesPerSecond: Int,
    val averageFrameMs: Double,
    val renderMs: Double,
    val dynamicPlanMs: Double,
    val drawCalls: Int,
    val groundTerrainDrawn: Int,
    val groundTerrainTotal: Int,
    val overlayTerrainDrawn: Int,
    val overlayTerrainTotal: Int,
    val terrainChecked: Int,
    val objectsDrawn: Int,
    val objectsTotal: Int,
    val entitiesDrawn: Int,
    val entitiesTotal: Int,
    val previewsDrawn: Int,
    val staticPlanMs: Double,
    val staticPlanUpdates: Int
) {
    fun rows(): List<DebugDiagnosticRow> = buildList {
        add(DebugDiagnosticRow("FPS", framesPerSecond.toString()))
        add(DebugDiagnosticRow("Frame", "${ms(averageFrameMs)} ms"))
        add(DebugDiagnosticRow("Render", "${ms(renderMs)} ms"))
        add(DebugDiagnosticRow("Plan", "${ms(dynamicPlanMs)} ms"))
        if (staticPlanUpdates > 0) {
            add(DebugDiagnosticRow("Static plan", "${ms(staticPlanMs)} ms"))
            add(DebugDiagnosticRow("Static updates", staticPlanUpdates.toString()))
        }
        add(DebugDiagnosticRow("Draw calls", drawCalls.toString()))
        add(DebugDiagnosticRow("Ground", "$groundTerrainDrawn/$groundTerrainTotal"))
        add(DebugDiagnosticRow("Overlays", "$overlayTerrainDrawn/$overlayTerrainTotal"))
        add(DebugDiagnosticRow("Checks", terrainChecked.toString()))
        add(DebugDiagnosticRow("Objects", "$objectsDrawn/$objectsTotal"))
        add(DebugDiagnosticRow("Entities", "$entitiesDrawn/$entitiesTotal"))
        add(DebugDiagnosticRow("Previews", previewsDrawn.toString()))
    }

    fun format(): String = rows().joinToString("\n") { "${it.key}: ${it.value}" }

    companion object {
        fun from(stats: RenderStats, framesPerSecond: Int, averageFrameMs: Double) =
            DebugPerformanceSnapshot(
                framesPerSecond, averageFrameMs, stats.cpuRenderMs,
                stats.dynamicPlanMs, stats.drawCalls, stats.groundTerrainDrawn,
                stats.groundTerrainTotal, stats.overlayTerrainDrawn,
                stats.overlayTerrainTotal, stats.terrainChecked, stats.objectsDrawn,
                stats.objectsTotal, stats.entitiesDrawn, stats.entitiesTotal,
                stats.previewsDrawn, stats.staticPlanMs, stats.staticPlanUpdates
            )

        private fun ms(value: Double) = String.format(Locale.ROOT, "%.2f", value)
    }
}
