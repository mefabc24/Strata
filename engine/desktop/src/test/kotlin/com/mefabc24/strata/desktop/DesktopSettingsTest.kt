package com.mefabc24.strata.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DesktopSettingsTest {

    @Test
    fun `defaults describe the standard Strata window`() {
        val settings = DesktopSettings()

        assertEquals("Strata Engine", settings.title)
        assertEquals(1280, settings.width)
        assertEquals(720, settings.height)
        assertEquals(true, settings.vsync)
        assertEquals(60, settings.foregroundFps)
    }

    @Test
    fun `invalid window settings are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            DesktopSettings().title = " "
        }

        assertFailsWith<IllegalArgumentException> {
            DesktopSettings().width = 0
        }

        assertFailsWith<IllegalArgumentException> {
            DesktopSettings().height = -1
        }

        assertFailsWith<IllegalArgumentException> {
            DesktopSettings().foregroundFps = 0
        }
    }

    @Test
    fun `settings are applied to the lwjgl configuration`() {
        val settings = DesktopSettings().apply {
            title = "Configured"
            width = 960
            height = 540
            vsync = false
            foregroundFps = 144
        }

        val config = Lwjgl3ApplicationConfiguration()
        settings.applyTo(config)

        assertEquals("Configured", config.field("title"))
        assertEquals(960, config.field("windowWidth"))
        assertEquals(540, config.field("windowHeight"))
        assertFalse(config.field("vSyncEnabled"))
        assertEquals(144, config.field("foregroundFPS"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any.field(name: String): T {
        var type: Class<*>? = javaClass

        while (type != null) {
            val field = runCatching {
                type.getDeclaredField(name)
            }.getOrNull()

            if (field != null) {
                field.isAccessible = true
                return field.get(this) as T
            }

            type = type.superclass
        }

        error("Field '$name' was not found.")
    }
}
