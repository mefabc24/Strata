package com.mefabc24.sandbox.input

import com.badlogic.gdx.Input
import com.mefabc24.sandbox.Boar
import com.mefabc24.sandbox.SandboxBuildDragController
import com.mefabc24.sandbox.SandboxEntitySpawner
import com.mefabc24.sandbox.SandboxMode
import com.mefabc24.sandbox.SandboxTerrainPainter
import com.mefabc24.sandbox.SandboxToolController
import com.mefabc24.sandbox.SandboxRoamingController
import com.mefabc24.sandbox.Wolf
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

internal fun sandboxInputBindings(
    tools: () -> SandboxToolController,
    painter: () -> SandboxTerrainPainter,
    buildDrag: () -> SandboxBuildDragController,
    entitySpawner: () -> SandboxEntitySpawner,
    roaming: () -> SandboxRoamingController,
    world: () -> World,
    playBuildingSound: () -> Unit
): List<WorldInputBinding> {
    return listOf(
        WorldInputBinding.Entity(
            trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            enabled = {
                tools().mode == SandboxMode.NONE ||
                        tools().mode == SandboxMode.BUILD
            }
        ) { entity ->
            if (
                entity.entity is Wolf ||
                entity.entity is Boar
            ) {
                println(
                    "Picked ${entity.entity::class.simpleName} " +
                            "at ${entity.position}, " +
                            "direction=${entity.direction}, " +
                            "moving=${entity.isMoving}"
                )
                true
            } else {
                false
            }
        },

        // Spawn the selected Sandbox entity at the clicked tile center.
        WorldInputBinding.Tile(
            trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            enabled = {
                tools().mode == SandboxMode.SPAWN
            }
        ) { x, y ->
            val entity = entitySpawner().spawn(
                TilePosition(x, y)
            )

            if (entity == null) {
                false
            } else {
                if (
                    entity.entity is Wolf ||
                    entity.entity is Boar
                ) {
                    roaming().control(entity)
                }

                println(
                    "Spawned ${entity.entity::class.simpleName} " +
                            "at ${entity.position}"
                )

                true
            }
        },

        // Paint terrain on the world grid.
        WorldInputBinding.Tile(
            trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            enabled = {
                tools().mode == SandboxMode.PAINT
            }
        ) { x, y ->
            painter().beginPaint(x, y)
        },

        // Begin rectangular object placement.
        WorldInputBinding.Tile(
            trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT),
            enabled = {
                tools().mode == SandboxMode.BUILD
            }
        ) { x, y ->
            buildDrag().begin(
                TilePosition(x, y)
            )
        },

        // Continue painting while dragging.
        WorldInputBinding.Grid(
            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
            enabled = {
                tools().mode == SandboxMode.PAINT
            }
        ) { x, y ->
            painter().dragPaint(x, y)
        },

        // Update the rectangular build preview while dragging.
        WorldInputBinding.Grid(
            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.LEFT),
            enabled = {
                tools().mode == SandboxMode.BUILD
            }
        ) { x, y ->
            buildDrag().dragTo(
                TilePosition(x, y)
            )
        },

        // Commit valid origins even when release is outside the world.
        WorldInputBinding.Grid(
            trigger = WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
            enabled = {
                tools().mode == SandboxMode.BUILD
            }
        ) { x, y ->
            if (!buildDrag().active) {
                false
            } else {
                val placed = buildDrag().finish(
                    TilePosition(x, y)
                )

                if (placed.isNotEmpty()) {
                    println(
                        "Placed ${placed.size} object(s)"
                    )
                    playBuildingSound()
                }

                true
            }
        },

        // Finish painting even when released outside the world.
        WorldInputBinding.NoPicking(
            trigger = WorldInputTrigger.MouseUp(Input.Buttons.LEFT),
            enabled = {
                tools().mode == SandboxMode.PAINT
            }
        ) {
            painter().endPaint()
        },

        // Begin erasing the selected overlay.
        WorldInputBinding.Tile(
            trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
            enabled = {
                tools().mode == SandboxMode.PAINT &&
                        painter().layerId != null
            }
        ) { x, y ->
            painter().beginErase(x, y)
        },

        // Continue erasing while dragging.
        WorldInputBinding.Grid(
            trigger = WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT),
            enabled = {
                tools().mode == SandboxMode.PAINT &&
                        painter().layerId != null
            }
        ) { x, y ->
            painter().dragErase(x, y)
        },

        // Finish erasing even when released outside the world.
        WorldInputBinding.NoPicking(
            trigger = WorldInputTrigger.MouseUp(Input.Buttons.RIGHT),
            enabled = {
                tools().mode == SandboxMode.PAINT
            }
        ) {
            painter().endErase()
        },

        // Remove objects outside painting and spawning modes.
        WorldInputBinding.Object(
            trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
            mode = ObjectPickingMode.SPRITE_OR_FOOTPRINT,
            enabled = {
                tools().mode == SandboxMode.NONE ||
                        tools().mode == SandboxMode.BUILD
            }
        ) { placed ->
            world().remove(placed)
            true
        },

        // Inspect the ground tile.
        WorldInputBinding.Tile(
            trigger = WorldInputTrigger.KeyDown(Input.Keys.P)
        ) { x, y ->
            println(
                "Tile at ($x, $y): ${world().getTile(x, y)}"
            )
            true
        },

        // Toggle painting mode.
        WorldInputBinding.NoPicking(
            trigger = WorldInputTrigger.KeyDown(Input.Keys.T)
        ) {
            val mode =
                if (tools().mode == SandboxMode.PAINT) {
                    SandboxMode.NONE
                } else {
                    SandboxMode.PAINT
                }

            tools().select(mode)

            println(
                "Sandbox mode: ${mode.displayName}"
            )

            true
        },

        // Cycle ground and overlay layers.
        WorldInputBinding.NoPicking(
            trigger = WorldInputTrigger.KeyDown(Input.Keys.O),
            enabled = {
                tools().mode == SandboxMode.PAINT
            }
        ) {
            painter().cycleLayer()

            println(
                "Selected layer: " +
                        (painter().layerId ?: "ground")
            )

            true
        }
    )
}