package com.mefabc24.sandbox

import com.mefabc24.strata.desktop.DesktopLauncher
import com.badlogic.gdx.graphics.Color

fun main() {
    DesktopLauncher.launch(
        game = SandboxGame(),
        title = "Sandbox",
        width = 1280,
        height = 720,
        backgroundColor = Color(0.53f, 0.81f, 0.92f, 1f)
    )
}