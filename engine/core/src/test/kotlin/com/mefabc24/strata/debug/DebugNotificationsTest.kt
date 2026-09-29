package com.mefabc24.strata.debug

import com.mefabc24.strata.debug.ui.DebugOverlayHorizontal
import com.mefabc24.strata.debug.ui.DebugOverlayVertical
import com.mefabc24.strata.debug.ui.notificationAlignment
import com.mefabc24.strata.simulation.SimulationController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugNotificationsTest {

    @Test
    fun `notifications are enabled by default`() {
        assertTrue(DebugNotifications().enabled)
    }

    @Test
    fun `notification position defaults to top center`() {
        assertEquals(
            DebugNotificationPosition.TOP_CENTER,
            DebugNotifications().position
        )
    }

    @Test
    fun `every notification position maps to expected overlay alignment`() {
        val expected = mapOf(
            DebugNotificationPosition.TOP_CENTER to (
                    DebugOverlayHorizontal.CENTER to DebugOverlayVertical.TOP
                    ),
            DebugNotificationPosition.BOTTOM_RIGHT to (
                    DebugOverlayHorizontal.RIGHT to DebugOverlayVertical.BOTTOM
                    )
        )

        expected.forEach { (position, alignment) ->
            val actual = notificationAlignment(position)

            assertEquals(
                alignment.first,
                actual.horizontal,
                position.name
            )
            assertEquals(
                alignment.second,
                actual.vertical,
                position.name
            )
        }
    }

    @Test
    fun `runtime position changes update alignment state`() {
        val notifications = DebugNotifications()

        notifications.position = DebugNotificationPosition.BOTTOM_RIGHT

        val alignment = notificationAlignment(notifications.position)

        assertEquals(
            DebugOverlayHorizontal.RIGHT,
            alignment.horizontal
        )
        assertEquals(
            DebugOverlayVertical.BOTTOM,
            alignment.vertical
        )
    }

    @Test
    fun `disabled notifications are not emitted`() {
        val notifications = DebugNotifications()

        notifications.enabled = false
        notifications.emit("hidden")

        assertTrue(notifications.visible.isEmpty())
    }

    @Test
    fun `disabling notifications clears visible notifications`() {
        val notifications = DebugNotifications()

        notifications.emit("visible")
        assertFalse(notifications.visible.isEmpty())

        notifications.enabled = false

        assertTrue(notifications.visible.isEmpty())
    }

    @Test
    fun `toast queue is bounded to newest notifications`() {
        val notifications = DebugNotifications(maxVisible = 2)

        notifications.emit("first")
        notifications.emit(
            "second",
            DebugNotificationSeverity.WARNING
        )
        notifications.emit(
            "third",
            DebugNotificationSeverity.ERROR
        )

        assertEquals(
            listOf("second", "third"),
            notifications.visible.map { it.message }
        )
        assertEquals(
            listOf(
                DebugNotificationSeverity.WARNING,
                DebugNotificationSeverity.ERROR
            ),
            notifications.visible.map { it.severity }
        )
    }

    @Test
    fun `notifications expire from real time delta`() {
        val notifications = DebugNotifications(
            defaultDurationSeconds = 1f
        )

        notifications.emit("short")

        notifications.update(0.75f)

        assertEquals(
            1,
            notifications.visible.size
        )

        notifications.update(0.25f)

        assertTrue(notifications.visible.isEmpty())
    }

    @Test
    fun `paused simulation does not stop real time expiration`() {
        val simulation = SimulationController().apply {
            pause()
        }
        val notifications = DebugNotifications(
            defaultDurationSeconds = 1f
        )

        notifications.emit("expires")

        val realDelta = 1f

        assertEquals(
            0f,
            simulation.simulationDelta(realDelta)
        )

        notifications.update(realDelta)

        assertTrue(notifications.visible.isEmpty())
    }
}