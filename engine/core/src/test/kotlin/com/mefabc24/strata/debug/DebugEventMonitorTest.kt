package com.mefabc24.strata.debug

import com.mefabc24.strata.event.EventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugEventMonitorTest {
    @Test
    fun `history is bounded and retains newest events`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus, capacity = 2).apply {
            captureEnabled = true
        }

        bus.publish(TestEvent(1))
        bus.publish(TestEvent(2))
        bus.publish(TestEvent(3))

        assertEquals(listOf(2L, 3L), monitor.records.map { it.sequence })
        assertEquals(listOf("TestEvent(value=2)", "TestEvent(value=3)"), monitor.records.map { it.value })
    }

    @Test
    fun `capture off does not collect and does not stop typed dispatch`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus)
        var dispatched = 0
        bus.subscribe<TestEvent> { dispatched++ }

        bus.publish(TestEvent(1))
        monitor.captureEnabled = true
        bus.publish(TestEvent(2))

        assertEquals(2, dispatched)
        assertEquals(listOf("TestEvent(value=2)"), monitor.records.map { it.value })
    }

    @Test
    fun `clear removes captured history`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus).apply { captureEnabled = true }
        bus.publish(TestEvent(1))

        monitor.clear()

        assertTrue(monitor.records.isEmpty())
    }

    @Test
    fun `capture defaults to off`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus)
        var dispatched = 0
        bus.subscribe<TestEvent> { dispatched++ }

        bus.publish(TestEvent(1))

        assertEquals(1, dispatched)
        assertTrue(monitor.records.isEmpty())
    }

    @Test
    fun `hidden monitor captures events for later presentation`() {
        val bus = EventBus()
        val settings = DebugEventMonitorSettings().apply {
            visible = false
            captureEnabled = true
        }
        val monitor = DebugEventMonitor(bus).apply {
            captureEnabled = settings.captureEnabled
        }

        bus.publish(TestEvent(1))
        bus.publish(TestEvent(2))
        settings.visible = true

        assertTrue(settings.visible)
        assertEquals(
            listOf("TestEvent(value=1)", "TestEvent(value=2)"),
            monitor.records.map { it.value }
        )
    }

    @Test
    fun `visibility changes preserve captured history`() {
        val bus = EventBus()
        val settings = DebugEventMonitorSettings().apply {
            visible = true
            captureEnabled = true
        }
        val monitor = DebugEventMonitor(bus).apply { captureEnabled = true }
        bus.publish(TestEvent(1))

        settings.visible = false
        settings.visible = true

        assertEquals(listOf("TestEvent(value=1)"), monitor.records.map { it.value })
    }

    @Test
    fun `capture changes preserve history and resume appending`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus).apply { captureEnabled = true }
        bus.publish(TestEvent(1))

        monitor.captureEnabled = false
        bus.publish(TestEvent(2))

        assertFalse(monitor.captureEnabled)
        assertEquals(listOf("TestEvent(value=1)"), monitor.records.map { it.value })

        monitor.captureEnabled = true
        bus.publish(TestEvent(3))

        assertEquals(
            listOf("TestEvent(value=1)", "TestEvent(value=3)"),
            monitor.records.map { it.value }
        )
    }

    @Test
    fun `monitor preserves original long event value`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus).apply { captureEnabled = true }
        val value = "x".repeat(1_000)

        bus.publish(ValueEvent(value))

        assertEquals(ValueEvent(value).toString(), monitor.records.single().value)
    }

    private data class TestEvent(val value: Int)
    private data class ValueEvent(val value: String)
}
