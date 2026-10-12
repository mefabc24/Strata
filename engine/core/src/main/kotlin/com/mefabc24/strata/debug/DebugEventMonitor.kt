package com.mefabc24.strata.debug

import com.mefabc24.strata.event.EventBus
import com.mefabc24.strata.event.EventSubscription

/** One bounded diagnostic record captured from a scene event bus. */
data class DebugEventRecord(
    val sequence: Long,
    val eventType: String,
    val value: String
)

/** Captures recent published events without participating in dispatch. */
class DebugEventMonitor(
    eventBus: EventBus,
    val capacity: Int = 100
) {
    init {
        require(capacity > 0) { "Event monitor capacity must be positive." }
    }

    private val history = ArrayDeque<DebugEventRecord>(capacity)
    private var nextSequence = 1L
    private val subscription: EventSubscription = eventBus.observe(::capture)

    /** Whether published events are appended to [records]. */
    var captureEnabled: Boolean = true

    /** Captured events in oldest-to-newest order. */
    val records: List<DebugEventRecord>
        get() = history.toList()

    fun clear() {
        history.clear()
    }

    fun dispose() {
        subscription.unsubscribe()
    }

    private fun capture(event: Any) {
        if (!captureEnabled) return
        if (history.size == capacity) history.removeFirst()
        history.addLast(
            DebugEventRecord(
                sequence = nextSequence++,
                eventType = event::class.simpleName ?: event::class.toString(),
                value = event.toString()
            )
        )
    }
}
