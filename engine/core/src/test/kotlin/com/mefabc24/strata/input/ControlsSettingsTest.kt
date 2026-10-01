package com.mefabc24.strata.input

import com.badlogic.gdx.Input
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ControlsSettingsTest {
    @Test
    fun `copy snapshots camera and gameplay settings`() {
        val binding = WorldInputBinding.NoPicking(
            WorldInputTrigger.KeyDown(Input.Keys.SPACE)
        ) { true }
        val settings = ControlsSettings().apply {
            camera {
                moveUp = Input.Keys.UP
                dragButton = Input.Buttons.RIGHT
                keyboardMovementEnabled = false
            }
            gameplay { bindings = listOf(binding) }
        }

        val copy = settings.copy()
        settings.camera.moveUp = Input.Keys.W
        settings.gameplay.bindings = emptyList()

        assertEquals(Input.Keys.UP, copy.camera.moveUp)
        assertEquals(Input.Buttons.RIGHT, copy.camera.dragButton)
        assertFalse(copy.camera.keyboardMovementEnabled)
        assertEquals(listOf(binding), copy.gameplay.bindings)
    }

    @Test
    fun `gameplay bindings assignment snapshots the supplied list`() {
        val source = mutableListOf<WorldInputBinding>()
        val settings = GameplayControlsSettings()
        settings.bindings = source

        source += WorldInputBinding.NoPicking(
            WorldInputTrigger.KeyDown(Input.Keys.SPACE)
        ) { true }

        assertEquals(emptyList(), settings.bindings)
    }
}
