package com.mefabc24.strata.event

import kotlin.reflect.KClass

/**
 * Dispatches typed events synchronously to registered listeners.
 *
 * Listeners are invoked in registration order.
 */
class EventBus {

    private val listeners =
        mutableMapOf<KClass<*>, MutableList<(Any) -> Unit>>()
    private val observers = mutableListOf<(Any) -> Unit>()

    /**
     * Registers a listener for the given event type.
     */
    inline fun <reified T : Any> subscribe(
        noinline listener: (T) -> Unit
    ): EventSubscription {
        return subscribe(
            eventType = T::class,
            listener = listener
        )
    }

    /**
     * Publishes an event to all listeners registered for its exact type.
     */
    fun publish(event: Any) {
        val eventListeners =
            listeners[event::class]?.toList().orEmpty()

        for (listener in eventListeners) {
            listener(event)
        }

        for (observer in observers.toList()) {
            observer(event)
        }
    }

    /** Observes every published event without changing typed subscriptions. */
    fun observe(observer: (Any) -> Unit): EventSubscription {
        observers += observer
        return EventSubscription { observers.remove(observer) }
    }

    /**
     * Removes all registered listeners.
     */
    fun clear() {
        listeners.clear()
        observers.clear()
    }

    @PublishedApi
    internal fun <T : Any> subscribe(
        eventType: KClass<T>,
        listener: (T) -> Unit
    ): EventSubscription {
        val wrappedListener: (Any) -> Unit = { event ->
            @Suppress("UNCHECKED_CAST")
            listener(event as T)
        }

        listeners
            .getOrPut(eventType) { mutableListOf() }
            .add(wrappedListener)

        return EventSubscription {
            unsubscribe(
                eventType = eventType,
                listener = wrappedListener
            )
        }
    }

    private fun unsubscribe(
        eventType: KClass<*>,
        listener: (Any) -> Unit
    ) {
        val eventListeners =
            listeners[eventType] ?: return

        eventListeners.remove(listener)

        if (eventListeners.isEmpty()) {
            listeners.remove(eventType)
        }
    }
}

/**
 * Represents an active event subscription.
 */
class EventSubscription internal constructor(
    private val unsubscribeAction: () -> Unit
) {

    private var active = true

    /**
     * Removes the associated event listener.
     */
    fun unsubscribe() {
        if (!active) return

        active = false
        unsubscribeAction()
    }
}
