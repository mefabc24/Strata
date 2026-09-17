package com.mefabc24.sandbox

import com.mefabc24.strata.StrataGame

class SandboxGame : StrataGame {
    override fun create() {
        println("Strata sandbox initialized")
    }

    override fun update(delta: Float) {
        // update game logic
    }

    override fun render() {
        // render game world
    }

    override fun dispose() {
        println("Strata sandbox disposed")
    }
}