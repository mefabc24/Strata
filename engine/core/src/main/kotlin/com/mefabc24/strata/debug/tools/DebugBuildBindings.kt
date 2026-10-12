package com.mefabc24.strata.debug.tools

import com.badlogic.gdx.Input
import com.mefabc24.strata.debug.DebugToolMode
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.placement.PlacementDiagnostic
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TilePosition

/** Input policy for Build; object removal remains a separate, unchanged binding. */
internal fun debugBuildBindings(
    tools: DebugToolController,
    placement: PlacementController?,
    onFinished: (List<PlacedObject>, PlacementDiagnostic?) -> Unit
): List<WorldInputBinding> = listOf(
    WorldInputBinding.Grid(
        WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
        { tools.mode == DebugToolMode.BUILD && tools.buildShape == DebugBuildShape.PATH && tools.buildPathActive }
    ) { x, y -> tools.beginBuild(TilePosition(x, y)) },
    WorldInputBinding.Tile(
        WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
        { tools.mode == DebugToolMode.BUILD }
    ) { x, y -> tools.beginBuild(TilePosition(x, y)) },
    WorldInputBinding.Grid(
        WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
        { tools.mode == DebugToolMode.BUILD }
    ) { x, y -> tools.dragBuild(TilePosition(x, y)) },
    WorldInputBinding.Grid(
        WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
        { tools.mode == DebugToolMode.BUILD }
    ) { x, y ->
        if (tools.buildShape == DebugBuildShape.PATH) true
        else if (!tools.buildDragging) false
        else {
            val diagnostic = placement?.currentDiagnostic
            onFinished(tools.finishBuild(TilePosition(x, y)), diagnostic)
            true
        }
    },
    WorldInputBinding.NoPicking(
        WorldInputTrigger.KeyDown(Input.Keys.ENTER),
        { tools.mode == DebugToolMode.BUILD && tools.buildPathActive }
    ) {
        val diagnostic = placement?.currentDiagnostic
        onFinished(tools.finishBuild(), diagnostic)
        true
    },
    WorldInputBinding.NoPicking(
        WorldInputTrigger.KeyDown(Input.Keys.ESCAPE),
        { tools.mode == DebugToolMode.BUILD && tools.buildPathActive }
    ) { tools.cancelBuildPath() }
)
