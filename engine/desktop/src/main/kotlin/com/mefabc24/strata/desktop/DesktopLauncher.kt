package com.mefabc24.strata.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.mefabc24.strata.StrataEngine
import com.mefabc24.strata.StrataGame

/** Basic settings owned by the LWJGL3 desktop backend. */
class DesktopSettings {
    var title: String = "Strata Engine"
        set(value) {
            require(value.isNotBlank()) {
                "Desktop window title must not be blank."
            }

            field = value
        }

    var width: Int = 1280
        set(value) {
            require(value > 0) {
                "Desktop window width must be positive."
            }

            field = value
        }

    var height: Int = 720
        set(value) {
            require(value > 0) {
                "Desktop window height must be positive."
            }

            field = value
        }

    var vsync: Boolean = true

    var foregroundFps: Int = 60
        set(value) {
            require(value > 0) {
                "Desktop foreground FPS must be positive."
            }

            field = value
        }

    internal fun applyTo(config: Lwjgl3ApplicationConfiguration) {
        config.setTitle(title)
        config.setWindowedMode(width, height)
        config.useVsync(vsync)
        config.setForegroundFPS(foregroundFps)
    }
}

object DesktopLauncher {
    fun launch(
        game: StrataGame,
        configure: DesktopSettings.() -> Unit = {}
    ) {
        val settings = DesktopSettings().apply(configure)
        val config = Lwjgl3ApplicationConfiguration()
        settings.applyTo(config)

        Lwjgl3Application(
            StrataEngine(game),
            config
        )
    }
}
