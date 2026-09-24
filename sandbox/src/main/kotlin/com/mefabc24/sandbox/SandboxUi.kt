package com.mefabc24.sandbox

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.ui.StrataInsets
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataSelectableButton
import com.mefabc24.strata.ui.StrataSeparatorStyle
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.StrataUiTheme
import com.mefabc24.strata.ui.cell
import com.mefabc24.strata.world.Placeable

private enum class SandboxMode(
    val displayName: String
) {
    BUILD("Build"),
    PAINT("Paint")
}

private enum class SandboxBuildOption(
    val displayName: String,
    val create: () -> Placeable
) {
    HOUSE("House", ::House),
    OAK_TREE("Oak tree", ::OakTree);

    companion object {
        fun from(placeable: Placeable?): SandboxBuildOption? {
            return when (placeable) {
                is House -> HOUSE
                is OakTree -> OAK_TREE
                else -> null
            }
        }
    }
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
    private val placementController: PlacementController
) {

    private val modeSelection = ui.selectionGroup(
        options = SandboxMode.entries,
        initialSelection = if (painter.enabled) {
            SandboxMode.PAINT
        } else {
            SandboxMode.BUILD
        }
    ) { selected ->
        painter.enabled = selected == SandboxMode.PAINT
        updateStatus()
    }

    private val terrainSelection = ui.selectionGroup(
        options = TerrainType.entries,
        initialSelection = painter.terrain
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
        options = SandboxBuildOption.entries,
        initialSelection = SandboxBuildOption.from(
            placementController.selectedPlaceable
        ) ?: SandboxBuildOption.HOUSE
    ) { selected ->
        placementController.selectedPlaceable = selected.create()
        updateStatus()
    }

    private val buildButtons =
        mutableListOf<StrataSelectableButton<SandboxBuildOption>>()

    private val terrainButtons =
        mutableListOf<StrataSelectableButton<TerrainType>>()

    private val layerButtons =
        mutableListOf<StrataSelectableButton<SandboxPaintLayer>>()

    private lateinit var modeStatus: Label
    private lateinit var buildStatus: Label
    private lateinit var terrainStatus: Label
    private lateinit var layerStatus: Label

    init {
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

            label("Build object")

            row {
                for (option in SandboxBuildOption.entries) {
                    buildButtons += selectableButton(
                        text = option.displayName,
                        value = option,
                        group = buildSelection
                    ).cell {
                        width(116f)
                        height(34f)
                    }
                }
            }

            label("Terrain")

            row {
                for (terrain in TerrainType.entries) {
                    terrainButtons += selectableButton(
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
                    layerButtons += selectableButton(
                        text = layer.displayName,
                        value = layer,
                        group = layerSelection
                    ).cell {
                        width(116f)
                        height(34f)
                    }
                }
            }

            separator()
            label("Status", styleName = "title")

            modeStatus = label("")
            buildStatus = label("")
            terrainStatus = label("")
            layerStatus = label("")
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
        modeSelection.select(
            if (painter.enabled) {
                SandboxMode.PAINT
            } else {
                SandboxMode.BUILD
            }
        )

        terrainSelection.select(painter.terrain)

        SandboxPaintLayer.from(painter.layerId)?.let {
            layerSelection.select(it)
        }

        SandboxBuildOption.from(
            placementController.selectedPlaceable
        )?.let {
            buildSelection.select(it)
        }

        val buildEnabled = !painter.enabled
        val paintEnabled = painter.enabled

        for (button in buildButtons) {
            button.isDisabled = !buildEnabled
        }

        for (button in terrainButtons) {
            button.isDisabled = !paintEnabled
        }

        for (button in layerButtons) {
            button.isDisabled = !paintEnabled
        }

        updateStatus()
    }

    private fun updateStatus() {
        if (!::modeStatus.isInitialized) return

        modeStatus.setText(
            "Mode: ${modeSelection.selected?.displayName}"
        )

        buildStatus.setText(
            "Object: ${buildSelection.selected?.displayName}"
        )

        terrainStatus.setText(
            "Terrain: ${terrainSelection.selected?.displayName()}"
        )

        layerStatus.setText(
            "Layer: ${layerSelection.selected?.displayName}"
        )
    }

    companion object {

        fun createTheme() = StrataUiTheme(
            labelStyle = "default",
            buttonStyle = "default",
            toggleButtonStyle = "default",
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

private fun TerrainType.displayName(): String {
    return name.lowercase().replaceFirstChar {
        it.titlecase()
    }
}
