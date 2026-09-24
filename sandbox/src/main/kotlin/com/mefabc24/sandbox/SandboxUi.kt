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
import com.mefabc24.strata.render.ObjectEntry
import com.mefabc24.strata.terrain.TerrainEntry
import com.mefabc24.strata.ui.StrataColumn
import com.mefabc24.strata.ui.StrataInsets
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataSeparatorStyle
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.StrataUiTheme
import com.mefabc24.strata.ui.cell

private enum class SandboxMode(
    val displayName: String
) {
    BUILD("Build"),
    PAINT("Paint")
}

private enum class SandboxPaintLayer(
    val displayName: String,
    val layerId: String?
) {
    GROUND("Ground", null),
    DEMO_OVERLAY("Demo overlay", "demo");

    companion object {
        fun from(layerId: String?): SandboxPaintLayer? {
            return entries.firstOrNull {
                it.layerId == layerId
            }
        }
    }
}

class SandboxUi(
    private val ui: StrataUi,
    private val painter: SandboxTerrainPainter,
    private val placementController: PlacementController,
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
        options = SandboxPaintLayer.entries,
        initialSelection = SandboxPaintLayer.from(painter.layerId)
            ?: SandboxPaintLayer.GROUND
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
        placementController.selectedPlaceable = selected.create()
        updateStatus()
    }

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
            placementController.selectedPlaceable = selectedBuildEntry.create()
        }

        buildUi()
        sync()
    }

    private fun buildUi() {
        ui.root.pad(16f)

        ui.panel {
            label(
                text = "Strata tools",
                styleName = "title"
            )

            separator()

            label("Mode")

            row {
                for (mode in SandboxMode.entries) {
                    selectableButton(
                        text = mode.displayName,
                        value = mode,
                        group = modeSelection
                    ).cell {
                        width(116f)
                        height(36f)
                    }
                }
            }

            stack {
                buildControls = column {
                    label("Build object")

                    grid(columns = 3) {
                        for (entry in buildEntries) {
                            column(
                                spacing = 4f,
                                alignment = Align.center
                            ) {
                                selectableImageButton(
                                    drawable = TextureRegionDrawable(
                                        entry.visual.texture
                                    ),
                                    value = entry,
                                    group = buildSelection
                                ).apply {
                                    image.setScaling(Scaling.fit)
                                    imageCell.pad(6f)
                                }.cell {
                                    width(72f)
                                    height(72f)
                                }

                                label(entry.displayName()).cell {
                                    center()
                                }
                            }
                        }
                    }
                }

                paintControls = column {
                    label("Terrain")

                    grid(columns = 3) {
                        for (terrain in terrains) {
                            selectableButton(
                                text = terrain.displayName(),
                                value = terrain,
                                group = terrainSelection
                            ).cell {
                                width(74f)
                                height(34f)
                            }
                        }
                    }

                    label("Paint layer")

                    row {
                        for (layer in SandboxPaintLayer.entries) {
                            selectableButton(
                                text = layer.displayName,
                                value = layer,
                                group = layerSelection
                            ).cell {
                                width(116f)
                                height(34f)
                            }
                        }
                    }
                }
            }.cell {
                growX()
            }

            separator()
            label("Status", styleName = "title")

            modeStatus = label("")
            selectionStatus = label("")
        }.cell {
            width(272f)
            top()
            left()
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

        SandboxPaintLayer.from(painter.layerId)?.let {
            layerSelection.select(it)
        }

        buildEntries.firstOrNull { entry ->
            placementController.selectedPlaceable?.let(entry.type::isInstance)
                ?: false
        }?.let { entry ->
            buildSelection.select(entry)
        }

        buildControls.isVisible = !painter.enabled
        paintControls.isVisible = painter.enabled

        updateStatus()
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
                "Terrain: ${terrainSelection.selected?.displayName()}  " +
                    "Layer: ${layerSelection.selected?.displayName}"
            }
        )
    }

    companion object {

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
    return name.lowercase().replaceFirstChar {
        it.titlecase()
    }
}
