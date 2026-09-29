package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.mefabc24.strata.debug.DebugNotificationSeverity
import com.mefabc24.strata.debug.DebugNotifications
import com.mefabc24.strata.ui.StrataUi

/** Renders the debug notification queue independently from game UI. */
internal class DebugNotificationOverlay(
    private val ui: StrataUi,
    private val notifications: DebugNotifications
) {
    private val root = Table().apply {
        setFillParent(true)
        top().right().pad(16f)
        touchable = Touchable.disabled
    }
    private var renderedIds: List<Long> = emptyList()

    init {
        ui.stage.addActor(root)
    }

    fun sync() {
        val visible = notifications.visible.asReversed()
        val ids = visible.map { it.id }
        if (ids == renderedIds) return
        renderedIds = ids
        root.clearChildren()
        visible.forEach { notification ->
            root.add(Table(ui.skin).apply {
                background = toastBackground(notification.severity)
                pad(9f, 12f, 9f, 12f)
                add(Label(notification.message, ui.skin).apply {
                    color = textColor(notification.severity)
                    setWrap(true)
                }).width(300f).left()
            }).width(324f).padBottom(6f).row()
        }
    }

    private fun toastBackground(severity: DebugNotificationSeverity): Drawable =
        ui.skin.newDrawable(
            "debug-white",
            when (severity) {
                DebugNotificationSeverity.INFO -> Color(0.10f, 0.25f, 0.38f, 0.96f)
                DebugNotificationSeverity.SUCCESS -> Color(0.10f, 0.34f, 0.20f, 0.96f)
                DebugNotificationSeverity.WARNING -> Color(0.43f, 0.30f, 0.08f, 0.96f)
                DebugNotificationSeverity.ERROR -> Color(0.43f, 0.12f, 0.14f, 0.96f)
            }
        )

    private fun textColor(severity: DebugNotificationSeverity): Color =
        when (severity) {
            DebugNotificationSeverity.INFO -> Color(0.75f, 0.90f, 1f, 1f)
            DebugNotificationSeverity.SUCCESS -> Color(0.75f, 1f, 0.82f, 1f)
            DebugNotificationSeverity.WARNING -> Color(1f, 0.91f, 0.65f, 1f)
            DebugNotificationSeverity.ERROR -> Color(1f, 0.78f, 0.78f, 1f)
        }
}
