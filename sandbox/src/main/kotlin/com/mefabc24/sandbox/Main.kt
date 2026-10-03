package com.mefabc24.sandbox

import com.mefabc24.strata.desktop.DesktopLauncher

fun main() {
    DesktopLauncher.launch(SandboxGame()) {
        title = "Sandbox"
        width = 1920
        height = 1080
        vsync = false
        foregroundFps = 0
    }
}
