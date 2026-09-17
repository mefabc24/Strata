package com.mefabc24.strata

interface StrataGame {
    fun create()
    fun update(delta: Float)
    fun render()
    fun dispose()
    fun resize(width: Int, height: Int)
}