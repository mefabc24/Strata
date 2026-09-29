package com.mefabc24.strata.debug

import com.mefabc24.strata.simulation.SimulationController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugNotificationsTest {
    @Test
    fun `toast queue is bounded to newest notifications`() {
        val notifications = DebugNotifications(maxVisible = 2)

        notifications.emit("first")
        notifications.emit("second", DebugNotificationSeverity.WARNING)
        notifications.emit("third", DebugNotificationSeverity.ERROR)

        assertEquals(listOf("second", "third"), notifications.visible.map { it.message })
        assertEquals(
            listOf(DebugNotificationSeverity.WARNING, DebugNotificationSeverity.ERROR),
            notifications.visible.map { it.severity }
        )
    }

    @Test
    fun `notifications expire from real time delta`() {
        val notifications = DebugNotifications(defaultDurationSeconds = 1f)
        notifications.emit("short")

        notifications.update(0.75f)
        assertEquals(1, notifications.visible.size)
        notifications.update(0.25f)

        assertTrue(notifications.visible.isEmpty())
    }

    @Test
    fun `paused simulation does not stop real time expiration`() {
        val simulation = SimulationController().apply { pause() }
        val notifications = DebugNotifications(defaultDurationSeconds = 1f)
        notifications.emit("expires")

        val realDelta = 1f
        assertEquals(0f, simulation.simulationDelta(realDelta))
        notifications.update(realDelta)

        assertTrue(notifications.visible.isEmpty())
    }
}
