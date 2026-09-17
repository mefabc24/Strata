package com.mefabc24.strata

interface StrataGame {
    fun create()
    fun update(delta: Float)
    fun render()
    fun dispose()
}