package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import kotlin.test.Test
import kotlin.test.assertEquals

class StrataStackVisibilityTest {
    @Test
    fun `stack preferred height follows only the visible section`() {
        val context = StrataUiContext(
            skin = Skin(),
            theme = StrataUiTheme(spacing = 0f)
        )
        val stack = StrataStack(context)
        val shortSection = stack.actor(PreferredWidget(160f, 40f))
        val tallSection = stack.actor(PreferredWidget(160f, 240f)).apply {
            isVisible = false
        }

        try {
            assertEquals(40f, stack.prefHeight)

            shortSection.isVisible = false
            tallSection.isVisible = true
            stack.invalidateHierarchy()

            assertEquals(240f, stack.prefHeight)

            tallSection.isVisible = false
            stack.invalidateHierarchy()

            assertEquals(0f, stack.prefHeight)
        } finally {
            context.dispose()
            context.skin.dispose()
        }
    }

    private class PreferredWidget(
        private val preferredWidth: Float,
        private val preferredHeight: Float
    ) : Widget() {
        override fun getPrefWidth(): Float = preferredWidth
        override fun getPrefHeight(): Float = preferredHeight
    }
}
