package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Value
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.world.World
import java.util.Locale

internal class DebugStatsOverlay(
    ui: StrataUi,
    private val stats: () -> RenderStats,
    private val performanceEnabled: () -> Boolean,
    private val worldStatsEnabled: () -> Boolean,
    private val world: World,
    private val placement: PlacementController?,
    private val framesPerSecond: () -> Int = { Gdx.graphics.framesPerSecond }
) {
    private val performanceState = DebugPerformanceOverlayState()
    private val state = DebugStatsOverlayState()
    private val performanceLabel = Label("", ui.skin, ui.theme.labelStyle).apply {
        setAlignment(Align.left)
        touchable = Touchable.disabled
    }
    private val worldLabel = Label("", ui.skin, ui.theme.labelStyle).apply {
        setAlignment(Align.left)
        touchable = Touchable.disabled
    }
    private lateinit var performanceCell: Cell<Label>
    private lateinit var worldCell: Cell<Label>
    private val content = Table(ui.skin).apply {
        background = ui.skin.get(
            requireNotNull(ui.theme.panelStyle),
            StrataPanelStyle::class.java
        ).background
        pad(8f)
        touchable = Touchable.disabled
        performanceCell = add(performanceLabel).left()
        row()
        worldCell = add(worldLabel).left()
    }
    private var worldElapsed = 0f
    private val root = Table().apply {
        setFillParent(true)
        top().right()
        pad(16f)
        touchable = Touchable.disabled
        isVisible = false
        add(content)
    }

    init { ui.stage.addActor(root) }

    fun update(delta: Float) {
        val performance = performanceEnabled()
        val worldStats = worldStatsEnabled()
        state.sync(performance, worldStats)
        root.isVisible = state.visible
        performanceLabel.isVisible = state.performanceVisible
        worldLabel.isVisible = state.worldVisible
        if (performance) performanceCell.height(Value.prefHeight)
        else performanceCell.height(0f)
        if (worldStats) worldCell.height(Value.prefHeight)
        else worldCell.height(0f)
        worldCell.padTop(if (performance && worldStats) 8f else 0f)
        content.invalidateHierarchy()

        val average = performanceState.update(performance, delta)
        if (average != null) {
            performanceLabel.setText(
                DebugPerformanceSnapshot.from(stats(), framesPerSecond(), average).format()
            )
        }
        if (!performance) performanceLabel.setText("")

        if (!worldStats) {
            worldElapsed = 0f
            worldLabel.setText("")
        } else {
            worldElapsed += delta
            if (state.worldBecameVisible || worldElapsed >= 0.25f) {
                worldElapsed = 0f
                worldLabel.setText(
                    DebugWorldStatsSnapshot.from(world, placement, stats()).format()
                )
            }
        }
    }
}

/** Visibility model for independently enabled sections in the shared overlay. */
class DebugStatsOverlayState {
    var performanceVisible: Boolean = false
        private set
    var worldVisible: Boolean = false
        private set
    var worldBecameVisible: Boolean = false
        private set
    val visible: Boolean get() = performanceVisible || worldVisible

    fun sync(performanceEnabled: Boolean, worldEnabled: Boolean) {
        worldBecameVisible = worldEnabled && !worldVisible
        performanceVisible = performanceEnabled
        worldVisible = worldEnabled
    }
}

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
    fun format(): String = buildString {
        append("World: $width x $height")
        append("\nGround: $groundTiles")
        append("\nOverlays: $overlayTiles ($overlayLayers layers)")
        append("\nObjects: $objects ($objectsDrawn drawn)")
        append("\nEntities: $entities ($entitiesDrawn drawn)")
        append("\nMoving: $movingEntities")
        append("\nActive paths: $activePaths")
        append("\nPlacement previews: $placementPreviews")
    }

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
    fun format(): String = buildString {
        append("FPS: $framesPerSecond")
        append("\nFrame: ${ms(averageFrameMs)} ms")
        append("\nRender: ${ms(renderMs)} ms")
        append("\nPlan: ${ms(dynamicPlanMs)} ms")
        if (staticPlanUpdates > 0) {
            append("\nStatic plan: ${ms(staticPlanMs)} ms")
            append("\nStatic updates: $staticPlanUpdates")
        }
        append("\nDraw calls: $drawCalls")
        append("\n\nGround: $groundTerrainDrawn/$groundTerrainTotal")
        append("\nOverlays: $overlayTerrainDrawn/$overlayTerrainTotal")
        append("\nChecks: $terrainChecked")
        append("\nObjects: $objectsDrawn/$objectsTotal")
        append("\nEntities: $entitiesDrawn/$entitiesTotal")
        append("\nPreviews: $previewsDrawn")
    }

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
