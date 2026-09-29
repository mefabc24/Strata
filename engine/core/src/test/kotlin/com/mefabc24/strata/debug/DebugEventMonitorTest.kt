package com.mefabc24.strata.debug

import com.mefabc24.strata.event.EventBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugEventMonitorTest {
    @Test
    fun `history is bounded and retains newest events`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus, capacity = 2).apply { enabled = true }

        bus.publish(TestEvent(1))
        bus.publish(TestEvent(2))
        bus.publish(TestEvent(3))

        assertEquals(listOf(2L, 3L), monitor.records.map { it.sequence })
        assertEquals(listOf("TestEvent(value=2)", "TestEvent(value=3)"), monitor.records.map { it.value })
    }

    @Test
    fun `paused capture does not stop typed dispatch`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus).apply {
            enabled = true
            paused = true
        }
        var dispatched = 0
        bus.subscribe<TestEvent> { dispatched++ }

        bus.publish(TestEvent(1))
        monitor.paused = false
        bus.publish(TestEvent(2))

        assertEquals(2, dispatched)
        assertEquals(listOf("TestEvent(value=2)"), monitor.records.map { it.value })
    }

    @Test
    fun `clear removes captured history`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus).apply { enabled = true }
        bus.publish(TestEvent(1))

        monitor.clear()

        assertTrue(monitor.records.isEmpty())
    }

    @Test
    fun `disabled monitor does not interfere with dispatch`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus)
        var dispatched = 0
        bus.subscribe<TestEvent> { dispatched++ }

        bus.publish(TestEvent(1))

        assertEquals(1, dispatched)
        assertTrue(monitor.records.isEmpty())
    }

    @Test
    fun `monitor preserves original long event value`() {
        val bus = EventBus()
        val monitor = DebugEventMonitor(bus).apply { enabled = true }
        val value = "x".repeat(1_000)

        bus.publish(ValueEvent(value))

        assertEquals(ValueEvent(value).toString(), monitor.records.single().value)
    }

    private data class TestEvent(val value: Int)
    private data class ValueEvent(val value: String)
}
