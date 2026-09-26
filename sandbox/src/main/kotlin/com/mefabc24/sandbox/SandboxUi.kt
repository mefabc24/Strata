package com.mefabc24.sandbox

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.render.`object`.ObjectEntry
import com.mefabc24.strata.scene.DebugGridRenderLayer
import com.mefabc24.strata.scene.DebugSettings
import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.ui.StrataColumn
import com.mefabc24.strata.ui.StrataInsets
import com.mefabc24.strata.ui.StrataLayout
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataSeparatorStyle
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.StrataUiTheme
import com.mefabc24.strata.ui.cell
import java.util.Locale

private enum class SandboxPanelTab(
    val displayName: String
) {
    TOOLS("Tools"),
    DEBUG("Debug")
}

private enum class SandboxMode(
    val displayName: String
) {
    BUILD("Build"),
    PAINT("Paint")
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
    private val debugSettings: DebugSettings,
    terrainEntries: List<TerrainEntry<TerrainType>>,
    objectEntries: List<ObjectEntry>
) {

    private val terrains = terrainEntries.map(TerrainEntry<TerrainType>::type).also {
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

    private val tabSelection = ui.selectionGroup(
        options = SandboxPanelTab.entries,
        initialSelection = SandboxPanelTab.TOOLS
    ) {
        updatePanelTab()
    }

    private val modeSelection = ui.selectionGroup(
        options = SandboxMode.entries,
        initialSelection = if (painter.enabled) {
            SandboxMode.PAINT
        } else {
            SandboxMode.BUILD
        }
    ) { selected ->
        painter.enabled = selected == SandboxMode.PAINT
        placementController.enabled = selected == SandboxMode.BUILD
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
        placementController.selectedFactory = selected::create
        updateStatus()
    }

    private val renderLayerSelection = ui.selectionGroup(
        options = DebugGridRenderLayer.entries,
        initialSelection = debugSettings.grid.renderLayer
    ) { selected ->
        debugSettings.grid.renderLayer = selected
    }

    private var gridBackgroundColor =
        debugSettings.grid.backgroundColor ?: Color.WHITE.cpy().apply {
            a = DEFAULT_BACKGROUND_ALPHA
        }

    private var hoverBackgroundColor =
        debugSettings.grid.hoverBackgroundColor ?: Color.WHITE.cpy().apply {
            a = DEFAULT_BACKGROUND_ALPHA
        }

    private lateinit var toolsControls: StrataColumn
    private lateinit var debugControls: StrataColumn
    private lateinit var buildControls: StrataColumn
    private lateinit var paintControls: StrataColumn
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
            defaults().growX().fillX()

            expander(
                title = "Strata tools",
                spacing = SECTION_GAP
            ) {
                defaults().growX().fillX()

                row(spacing = CONTROL_GAP) {
                    defaults()
                        .growX()
                        .fillX()
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
                        defaults().growX().fillX()
                        buildToolsTab()
                    }

                    debugControls = column(spacing = SECTION_GAP) {
                        defaults().growX().fillX()
                        buildDebugTab()
                    }
                }.cell {
                    growX()
                    fillX()
                }
            }.cell {
                growX()
                fillX()
            }
        }.cell {
            width(PANEL_WIDTH)
            top()
            left()
        }
    }

    private fun StrataColumn.buildToolsTab() {
        label("Mode")

        row(spacing = CONTROL_GAP) {
            defaults()
                .growX()
                .fillX()
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
                defaults().growX().fillX()
                label("Build object")

                grid(
                    columns = BUILD_COLUMNS,
                    spacing = CONTROL_GAP,
                    alignment = Align.center
                ) {
                    defaults().growX().fillX().uniformX()

                    for (entry in buildEntries) {
                        column(
                            spacing = CONTROL_GAP,
                            alignment = Align.center
                        ) {
                            defaults().growX().fillX()

                            selectableImageButton(
                                drawable = TextureRegionDrawable(
                                    entry.visual.texture
                                ),
                                value = entry,
                                group = buildSelection
                            ).apply {
                                image.setScaling(Scaling.fit)
                                imageCell.pad(IMAGE_PADDING)
                            }.cell {
                                growX()
                                fillX()
                                height(BUILD_BUTTON_HEIGHT)
                                maxWidth(BUILD_CELL_WIDTH)
                            }

                            label(entry.displayName()).cell {
                                center()
                            }
                        }.cell {
                            width(BUILD_CELL_WIDTH)
                            maxWidth(BUILD_CELL_WIDTH)
                        }
                    }
                }
            }

            paintControls = column(spacing = SECTION_GAP) {
                defaults().growX().fillX()
                label("Terrain")

                grid(
                    columns = TERRAIN_COLUMNS,
                    spacing = CONTROL_GAP,
                    alignment = Align.center
                ) {
                    defaults()
                        .growX()
                        .fillX()
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
                        .growX()
                        .fillX()
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
        }.cell {
            growX()
            fillX()
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
                .growX()
                .fillX()
                .uniformX()
                .height(MODE_BUTTON_HEIGHT)

            toggleButton(
                text = "Performance",
                checked = debugSettings.performance.enabled
            ) { enabled ->
                debugSettings.performance.enabled = enabled
            }

            toggleButton(
                text = "Grid",
                checked = debugSettings.grid.enabled
            ) { enabled ->
                debugSettings.grid.enabled = enabled
            }
        }

        label("Grid render layer")

        row(spacing = CONTROL_GAP) {
            defaults()
                .growX()
                .fillX()
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
                .growX()
                .fillX()
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
                size(NUMERIC_BUTTON_SIZE)
            }

            valueLabel = label(formatValue(value)).cell {
                width(NUMERIC_VALUE_WIDTH)
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
                size(NUMERIC_BUTTON_SIZE)
            }
        }
    }

    /**
     * Reflects state changes made through retained keyboard controls.
     */
    fun sync() {
        placementController.enabled = !painter.enabled

        modeSelection.select(
            if (painter.enabled) {
                SandboxMode.PAINT
            } else {
                SandboxMode.BUILD
            }
        )

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

        buildControls.isVisible = !painter.enabled
        paintControls.isVisible = painter.enabled
        updatePanelTab()

        updateStatus()
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
            if (modeSelection.selected == SandboxMode.BUILD) {
                "Object: ${buildSelection.selected?.displayName()}"
            } else {
                "Terrain: ${terrainSelection.selected?.displayName()}\n" +
                    "Layer: ${layerSelection.selected?.displayName}"
            }
        )
    }

    companion object {

        private const val PANEL_WIDTH = 280f
        private const val PANEL_PADDING = 12f
        private const val CONTENT_WIDTH =
            PANEL_WIDTH - PANEL_PADDING * 2f
        private const val ROOT_MARGIN = 16f
        private const val SECTION_GAP = 6f
        private const val CONTROL_GAP = 4f
        private const val MODE_BUTTON_HEIGHT = 36f
        private const val COMPACT_CONTROL_HEIGHT = 30f
        private const val BUILD_BUTTON_HEIGHT = 72f
        private const val IMAGE_PADDING = 6f
        private const val BUILD_COLUMNS = 3
        private const val TERRAIN_COLUMNS = 3
        private const val LAYER_COLUMNS = 2
        private const val BUILD_CELL_WIDTH =
            (CONTENT_WIDTH - CONTROL_GAP * (BUILD_COLUMNS - 1)) /
                BUILD_COLUMNS
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
