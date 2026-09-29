package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.mefabc24.strata.debug.DebugNotificationSeverity
import com.mefabc24.strata.debug.DebugNotifications
import com.mefabc24.strata.debug.DebugNotificationPosition
import com.mefabc24.strata.ui.StrataUi
import com.badlogic.gdx.utils.Align

/** Renders the debug notification queue independently from game UI. */
internal class DebugNotificationOverlay(
    private val ui: StrataUi,
    private val notifications: DebugNotifications
) {
    private val stack = Table()
    private val root = Table().apply {
        setFillParent(true)
        pad(16f)
        touchable = Touchable.disabled
    }
    private var renderedIds: List<Long> = emptyList()
    private var appliedPosition: DebugNotificationPosition? = null

    init {
        ui.stage.addActor(root)
    }

    fun sync() {
        root.isVisible = notifications.enabled

        if (!notifications.enabled) return

        applyPosition()
        root.toFront()
        val alignment = notificationAlignment(notifications.position)
        val visible = if (alignment.vertical == DebugOverlayVertical.TOP) {
            notifications.visible.asReversed()
        } else {
            notifications.visible
        }
        val ids = visible.map { it.id }
        if (ids == renderedIds) return
        renderedIds = ids
        stack.clearChildren()
        visible.forEach { notification ->
            stack.add(Table(ui.skin).apply {
                background = toastBackground(notification.severity)
                pad(9f, 12f, 9f, 12f)
                add(Label(notification.message, ui.skin).apply {
                    color = textColor(notification.severity)
                    setWrap(true)
                }).width(300f).left()
            }).width(324f).padBottom(6f).row()
        }
    }

    private fun applyPosition() {
        val position = notifications.position
        if (appliedPosition == position) return

        appliedPosition = position
        root.clearChildren()

        when (position) {
            DebugNotificationPosition.TOP_CENTER ->
                root.align(Align.top)

            DebugNotificationPosition.BOTTOM_RIGHT ->
                root.align(Align.bottom or Align.right)
        }

        root.add(stack).width(324f)
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

internal enum class DebugOverlayHorizontal { LEFT, CENTER, RIGHT }
internal enum class DebugOverlayVertical { TOP, BOTTOM }

internal data class DebugNotificationAlignment(
    val horizontal: DebugOverlayHorizontal,
    val vertical: DebugOverlayVertical
)

internal fun notificationAlignment(
    position: DebugNotificationPosition
): DebugNotificationAlignment = when (position) {
    DebugNotificationPosition.TOP_CENTER ->
        DebugNotificationAlignment(
            DebugOverlayHorizontal.CENTER,
            DebugOverlayVertical.TOP
        )

    DebugNotificationPosition.BOTTOM_RIGHT ->
        DebugNotificationAlignment(
            DebugOverlayHorizontal.RIGHT,
            DebugOverlayVertical.BOTTOM
        )
}