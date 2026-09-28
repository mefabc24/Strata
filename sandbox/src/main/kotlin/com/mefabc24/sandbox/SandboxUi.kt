package com.mefabc24.sandbox

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.render.`object`.ObjectEntry
import com.mefabc24.strata.scene.DebugGridExtent
import com.mefabc24.strata.scene.DebugGridRenderLayer
import com.mefabc24.strata.scene.DebugSettings
import com.mefabc24.strata.simulation.SimulationController
import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.ui.StrataColumn
import com.mefabc24.strata.ui.StrataInsets
import com.mefabc24.strata.ui.StrataLayout
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataSeparatorStyle
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.StrataUiTheme
import com.mefabc24.strata.ui.cell
import com.mefabc24.strata.ui.fillAvailableX
import com.mefabc24.strata.ui.fixedSize
import com.mefabc24.strata.ui.fixedWidth
import com.mefabc24.strata.ui.hugX
import java.util.Locale

private enum class SandboxPanelTab(
    val displayName: String
) {
    TOOLS("Tools"),
    DEBUG("Debug")
}

private sealed interface SandboxPaintLayer {
    val layerId: String?

    data object Ground : SandboxPaintLayer {
        override val layerId: String? = null
    }

    data class Overlay(
        override val layerId: String
    ) : SandboxPaintLayer

    val displayName: String
        get() = when (this) {
            Ground -> "Ground"
            is Overlay -> layerId.toDisplayName()
        }
}

private fun String.toDisplayName(): String {
    return replace('-', ' ')
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar {
            it.titlecase()
        }
}

private fun paintLayersFor(
    overlayLayerIds: List<String>
): List<SandboxPaintLayer> {
    return buildList {
        add(SandboxPaintLayer.Ground)

        for (id in overlayLayerIds) {
            add(SandboxPaintLayer.Overlay(id))
        }
    }
}

