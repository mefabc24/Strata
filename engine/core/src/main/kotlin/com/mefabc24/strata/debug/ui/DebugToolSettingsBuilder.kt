package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.debug.*
import com.mefabc24.strata.debug.inspector.DebugInspection
import com.mefabc24.strata.debug.inspector.DebugInspector
import com.mefabc24.strata.debug.tools.*
import com.mefabc24.strata.pathfinding.PathMovementMode
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.`object`.ObjectEntry
import com.mefabc24.strata.render.entity.EntityEntry
import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.ui.*

internal data class DebugToolSettingsUi(
    val buildControls: StrataColumn,
    val deleteControls: StrataColumn,
    val paintControls: StrataColumn,
    val overlayPaintControls: StrataColumn,
    val spawnControls: StrataColumn,
    val inspectControls: StrataColumn,
    val moveControls: StrataColumn,
    val pathControls: StrataColumn,
    val cameraControls: StrataColumn,
    val inspectorRows: DebugDiagnosticTable,
    val statusRows: Map<DebugToolMode, DebugDiagnosticTable>
)

/** Builds contextual settings for the tools exposed by the Tool Rail. */
internal class DebugToolSettingsBuilder(
    private val ui: StrataUi,
    private val settings: DebugSettings,
    private val tools: DebugToolController,
    private val painter: DebugTerrainPainter,
    private val spawner: DebugEntitySpawner,
    private val buildEntries: List<ObjectEntry>,
    private val placement: PlacementController?,
    private val inspector: DebugInspector,
    private val pathfinding: DebugPathfindingTool,
    private val buildSelection: StrataSelectionGroup<ObjectEntry>?,
    private val terrainSelection: StrataSelectionGroup<TerrainEntry>?,
    private val spawnSelection: StrataSelectionGroup<EntityEntry>?,
    private val paintTargets: List<DebugPaintTarget>,
    private val paintTargetSelection: StrataSelectionGroup<DebugPaintTarget>,
    private val overlaySelection: StrataSelectionGroup<String>?,
    private val synchronizers: DebugControlBindings,
    private val onLayoutChanged: () -> Unit
) {
    private lateinit var buildControls: StrataColumn
    private lateinit var deleteControls: StrataColumn
    private lateinit var paintControls: StrataColumn
    private lateinit var overlayPaintControls: StrataColumn
    private lateinit var spawnControls: StrataColumn
    private lateinit var inspectControls: StrataColumn
    private lateinit var moveControls: StrataColumn
    private lateinit var pathControls: StrataColumn
    private lateinit var cameraControls: StrataColumn
    private lateinit var inspectorRows: DebugDiagnosticTable
    private val toolStatusRows = mutableMapOf<DebugToolMode, DebugDiagnosticTable>()

    fun build(column: StrataColumn): DebugToolSettingsUi {
        column.buildToolSettings()
        return DebugToolSettingsUi(
            buildControls,
            deleteControls,
            paintControls,
            overlayPaintControls,
            spawnControls,
            inspectControls,
            moveControls,
            pathControls,
            cameraControls,
            inspectorRows,
            toolStatusRows
        )
    }

    private fun StrataColumn.buildToolSettings() {
        stack {
            buildControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander(DebugToolMode.BUILD, "Selection") {
                    val group = buildSelection
                    if (group == null) {
                        emptyToolMessage("No constructible objects registered")
                    } else {
                        actor(DebugToolSelectionList(
                            items = buildEntries.map { entry ->
                                val footprint = entry.create().footprint
                                DebugToolSelectionItem(
                                    value = entry,
                                    displayName = entry.displayName(),
                                    texture = entry.selectionVisual.texture,
                                    secondaryText = "${footprint.offsets.size} ${if (footprint.offsets.size == 1) "tile" else "tiles"}",
                                    trailingText = footprint.dimensionsText()
                                )
                            },
                            selectionGroup = group,
                            skin = ui.skin,
                            searchHint = "Search objects...",
                            onLayoutChanged = ::invalidateToolFlyoutLayout
                        )).cell { fillAvailableX() }
                    }
                }
                toolSettingsExpander(DebugToolMode.BUILD, "Preview") {
                    val group = buildSelection
                    if (group == null) {
                        emptyToolMessage("Select a registered object to preview it")
                    } else {
                        val preview = DebugToolPreviewCard(ui.skin)
                        fun updatePreview(entry: ObjectEntry?) {
                            if (entry == null) return
                            val footprint = entry.create().footprint
                            val visual = entry.selectionVisual
                            val texture = visual.texture
                            preview.show(DebugToolPreview(
                                key = entry,
                                displayName = entry.displayName(),
                                texture = texture,
                                details = listOf(
                                    "Footprint" to footprint.dimensionsText(),
                                    "Occupied" to "${footprint.offsets.size} ${if (footprint.offsets.size == 1) "tile" else "tiles"}",
                                    "Sprite" to "${texture.regionWidth} x ${texture.regionHeight} px",
                                    "Frames" to visual.sprite.frameCount.toString()
                                )
                            ))
                        }
                        actor(preview).cell { fillAvailableX(); height(110f) }
                        ui.root.context.own(group.onSelectionChanged(::updatePreview))
                        updatePreview(group.selected)
                    }
                }
                toolStatus(DebugToolMode.BUILD)
            }
            deleteControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander(DebugToolMode.DELETE, "Brush") {
                    brushSizeStepper("Brush size", { settings.delete.brushSize }) {
                        settings.delete.brushSize = it
                    }
                    simpleToggle("Drag deletion", { settings.delete.dragEnabled }) {
                        settings.delete.dragEnabled = it
                    }
                    simpleToggle("Show affected area", { settings.delete.showBrushPreview }) {
                        settings.delete.showBrushPreview = it
                    }
                    simpleToggle("Show tile borders", { settings.delete.showTileBorders }) {
                        settings.delete.showTileBorders = it
                    }
                }
                toolStatus(DebugToolMode.DELETE)
            }
            paintControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander(DebugToolMode.PAINT, "Brush") {
                    brushSizeStepper("Brush size", { settings.paint.brushSize }) {
                        settings.paint.brushSize = it
                    }
                    simpleToggle("Show affected area", { settings.paint.showBrushPreview }) {
                        settings.paint.showBrushPreview = it
                    }
                    simpleToggle("Show tile borders", { settings.paint.showTileBorders }) {
                        settings.paint.showTileBorders = it
                    }
                }
                toolSettingsExpander(DebugToolMode.PAINT, "Terrain") {
                    val group = terrainSelection
                    if (group == null) {
                        emptyToolMessage("No paintable terrain registered")
                    } else {
                        actor(DebugToolSelectionList(
                            items = painter.entries.map { entry ->
                                DebugToolSelectionItem(
                                    value = entry,
                                    displayName = entry.type.toString().toDisplayName(),
                                    texture = entry.selectionTexture
                                )
                            },
                            selectionGroup = group,
                            skin = ui.skin,
                            searchHint = "Search terrain...",
                            showSearch = false,
                            onLayoutChanged = ::invalidateToolFlyoutLayout
                        )).cell { fillAvailableX() }
                    }
                }
                toolSettingsExpander(DebugToolMode.PAINT, "Target") {
                    selectionSettingRow(
                        "Paint layer",
                        paintTargets,
                        paintTargetSelection
                    ) { it.name.toDisplayName() }
                    overlayPaintControls = column(spacing = 0f) {
                        defaults().fillAvailableX()
                        overlaySelection?.let { group ->
                            boundDropdown(
                                "Overlay layer",
                                painter.overlayLayerIds,
                                { checkNotNull(group.selected) },
                                String::toDisplayName,
                                group::select
                            )
                        }
                    }
                }
                toolStatus(DebugToolMode.PAINT)
            }
            spawnControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander(DebugToolMode.SPAWN, "Selection") {
                    val group = spawnSelection
                    if (group == null) {
                        emptyToolMessage("No spawnable entities registered")
                    } else {
                        actor(DebugToolSelectionList(
                            items = spawner.entries.map { entry ->
                                val visual = entry.selectionVisual
                                val frames = visual.sprite.frameCount
                                DebugToolSelectionItem(
                                    value = entry,
                                    displayName = entry.type.displayName(),
                                    texture = visual.texture,
                                    secondaryText = if (frames == 1) {
                                        "Static sprite"
                                    } else {
                                        "$frames animation frames"
                                    }
                                )
                            },
                            selectionGroup = group,
                            skin = ui.skin,
                            searchHint = "Search entities...",
                            onLayoutChanged = ::invalidateToolFlyoutLayout
                        )).cell { fillAvailableX() }
                    }
                }
                toolSettingsExpander(DebugToolMode.SPAWN, "Preview") {
                    val group = spawnSelection
                    if (group == null) {
                        emptyToolMessage("Select a registered entity to preview it")
                    } else {
                        val preview = DebugToolPreviewCard(ui.skin)
                        fun updatePreview(entry: EntityEntry?) {
                            if (entry == null) return
                            val visual = entry.selectionVisual
                            val texture = visual.texture
                            preview.show(DebugToolPreview(
                                key = entry,
                                displayName = entry.type.displayName(),
                                texture = texture,
                                details = buildList {
                                    add("Sprite" to "${texture.regionWidth} x ${texture.regionHeight} px")
                                    add("Frames" to visual.sprite.frameCount.toString())
                                    if (visual.width != null && visual.height != null) {
                                        add("Size" to "${visual.width} x ${visual.height}")
                                    }
                                    add("Scale" to visual.scale.toString())
                                }
                            ))
                        }
                        actor(preview).cell { fillAvailableX(); height(110f) }
                        ui.root.context.own(group.onSelectionChanged(::updatePreview))
                        updatePreview(group.selected)
                    }
                }
                toolStatus(DebugToolMode.SPAWN)
            }
            inspectControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander(DebugToolMode.INSPECT, "Selection") {
                    inspectorRows = diagnosticTable()
                    compactAction("Clear selection") { inspector.clear() }
                }
                toolSettingsExpander(DebugToolMode.INSPECT, "Entity control") {
                simpleToggle(
                    "Frozen",
                    {
                        (inspector.selection as? DebugInspection.EntityTarget)
                            ?.entity
                            ?.let(settings::isEntityFrozen)
                            ?: false
                    }
                ) { frozen ->
                    (inspector.selection as? DebugInspection.EntityTarget)
                        ?.entity
                        ?.let {
                            settings.setEntityFrozen(it, frozen)
                            settings.notify(
                                if (frozen) "Entity frozen" else "Entity unfrozen",
                                DebugNotificationSeverity.SUCCESS
                            )
                        }
                }
                simpleToggle(
                    "Freeze animation",
                    { settings.inspect.freezeEntityAnimation }
                ) { settings.inspect.freezeEntityAnimation = it }
                compactAction("Unfreeze all entities") {
                    val count = settings.unfreezeAllEntities()
                    settings.notify(
                        if (count == 0) "No frozen entities" else "Unfroze $count entities",
                        if (count == 0) DebugNotificationSeverity.INFO else DebugNotificationSeverity.SUCCESS
                    )
                }
                }
                toolSettingsExpander(
                    DebugToolMode.INSPECT,
                    "Visualization",
                    expanded = false
                ) {
                settingGroupLabel("Tile")
                toggleRows(
                    toggle("Selected tile", { settings.inspect.showTile }) {
                        settings.inspect.showTile = it
                    }
                )
                settingGroupLabel("Object")
                toggleRows(
                    toggle("Footprint", { settings.inspect.showObjectFootprint }) {
                        settings.inspect.showObjectFootprint = it
                    },
                    toggle("Origin", { settings.inspect.showObjectOrigin }) {
                        settings.inspect.showObjectOrigin = it
                    },
                    toggle("Sprite bounds", { settings.inspect.showObjectSpriteBounds }) {
                        settings.inspect.showObjectSpriteBounds = it
                    }
                )
                settingGroupLabel("Entity")
                toggleRows(
                    toggle("Current tile", { settings.inspect.showEntityTile }) {
                        settings.inspect.showEntityTile = it
                    },
                    toggle("Position", { settings.inspect.showEntityPosition }) {
                        settings.inspect.showEntityPosition = it
                    },
                    toggle("Path", { settings.inspect.showEntityPath }) {
                        settings.inspect.showEntityPath = it
                    },
                    toggle("Direction", { settings.inspect.showEntityDirection }) {
                        settings.inspect.showEntityDirection = it
                    },
                    toggle("Sprite bounds", { settings.inspect.showEntitySpriteBounds }) {
                        settings.inspect.showEntitySpriteBounds = it
                    }
                )
                }
                toolStatus(DebugToolMode.INSPECT)
            }
            moveControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolStatus(DebugToolMode.MOVE)
            }
            pathControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                val movementModes = ui.selectionGroup(
                    PathMovementMode.entries,
                    settings.pathfinding.movementMode
                ) {
                    settings.pathfinding.movementMode = it
                }
                synchronizers += {
                    movementModes.select(settings.pathfinding.movementMode)
                }
                toolSettingsExpander(DebugToolMode.PATHFINDING, "Search") {
                    selectionSettingRow(
                        "Movement",
                        PathMovementMode.entries,
                        movementModes
                    ) {
                        when (it) {
                            PathMovementMode.FOUR_WAY -> "4-way"
                            PathMovementMode.EIGHT_WAY -> "8-way"
                        }
                    }
                }
                toolSettingsExpander(DebugToolMode.PATHFINDING, "Visualization") {
                    simpleToggle(
                        "World visualization",
                        { settings.pathfinding.enabled }
                    ) { settings.pathfinding.enabled = it }
                    toggleRows(
                        toggle("Explored nodes", { settings.pathfinding.showExploredNodes }) {
                            settings.pathfinding.showExploredNodes = it
                        },
                        toggle("Final path", { settings.pathfinding.showFinalPath }) {
                            settings.pathfinding.showFinalPath = it
                        },
                        toggle("Open set", { settings.pathfinding.showOpenSet }) {
                            settings.pathfinding.showOpenSet = it
                        },
                        toggle("Closed set", { settings.pathfinding.showClosedSet }) {
                            settings.pathfinding.showClosedSet = it
                        },
                        toggle("G cost", { settings.pathfinding.showGCost }) {
                            settings.pathfinding.showGCost = it
                        },
                        toggle("H cost", { settings.pathfinding.showHCost }) {
                            settings.pathfinding.showHCost = it
                        },
                        toggle("F cost", { settings.pathfinding.showFCost }) {
                            settings.pathfinding.showFCost = it
                        },
                        toggle("Parent direction", { settings.pathfinding.showParentDirections }) {
                            settings.pathfinding.showParentDirections = it
                        },
                        toggle("Exploration order", { settings.pathfinding.showExplorationOrder }) {
                            settings.pathfinding.showExplorationOrder = it
                        },
                        toggle("Rejected transitions", { settings.pathfinding.showRejectedTransitions }) {
                            settings.pathfinding.showRejectedTransitions = it
                        }
                    )
                    boundStepper(
                        "Maximum label zoom",
                        { settings.pathfinding.maximumLabelZoom },
                        0.25f,
                        8f,
                        0.25f
                    ) { settings.pathfinding.maximumLabelZoom = it }
                    boundStepper(
                        "Maximum labels",
                        { settings.pathfinding.maximumVisibleLabels.toFloat() },
                        16f,
                        1024f,
                        16f
                    ) { settings.pathfinding.maximumVisibleLabels = it.toInt() }
                    boundStepper(
                        "Recorded rejections",
                        { settings.pathfinding.maximumRejectedTransitions.toFloat() },
                        0f,
                        8192f,
                        128f
                    ) { settings.pathfinding.maximumRejectedTransitions = it.toInt() }
                }
                toolSettingsExpander(
                    DebugToolMode.PATHFINDING,
                    "Diagnostic search",
                    expanded = false
                ) {
                    compactActions(
                        "Start" to { pathfinding.startDiagnosticSearch() },
                        "Step" to { pathfinding.stepDiagnosticSearch() }
                    )
                    compactActions(
                        "Continue" to { pathfinding.continueDiagnosticSearch() },
                        "Pause" to { pathfinding.pauseDiagnosticSearch() },
                        "Reset" to { pathfinding.resetDiagnosticSearch() }
                    )
                    boundStepper(
                        "Iterations / update",
                        { settings.pathfinding.automaticIterationsPerUpdate.toFloat() },
                        1f,
                        64f,
                        1f
                    ) { settings.pathfinding.automaticIterationsPerUpdate = it.toInt() }
                }
                toolSettingsExpander(
                    DebugToolMode.PATHFINDING,
                    "Traversal",
                    expanded = false
                ) {
                    simpleToggle(
                        "Consume reached nodes",
                        { settings.pathfinding.consumeReachedWaypoints }
                    ) { settings.pathfinding.consumeReachedWaypoints = it }
                    boundStepper(
                        "Entity speed multiplier",
                        { settings.pathfinding.entitySpeedMultiplier },
                        0.25f,
                        10f,
                        0.25f
                    ) { settings.pathfinding.entitySpeedMultiplier = it }
                    compactAction("Clear path") {
                        if (pathfinding.clear()) {
                            settings.notify("Path cleared", DebugNotificationSeverity.INFO)
                        }
                    }
                }
                toolStatus(DebugToolMode.PATHFINDING)
            }
            cameraControls = column(spacing = 0f) {
                defaults().fillAvailableX()
                toolSettingsExpander(DebugToolMode.FREE_CAMERA, "Movement") {
                    simpleToggle(
                        "Disable restrictions",
                        { settings.camera.disableRestrictions }
                    ) { settings.camera.disableRestrictions = it }
                }
                toolSettingsExpander(DebugToolMode.FREE_CAMERA, "Visualization") {
                    toggleRows(
                        toggle("Visible area", { settings.camera.showVisibleArea }) {
                            settings.camera.showVisibleArea = it
                        },
                        toggle("World bounds", { settings.camera.showWorldBounds }) {
                            settings.camera.showWorldBounds = it
                        },
                        toggle("Clamp bounds", { settings.camera.showClampBounds }) {
                            settings.camera.showClampBounds = it
                        }
                    )
                }
                toolStatus(DebugToolMode.FREE_CAMERA)
            }
        }.cell { fillAvailableX() }
    }

    private data class ToggleBinding(
        val text: String,
        val read: () -> Boolean,
        val write: (Boolean) -> Unit
    )

    private fun toggle(
        text: String,
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ) = ToggleBinding(text, read, write)

    private fun StrataColumn.simpleToggle(
        text: String,
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ) {
        settingToggleRow(text, read, write)
    }

    private fun StrataColumn.settingGroupLabel(text: String) {
        row(
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 2f)
        ) {
            label(text, "title").cell { growX(); left() }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(28f) }
        separator()
    }

    private fun StrataColumn.emptyToolMessage(text: String) {
        row(
            padding = StrataInsets.symmetric(horizontal = 8f, vertical = 4f)
        ) {
            wrappingLabel(text)
        }.applyDebugSettingBackground().cell { fillAvailableX(); minHeight(42f) }
    }

    private fun StrataColumn.settingsExpander(
        title: String,
        expanded: Boolean = false,
        style: StrataExpanderStyle = debugExpanderStyle(),
        onExpandedChanged: (Boolean) -> Unit = {},
        configure: StrataColumn.() -> Unit
    ): StrataExpander {
        val expander = expander(
            title = title,
            expanded = expanded,
            spacing = 0f,
            headerHeight = 34f,
            expandedStyle = style,
            onExpandedChanged = onExpandedChanged
        ) {
            defaults().fillAvailableX()
            configure()
        }
        getCell(expander).fillAvailableX()
        return expander
    }

    private fun StrataColumn.toolSettingsExpander(
        mode: DebugToolMode,
        title: String,
        expanded: Boolean = true,
        configure: StrataColumn.() -> Unit
    ): StrataExpander {
        require(title in debugToolFlyoutSectionTitles(mode)) {
            "Unknown $mode tool flyout section: $title"
        }
        return settingsExpander(
            title = title,
            expanded = expanded,
            style = toolExpanderStyle(),
            onExpandedChanged = { invalidateToolFlyoutLayout() },
            configure = configure
        )
    }

    private fun StrataColumn.toolStatus(mode: DebugToolMode) {
        toolSettingsExpander(mode, "Status") {
            toolStatusRows[mode] = diagnosticTable().apply {
                pad(4f, 9f, 4f, 9f)
            }
        }
    }

    private fun invalidateToolFlyoutLayout() {
        onLayoutChanged()
    }

    private fun debugExpanderStyle(): StrataExpanderStyle = ui.skin.get(
        "debug-expander",
        StrataExpanderStyle::class.java
    )

    private fun toolExpanderStyle(): StrataExpanderStyle = ui.skin.get(
        "debug-tool-expander",
        StrataExpanderStyle::class.java
    )

    private fun StrataColumn.toggleRows(vararg controls: ToggleBinding) {
        controls.forEach { binding ->
            settingToggleRow(binding.text, binding.read, binding.write)
        }
    }

    private fun StrataColumn.settingToggleRow(
        text: String,
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ) {
        row(
            spacing = 6f,
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 2f)
        ) {
            label(text).cell { growX(); left() }
            settingToggle(read, write).cell { width(56f); height(26f) }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(32f) }
        separator()
    }

    private fun <T> StrataColumn.selectionSettingRow(
        text: String,
        options: Iterable<T>,
        group: StrataSelectionGroup<T>,
        displayText: (T) -> String
    ) {
        row(
            spacing = 3f,
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 2f)
        ) {
            label(text).cell { growX(); left() }
            options.forEach { option ->
                selectableButton(
                    displayText(option),
                    option,
                    group
                ).cell { width(70f); height(26f) }
            }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(32f) }
        separator()
    }

    private fun StrataColumn.compactAction(
        text: String,
        onClick: () -> Unit
    ) {
        compactActions(text to onClick)
    }

    private fun StrataColumn.compactActions(
        vararg actions: Pair<String, () -> Unit>
    ) {
        row(
            spacing = 0f,
            padding = StrataInsets.NONE
        ) {
            actions.forEachIndexed { index, (label, action) ->
                if (index > 0) {
                    actor(StrataSeparator(
                        StrataSeparatorOrientation.VERTICAL,
                        ui.skin.get("debug-separator", StrataSeparatorStyle::class.java)
                    )).cell {
                        width(2f)
                        height(32f)
                    }
                }
                button(label, "debug-action-card", action).cell {
                    growX()
                    height(32f)
                }
            }
        }.cell {
            fillAvailableX()
            height(32f)
        }
        separator()
    }

    private fun <T> StrataColumn.boundDropdown(
        text: String,
        options: Iterable<T>,
        read: () -> T,
        displayText: (T) -> String = { it.toString() },
        write: (T) -> Unit
    ): StrataDropdown<T> {
        lateinit var dropdown: StrataDropdown<T>
        row(
            spacing = 6f,
            padding = StrataInsets.symmetric(horizontal = 6f, vertical = 2f)
        ) {
            label(text).cell { growX(); left() }
            dropdown = dropdown(
                options = options,
                selected = read(),
                displayText = displayText,
                onChanged = write
            ).cell { width(148f); height(28f) }
        }.applyDebugSettingBackground().cell { fillAvailableX(); height(32f) }
        separator()
        synchronizers += { dropdown.sync(read()) }
        return dropdown
    }

    private fun StrataLayout.settingToggle(
        read: () -> Boolean,
        write: (Boolean) -> Unit
    ): StrataToggleButton {
        val button = toggleButton(
            if (read()) "ON" else "OFF",
            read(),
            onChanged = write
        )
        synchronizers += {
            val checked = read()
            button.syncChecked(checked)
            button.setText(if (checked) "ON" else "OFF")
        }
        return button
    }

    private fun StrataColumn.boundStepper(
        text: String,
        read: () -> Float,
        minimum: Float,
        maximum: Float,
        step: Float,
        write: (Float) -> Unit
    ): StrataNumericStepper {
        val stepper = numericStepper(
            text, read(), minimum, maximum, step,
            valueStyleName = "debug-stepper-value",
            onChanged = write
        ).applyDebugSettingBackground().apply {
            padLeft(6f)
            padRight(6f)
        }.cell {
            fillAvailableX()
            height(32f)
        }
        separator()
        synchronizers += { stepper.sync(read()) }
        return stepper
    }

    private fun StrataColumn.brushSizeStepper(
        text: String,
        read: () -> Int,
        write: (Int) -> Unit
    ): StrataNumericStepper {
        val stepper = numericStepper(
            text,
            read().toFloat(),
            1f,
            9f,
            2f,
            decimals = 0,
            onChanged = { write(it.toInt()) }
        ).applyDebugSettingBackground().cell {
            fillAvailableX()
            height(32f)
        }.apply {
            padLeft(6f)
            padRight(6f)
        }
        separator()
        synchronizers += { stepper.sync(read().toFloat()) }
        return stepper
    }

    private fun StrataLayout.wrappingLabel(text: String): Label = label(text).apply {
        setWrap(true)
    }.cell { fillAvailableX() }

    private fun StrataLayout.diagnosticTable(): DebugDiagnosticTable {
        val table = actor(DebugDiagnosticTable(ui.skin))
        table.applyDebugSettingBackground()
        val cell = requireNotNull(getCell(table)).apply { fillAvailableX() }
        table.bindLayout(cell)
        return table
    }

    private fun <T : Table> T.applyDebugSettingBackground(): T = apply {
        background = ui.skin.get(
            "debug-setting-row",
            StrataPanelStyle::class.java
        ).background
    }

}

private fun String.toDisplayName() = replace('_', ' ').replace('-', ' ')
    .lowercase().replaceFirstChar(Char::titlecase)
private fun kotlin.reflect.KClass<*>.displayName() = simpleName?.toDisplayName() ?: toString()
private fun ObjectEntry.displayName() = type.displayName()

private fun com.mefabc24.strata.world.Footprint.dimensionsText(): String {
    val width = offsets.maxOf { it.x } - offsets.minOf { it.x } + 1
    val height = offsets.maxOf { it.y } - offsets.minOf { it.y } + 1
    return "$width x $height"
}
