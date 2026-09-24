package com.mefabc24.sandbox

import com.mefabc24.strata.desktop.DesktopLauncher

fun main() {
    DesktopLauncher.launch(SandboxGame()) {
        title = "Sandbox"
        width = 1280
        height = 720
        vsync = true
        foregroundFps = 60
    }
}