class SandboxUi(
    private val ui: StrataUi,
    private val painter: SandboxTerrainPainter,
    private val placementController: PlacementController,
    private val buildDragController: SandboxBuildDragController,
    private val toolController: SandboxToolController,
    private val entitySpawner: SandboxEntitySpawner,
    private val debugSettings: DebugSettings,
    simulation: SimulationController,
    renderStats: () -> RenderStats,
    terrainEntries: List<TerrainEntry>,
    objectEntries: List<ObjectEntry>
) {

    private val terrains: List<TerrainType> =
        terrainEntries.map { entry ->
            entry.type as? TerrainType
                ?: error(
                    "Sandbox terrain registry contains an unsupported terrain ID: ${entry.type}"
                )
        }.also {
            require(it.isNotEmpty()) {
                "The Sandbox UI requires at least one registered terrain."
            }
        }

    private val buildEntries = objectEntries.toList().also {
        require(it.isNotEmpty()) {
            "The Sandbox UI requires at least one constructible object."
        }

        require(it.all(ObjectEntry::isConstructible)) {
            "All Sandbox build entries must have a registered factory."
        }

        require(it.all(ObjectEntry::isPrepared)) {
            "All Sandbox build entries must be prepared."
        }
    }

    private val paintLayers = paintLayersFor(painter.overlayLayerIds)

    private val performanceOverlay = SandboxPerformanceOverlay(
        ui = ui,
        stats = renderStats,
        enabled = { debugSettings.performance.enabled }
    )

    private val simulationOverlay = SandboxSimulationOverlay(
        ui = ui,
        simulation = simulation
    )

    private val tabSelection = ui.selectionGroup(
        options = SandboxPanelTab.entries,
        initialSelection = SandboxPanelTab.TOOLS
    ) {
        updatePanelTab()
    }

    private val modeSelection = ui.selectionGroup(
        options = SandboxMode.entries,
        initialSelection = toolController.mode
    ) { selected ->
        toolController.select(selected)
        updateStatus()
    }

    private val terrainSelection = ui.selectionGroup(
        options = terrains,
        initialSelection = painter.terrain.takeIf { it in terrains }
            ?: terrains.first()
    ) { selected ->
        painter.terrain = selected
        updateStatus()
    }

    private val layerSelection = ui.selectionGroup(
        options = paintLayers,
        initialSelection = paintLayers.first {
            it.layerId == painter.layerId
        }
    ) { selected ->
        painter.layerId = selected.layerId
        updateStatus()
    }

    private val buildSelection = ui.selectionGroup(
        options = buildEntries,
        initialSelection = buildEntries.firstOrNull { entry ->
            placementController.selectedPlaceable?.let(entry.type::isInstance)
                ?: false
        } ?: buildEntries.first()
    ) { selected ->
        buildDragController.cancel()
        placementController.selectedFactory = selected::create
        updateStatus()
    }

    private val spawnSelection = ui.selectionGroup(
        options = entitySpawner.entries,
        initialSelection = entitySpawner.selectedEntry
    ) { selected ->
        entitySpawner.selectedEntry = selected
        updateStatus()
    }

    private val renderLayerSelection = ui.selectionGroup(
        options = DebugGridRenderLayer.entries,
        initialSelection = debugSettings.grid.renderLayer
    ) { selected ->
        debugSettings.grid.renderLayer = selected
    }

    private val gridExtentSelection = ui.selectionGroup(
        options = DebugGridExtent.entries,
        initialSelection = debugSettings.grid.extent
    ) { selected ->
        debugSettings.grid.extent = selected
    }

    private var gridBackgroundColor =
        debugSettings.grid.backgroundColor ?: Color.WHITE.cpy().apply {
            a = DEFAULT_BACKGROUND_ALPHA
        }

    private var hoverBackgroundColor =
        debugSettings.grid.hoverBackgroundColor ?: Color.WHITE.cpy().apply {
            a = DEFAULT_BACKGROUND_ALPHA
        }

    private var objectTileFillColor =
        debugSettings.objects.occupiedTileFillColor
            ?: debugSettings.objects.occupiedTileColor.apply {
                a = DEFAULT_BACKGROUND_ALPHA
            }

    private var entityTileFillColor =
        debugSettings.entities.currentTileFillColor
            ?: debugSettings.entities.currentTileColor.apply {
                a = DEFAULT_BACKGROUND_ALPHA
            }

    private lateinit var toolsControls: StrataColumn
    private lateinit var debugControls: StrataColumn
    private lateinit var buildControls: StrataColumn
    private lateinit var paintControls: StrataColumn
    private lateinit var spawnControls: StrataColumn
    private lateinit var gridDebugControls: StrataColumn
    private lateinit var objectDebugControls: StrataColumn
    private lateinit var entityDebugControls: StrataColumn
    private lateinit var gridDebugCell: Cell<StrataColumn>
    private lateinit var objectDebugCell: Cell<StrataColumn>
    private lateinit var entityDebugCell: Cell<StrataColumn>
    private lateinit var modeStatus: Label
    private lateinit var selectionStatus: Label

    init {
        painter.terrain = checkNotNull(terrainSelection.selected)

        val selectedBuildEntry = checkNotNull(buildSelection.selected)
        if (
            placementController.selectedPlaceable?.let(
                selectedBuildEntry.type::isInstance
            ) != true
        ) {
            placementController.selectedFactory = selectedBuildEntry::create
        }

        buildUi()
        sync()
    }

    private fun buildUi() {
        ui.root.pad(ROOT_MARGIN)

        ui.panel(spacing = 0f) {
            defaults().fillAvailableX()

            expander(
                title = "Strata tools",
                spacing = SECTION_GAP
            ) {
                defaults().fillAvailableX()

                row(spacing = CONTROL_GAP) {
                    defaults()
                        .fillAvailableX()
                        .uniformX()
                        .height(MODE_BUTTON_HEIGHT)

                    for (tab in SandboxPanelTab.entries) {
                        selectableButton(
                            text = tab.displayName,
                            value = tab,
                            group = tabSelection
                        )
                    }
                }

                separator()

                stack {
                    toolsControls = column(spacing = SECTION_GAP) {
                        defaults().fillAvailableX()
                        buildToolsTab()
                    }

                    debugControls = column(spacing = SECTION_GAP) {
                        defaults().fillAvailableX()
                        buildDebugTab()
                    }
                }.cell {
                    fillAvailableX()
                }
            }.cell {
                fillAvailableX()
            }
        }.cell {
            minWidth(TOOLBAR_MIN_WIDTH)
            hugX()
            top()
            left()
        }
    }

    private fun StrataColumn.buildToolsTab() {
        label("Mode")

        row(spacing = CONTROL_GAP) {
            defaults()
                .fillAvailableX()
                .uniformX()
                .height(MODE_BUTTON_HEIGHT)

            for (mode in SandboxMode.entries) {
                selectableButton(
                    text = mode.displayName,
                    value = mode,
                    group = modeSelection
                )
            }
        }

        stack {
            buildControls = column(spacing = SECTION_GAP) {
                defaults().fillAvailableX()
                label("Build object")

                grid(
                    columns = BUILD_COLUMNS,
                    spacing = CONTROL_GAP,
                    alignment = Align.center
                ) {
                    defaults().fillAvailableX().uniformX()

                    for (entry in buildEntries) {
                        column(
                            spacing = CONTROL_GAP,
                            alignment = Align.center
                        ) {
                            defaults().fillAvailableX()

                            selectableImageButton(
                                drawable = TextureRegionDrawable(
                                    entry.selectionVisual.texture
                                ),
                                value = entry,
                                group = buildSelection
                            ).apply {
                                image.setScaling(Scaling.fit)
                                imageCell.pad(IMAGE_PADDING)
                            }.cell {
                                minWidth(BUILD_BUTTON_MIN_WIDTH)
                                fillAvailableX()
                                height(BUILD_BUTTON_HEIGHT)
                            }

                            label(entry.displayName()).cell {
                                center()
                            }
                        }.cell {
                            fillAvailableX()
                        }
                    }
                }
            }

            paintControls = column(spacing = SECTION_GAP) {
                defaults().fillAvailableX()
                label("Terrain")

                grid(
                    columns = TERRAIN_COLUMNS,
                    spacing = CONTROL_GAP,
                    alignment = Align.center
                ) {
                    defaults()
                        .fillAvailableX()
                        .uniformX()
                        .height(COMPACT_CONTROL_HEIGHT)

                    for (terrain in terrains) {
                        selectableButton(
                            text = terrain.displayName(),
                            value = terrain,
                            group = terrainSelection
                        )
                    }
                }

                label("Layer")

                grid(
                    columns = LAYER_COLUMNS,
                    spacing = CONTROL_GAP,
                    alignment = Align.center
                ) {
                    defaults()
                        .fillAvailableX()
                        .uniformX()
                        .height(COMPACT_CONTROL_HEIGHT)

                    for (layer in paintLayers) {
                        selectableButton(
                            text = layer.displayName,
                            value = layer,
                            group = layerSelection
                        )
                    }
                }
            }

            spawnControls = column(spacing = SECTION_GAP) {
                defaults().fillAvailableX()
                label("Spawn entity")

                grid(
                    columns = SPAWN_COLUMNS,
                    spacing = CONTROL_GAP,
                    alignment = Align.center
                ) {
                    defaults()
                        .fillAvailableX()
                        .uniformX()
                        .height(COMPACT_CONTROL_HEIGHT)

                    for (entry in entitySpawner.entries) {
                        selectableButton(
                            text = entry.name,
                            value = entry,
                            group = spawnSelection
                        )
                    }
                }
            }
        }.cell {
            fillAvailableX()
        }

        separator()
        label("Status", styleName = "title")
        modeStatus = label("")
        selectionStatus = label("")
    }

    private fun StrataColumn.buildDebugTab() {
        label("Runtime debug", styleName = "title")

        row(spacing = CONTROL_GAP) {
            defaults()
                .fillAvailableX()
                .uniformX()
                .height(MODE_BUTTON_HEIGHT)

            toggleButton(
                text = "Performance",
                checked = debugSettings.performance.enabled
            ) { enabled ->
                debugSettings.performance.enabled = enabled
            }

            toggleButton(
                text = "Simulation"
            ) { enabled ->
                simulationOverlay.setVisible(enabled)
            }
        }

        row(spacing = CONTROL_GAP) {
            defaults()
                .fillAvailableX()
                .uniformX()
                .height(MODE_BUTTON_HEIGHT)

            toggleButton(
                text = "Grid",
                checked = debugSettings.grid.enabled
            ) { enabled ->
                debugSettings.grid.enabled = enabled
                updateDebugVisibility()
            }

            toggleButton(
                text = "Objects",
                checked = debugSettings.objects.enabled
            ) { enabled ->
                debugSettings.objects.enabled = enabled
                updateDebugVisibility()
            }
        }

        toggleButton(
            text = "Entities",
            checked = debugSettings.entities.enabled
        ) { enabled ->
            debugSettings.entities.enabled = enabled
            updateDebugVisibility()
        }.cell {
            fillAvailableX()
            height(MODE_BUTTON_HEIGHT)
        }

        gridDebugControls = column(spacing = SECTION_GAP) {
            defaults().fillAvailableX()
            label("Grid", styleName = "title")
            label("Render layer")

            row(spacing = CONTROL_GAP) {
                defaults()
                    .fillAvailableX()
                    .uniformX()
                    .height(COMPACT_CONTROL_HEIGHT)

                selectableButton(
                    text = "Below objects",
                    value = DebugGridRenderLayer.BELOW_OBJECTS,
                    group = renderLayerSelection
                )

                selectableButton(
                    text = "Above objects",
                    value = DebugGridRenderLayer.ABOVE_OBJECTS,
                    group = renderLayerSelection
                )
            }

            label("Extent")

            row(spacing = CONTROL_GAP) {
                defaults()
                    .fillAvailableX()
                    .uniformX()
                    .height(COMPACT_CONTROL_HEIGHT)

                selectableButton(
                    text = "World",
                    value = DebugGridExtent.WORLD,
                    group = gridExtentSelection
                )

                selectableButton(
                    text = "Visible",
                    value = DebugGridExtent.VISIBLE,
                    group = gridExtentSelection
                )
            }

            numericControl(
                label = "Line width",
                initialValue = debugSettings.grid.lineWidth,
                step = 0.25f,
                range = 0.25f..8f
            ) { value ->
                debugSettings.grid.lineWidth = value
            }

            numericControl(
                label = "Grid alpha",
                initialValue = debugSettings.grid.color.a,
                step = ALPHA_STEP,
                range = ALPHA_RANGE
            ) { value ->
                debugSettings.grid.color = debugSettings.grid.color.apply {
                    a = value
                }
            }

            numericControl(
                label = "Hover alpha",
                initialValue = debugSettings.grid.hoverColor.a,
                step = ALPHA_STEP,
                range = ALPHA_RANGE
            ) { value ->
                debugSettings.grid.hoverColor =
                    debugSettings.grid.hoverColor.apply {
                        a = value
                    }
            }

            row(spacing = CONTROL_GAP) {
                defaults()
                    .fillAvailableX()
                    .uniformX()
                    .height(COMPACT_CONTROL_HEIGHT)

                toggleButton(
                    text = "Background",
                    checked = debugSettings.grid.backgroundColor != null
                ) { enabled ->
                    if (enabled) {
                        debugSettings.grid.backgroundColor = gridBackgroundColor
                    } else {
                        debugSettings.grid.backgroundColor?.let {
                            gridBackgroundColor = it
                        }
                        debugSettings.grid.backgroundColor = null
                    }
                }

                toggleButton(
                    text = "Hover background",
                    checked = debugSettings.grid.hoverBackgroundColor != null
                ) { enabled ->
                    if (enabled) {
                        debugSettings.grid.hoverBackgroundColor =
                            hoverBackgroundColor
                    } else {
                        debugSettings.grid.hoverBackgroundColor?.let {
                            hoverBackgroundColor = it
                        }
                        debugSettings.grid.hoverBackgroundColor = null
                    }
                }
            }

            numericControl(
                label = "Background alpha",
                initialValue = gridBackgroundColor.a,
                step = ALPHA_STEP,
                range = ALPHA_RANGE
            ) { value ->
                gridBackgroundColor = gridBackgroundColor.apply {
                    a = value
                }

                if (debugSettings.grid.backgroundColor != null) {
                    debugSettings.grid.backgroundColor = gridBackgroundColor
                }
            }

            numericControl(
                label = "Hover-bg alpha",
                initialValue = hoverBackgroundColor.a,
                step = ALPHA_STEP,
                range = ALPHA_RANGE
            ) { value ->
                hoverBackgroundColor = hoverBackgroundColor.apply {
                    a = value
                }

                if (debugSettings.grid.hoverBackgroundColor != null) {
                    debugSettings.grid.hoverBackgroundColor =
                        hoverBackgroundColor
                }
            }
        }.cell {
            fillAvailableX()
        }
        gridDebugCell = checkNotNull(getCell(gridDebugControls))

        objectDebugControls = column(spacing = SECTION_GAP) {
            defaults().fillAvailableX()
            label("Objects", styleName = "title")

            row(spacing = CONTROL_GAP) {
                defaults().fillAvailableX().uniformX()
                    .height(COMPACT_CONTROL_HEIGHT)
                toggleButton(
                    text = "Occupied tiles",
                    checked = debugSettings.objects.showOccupiedTiles
                ) { debugSettings.objects.showOccupiedTiles = it }
                toggleButton(
                    text = "Origin tile",
                    checked = debugSettings.objects.showOriginTile
                ) { debugSettings.objects.showOriginTile = it }
            }

            row(spacing = CONTROL_GAP) {
                defaults().fillAvailableX().uniformX()
                    .height(COMPACT_CONTROL_HEIGHT)
                toggleButton(
                    text = "Sprite bounds",
                    checked = debugSettings.objects.showSpriteBounds
                ) { debugSettings.objects.showSpriteBounds = it }
                toggleButton(
                    text = "Tile fill",
                    checked = debugSettings.objects.occupiedTileFillColor != null
                ) { enabled ->
                    if (enabled) {
                        debugSettings.objects.occupiedTileFillColor =
                            objectTileFillColor
                    } else {
                        debugSettings.objects.occupiedTileFillColor?.let {
                            objectTileFillColor = it
                        }
                        debugSettings.objects.occupiedTileFillColor = null
                    }
                }
            }

            numericControl(
                label = "Line width",
                initialValue = debugSettings.objects.lineWidth,
                step = 0.25f,
                range = 0.25f..8f
            ) { debugSettings.objects.lineWidth = it }

            numericControl(
                label = "Fill alpha",
                initialValue = objectTileFillColor.a,
                step = ALPHA_STEP,
                range = ALPHA_RANGE
            ) { value ->
                objectTileFillColor = objectTileFillColor.apply { a = value }
                if (debugSettings.objects.occupiedTileFillColor != null) {
                    debugSettings.objects.occupiedTileFillColor =
                        objectTileFillColor
                }
            }
        }.cell {
            fillAvailableX()
        }
        objectDebugCell = checkNotNull(getCell(objectDebugControls))

        entityDebugControls = column(spacing = SECTION_GAP) {
            defaults().fillAvailableX()
            label("Entities", styleName = "title")

            row(spacing = CONTROL_GAP) {
                defaults().fillAvailableX().uniformX()
                    .height(COMPACT_CONTROL_HEIGHT)
                toggleButton(
                    text = "Current tile",
                    checked = debugSettings.entities.showCurrentTile
                ) { debugSettings.entities.showCurrentTile = it }
                toggleButton(
                    text = "Position",
                    checked = debugSettings.entities.showPosition
                ) { debugSettings.entities.showPosition = it }
            }

            row(spacing = CONTROL_GAP) {
                defaults().fillAvailableX().uniformX()
                    .height(COMPACT_CONTROL_HEIGHT)
                toggleButton(
                    text = "Path",
                    checked = debugSettings.entities.showPath
                ) { debugSettings.entities.showPath = it }
                toggleButton(
                    text = "Direction",
                    checked = debugSettings.entities.showDirection
                ) { debugSettings.entities.showDirection = it }
            }

            row(spacing = CONTROL_GAP) {
                defaults().fillAvailableX().uniformX()
                    .height(COMPACT_CONTROL_HEIGHT)
                toggleButton(
                    text = "Sprite bounds",
                    checked = debugSettings.entities.showSpriteBounds
                ) { debugSettings.entities.showSpriteBounds = it }
                toggleButton(
                    text = "Tile fill",
                    checked = debugSettings.entities.currentTileFillColor != null
                ) { enabled ->
                    if (enabled) {
                        debugSettings.entities.currentTileFillColor =
                            entityTileFillColor
                    } else {
                        debugSettings.entities.currentTileFillColor?.let {
                            entityTileFillColor = it
                        }
                        debugSettings.entities.currentTileFillColor = null
                    }
                }
            }

            numericControl(
                label = "Line width",
                initialValue = debugSettings.entities.lineWidth,
                step = 0.25f,
                range = 0.25f..8f
            ) { debugSettings.entities.lineWidth = it }

            numericControl(
                label = "Fill alpha",
                initialValue = entityTileFillColor.a,
                step = ALPHA_STEP,
                range = ALPHA_RANGE
            ) { value ->
                entityTileFillColor = entityTileFillColor.apply { a = value }
                if (debugSettings.entities.currentTileFillColor != null) {
                    debugSettings.entities.currentTileFillColor =
                        entityTileFillColor
                }
            }
        }.cell {
            fillAvailableX()
        }
        entityDebugCell = checkNotNull(getCell(entityDebugControls))

        updateDebugVisibility()
    }

    private fun StrataLayout.numericControl(
        label: String,
        initialValue: Float,
        step: Float,
        range: ClosedFloatingPointRange<Float>,
        onChanged: (Float) -> Unit
    ) {
        var value = initialValue.coerceIn(range.start, range.endInclusive)
        lateinit var valueLabel: Label

        row(spacing = CONTROL_GAP) {
            label(label).cell {
                growX()
                left()
            }

            button("-") {
                value = (value - step).coerceIn(
                    range.start,
                    range.endInclusive
                )
                onChanged(value)
                valueLabel.setText(formatValue(value))
            }.cell {
                fixedSize(NUMERIC_BUTTON_SIZE, NUMERIC_BUTTON_SIZE)
            }

            valueLabel = label(formatValue(value)).cell {
                fixedWidth(NUMERIC_VALUE_WIDTH)
                center()
            }

            button("+") {
                value = (value + step).coerceIn(
                    range.start,
                    range.endInclusive
                )
                onChanged(value)
                valueLabel.setText(formatValue(value))
            }.cell {
                fixedSize(NUMERIC_BUTTON_SIZE, NUMERIC_BUTTON_SIZE)
            }
        }
    }

    /**
     * Reflects state changes made through retained keyboard controls.
     */
    fun sync() {
        modeSelection.select(toolController.mode)

        if (painter.terrain in terrains) {
            terrainSelection.select(painter.terrain)
        }

        paintLayers.firstOrNull {
            it.layerId == painter.layerId
        }?.let { layer ->
            layerSelection.select(layer)
        }

        buildEntries.firstOrNull { entry ->
            placementController.selectedPlaceable?.let(entry.type::isInstance)
                ?: false
        }?.let { entry ->
            buildSelection.select(entry)
        }

        entitySpawner.selectedEntry?.let { entry ->
            spawnSelection.select(entry)
        }

        buildControls.isVisible = toolController.mode == SandboxMode.BUILD
        paintControls.isVisible = toolController.mode == SandboxMode.PAINT
        spawnControls.isVisible = toolController.mode == SandboxMode.SPAWN
        updateDebugVisibility()
        updatePanelTab()

        updateStatus()
    }

    fun update(delta: Float) {
        sync()
        performanceOverlay.update(delta)
        simulationOverlay.sync()
    }

    private fun updateDebugVisibility() {
        if (
            !::gridDebugControls.isInitialized ||
            !::objectDebugControls.isInitialized ||
            !::entityDebugControls.isInitialized
        ) {
            return
        }

        setDebugControlsVisible(
            gridDebugCell,
            gridDebugControls,
            debugSettings.grid.enabled
        )
        setDebugControlsVisible(
            objectDebugCell,
            objectDebugControls,
            debugSettings.objects.enabled
        )
        setDebugControlsVisible(
            entityDebugCell,
            entityDebugControls,
            debugSettings.entities.enabled
        )
    }

    private fun setDebugControlsVisible(
        cell: Cell<StrataColumn>,
        controls: StrataColumn,
        visible: Boolean
    ) {
        val actor = controls.takeIf { visible }
        if (cell.actor !== actor) {
            cell.setActor(actor)
        }
    }

    private fun updatePanelTab() {
        if (
            !::toolsControls.isInitialized ||
            !::debugControls.isInitialized
        ) {
            return
        }

        val toolsSelected =
            tabSelection.selected == SandboxPanelTab.TOOLS

        toolsControls.isVisible = toolsSelected
        debugControls.isVisible = !toolsSelected
    }

    private fun updateStatus() {
        if (!::modeStatus.isInitialized) return

        modeStatus.setText(
            "Mode: ${modeSelection.selected?.displayName}"
        )

        selectionStatus.setText(
            when (modeSelection.selected) {
                SandboxMode.NONE -> "No editing tool active"
                SandboxMode.BUILD -> {
                    "Object: ${buildSelection.selected?.displayName()}"
                }
                SandboxMode.PAINT -> {
                    "Terrain: ${terrainSelection.selected?.displayName()}\n" +
                        "Layer: ${layerSelection.selected?.displayName}"
                }
                SandboxMode.SPAWN -> {
                    "Entity: ${spawnSelection.selected?.name}"
                }
                null -> ""
            }
        )
    }

    companion object {

        private const val TOOLBAR_MIN_WIDTH = 280f
        private const val ROOT_MARGIN = 16f
        private const val SECTION_GAP = 6f
        private const val CONTROL_GAP = 4f
        private const val MODE_BUTTON_HEIGHT = 36f
        private const val COMPACT_CONTROL_HEIGHT = 30f
        private const val BUILD_BUTTON_HEIGHT = 72f
        private const val BUILD_BUTTON_MIN_WIDTH = 72f
        private const val IMAGE_PADDING = 6f
        private const val BUILD_COLUMNS = 3
        private const val TERRAIN_COLUMNS = 3
        private const val LAYER_COLUMNS = 2
        private const val SPAWN_COLUMNS = 2
        private const val NUMERIC_BUTTON_SIZE = 28f
        private const val NUMERIC_VALUE_WIDTH = 44f
        private const val ALPHA_STEP = 0.05f
        private const val DEFAULT_BACKGROUND_ALPHA = 0.25f
        private val ALPHA_RANGE = 0f..1f

        private fun formatValue(value: Float): String {
            return String.format(Locale.ROOT, "%.2f", value)
        }

        fun createTheme() = StrataUiTheme(
            labelStyle = "default",
            buttonStyle = "default",
            toggleButtonStyle = "default",
            imageButtonStyle = "default",
            selectableImageButtonStyle = "default",
            panelStyle = "toolbar",
            separatorStyle = "toolbar",
            spacing = 8f
        )

        fun createSkin(): Skin {
            val skin = Skin()
            val font = BitmapFont()

            skin.add(
                "default-font",
                font
            )

            val pixmap = Pixmap(
                1,
                1,
                Pixmap.Format.RGBA8888
            ).apply {
                setColor(Color.WHITE)
                fill()
            }

            val texture = Texture(pixmap)
            pixmap.dispose()

            skin.add(
                "white",
                texture
            )

            val baseDrawable = TextureRegionDrawable(
                TextureRegion(texture)
            )

            skin.add(
                "default",
                Label.LabelStyle(
                    font,
                    Color.WHITE
                )
            )

            skin.add(
                "title",
                Label.LabelStyle(
                    font,
                    Color(0.55f, 0.82f, 1f, 1f)
                )
            )

            skin.add(
                "default",
                TextButton.TextButtonStyle().apply {
                    this.font = font
                    fontColor = Color.WHITE

                    up = baseDrawable.tint(
                        Color(0.18f, 0.18f, 0.20f, 1f)
                    )

                    over = baseDrawable.tint(
                        Color(0.25f, 0.25f, 0.28f, 1f)
                    )

                    down = baseDrawable.tint(
                        Color(0.12f, 0.12f, 0.14f, 1f)
                    )

                    checked = baseDrawable.tint(
                        Color(0.16f, 0.45f, 0.68f, 1f)
                    )

                    checkedOver = baseDrawable.tint(
                        Color(0.20f, 0.55f, 0.78f, 1f)
                    )

                    disabled = baseDrawable.tint(
                        Color(0.11f, 0.11f, 0.12f, 1f)
                    )

                    disabledFontColor = Color(0.5f, 0.5f, 0.52f, 1f)
                }
            )

            skin.add(
                "default",
                ImageButton.ImageButtonStyle().apply {
                    up = baseDrawable.tint(
                        Color(0.18f, 0.18f, 0.20f, 1f)
                    )

                    over = baseDrawable.tint(
                        Color(0.25f, 0.25f, 0.28f, 1f)
                    )

                    down = baseDrawable.tint(
                        Color(0.12f, 0.12f, 0.14f, 1f)
                    )

                    checked = baseDrawable.tint(
                        Color(0.16f, 0.45f, 0.68f, 1f)
                    )

                    checkedOver = baseDrawable.tint(
                        Color(0.20f, 0.55f, 0.78f, 1f)
                    )

                    disabled = baseDrawable.tint(
                        Color(0.11f, 0.11f, 0.12f, 1f)
                    )
                }
            )

            skin.add(
                "toolbar",
                StrataPanelStyle(
                    background = baseDrawable.tint(
                        Color(0.08f, 0.08f, 0.10f, 0.94f)
                    ),
                    padding = StrataInsets.all(12f)
                ),
                StrataPanelStyle::class.java
            )

            skin.add(
                "toolbar",
                StrataSeparatorStyle(
                    drawable = baseDrawable.tint(
                        Color(0.35f, 0.35f, 0.38f, 1f)
                    ),
                    thickness = 1f
                ),
                StrataSeparatorStyle::class.java
            )

            return skin
        }
    }
}

private fun ObjectEntry.displayName(): String {
    return type.simpleName
        ?.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
        ?: type.toString()
}

private fun TerrainType.displayName(): String {
    return name.toDisplayName()
}
