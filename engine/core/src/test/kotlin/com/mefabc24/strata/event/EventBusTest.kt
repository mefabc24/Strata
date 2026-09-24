package com.mefabc24.strata.event

import kotlin.test.Test
import kotlin.test.assertEquals

class EventBusTest {

    private data class TestEvent(
        val value: Int
    )

    private data class OtherEvent(
        val value: String
    )

    @Test
    fun `publishes event to matching listener`() {
        val events = EventBus()

        var received = 0

        events.subscribe<TestEvent> { event ->
            received = event.value
        }

        events.publish(
            TestEvent(42)
        )

        assertEquals(
            42,
            received
        )
    }

    @Test
    fun `does not publish event to different type`() {
        val events = EventBus()

        var received = 0

        events.subscribe<TestEvent> {
            received++
        }

        events.publish(
            OtherEvent("test")
        )

        assertEquals(
            0,
            received
        )
    }

    @Test
    fun `publishes event to multiple listeners`() {
        val events = EventBus()

        val received = mutableListOf<Int>()

        events.subscribe<TestEvent> { event ->
            received.add(event.value)
        }

        events.subscribe<TestEvent> { event ->
            received.add(event.value * 2)
        }

        events.publish(
            TestEvent(10)
        )

        assertEquals(
            listOf(10, 20),
            received
        )
    }

    @Test
    fun `subscription can be removed`() {
        val events = EventBus()

        var received = 0

        val subscription =
            events.subscribe<TestEvent> {
                received++
            }

        events.publish(
            TestEvent(1)
        )

        subscription.unsubscribe()

        events.publish(
            TestEvent(2)
        )

        assertEquals(
            1,
            received
        )
    }

    @Test
    fun `subscription can be removed multiple times`() {
        val events = EventBus()

        var received = 0

        val subscription =
            events.subscribe<TestEvent> {
                received++
            }

        subscription.unsubscribe()
        subscription.unsubscribe()

        events.publish(
            TestEvent(1)
        )

        assertEquals(
            0,
            received
        )
    }

    @Test
    fun `clear removes all listeners`() {
        val events = EventBus()

        var received = 0

        events.subscribe<TestEvent> {
            received++
        }

        events.subscribe<OtherEvent> {
            received++
        }

        events.clear()

        events.publish(
            TestEvent(1)
        )

        events.publish(
            OtherEvent("test")
        )

        assertEquals(
            0,
            received
        )
    }
}