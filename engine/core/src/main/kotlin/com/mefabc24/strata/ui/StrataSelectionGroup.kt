package com.mefabc24.strata.ui

/**
 * Maintains one selected value from a fixed set of options.
 *
 * The group contains no rendering code. UI controls can bind to it, while
 * game code can also select values directly. Required groups always have a
 * selection; optional groups can be cleared.
 */
class StrataSelectionGroup<T>(
    options: Iterable<T>,
    initialSelection: T? = null,
    val selectionRequired: Boolean = true
) {

    private val optionSet = LinkedHashSet<T>().apply {
        for (option in options) {
            require(option != null) {
                "Selection options must not contain null."
            }

            require(add(option)) {
                "Selection options must be unique. Duplicate: '$option'."
            }
        }
    }

    private val listeners = linkedSetOf<(T?) -> Unit>()
    private val attachments = mutableSetOf<T>()

    /**
     * Values accepted by this group, in declaration order.
     */
    val options: Set<T>
        get() = optionSet

    /**
     * The selected value, or null when this optional group is cleared.
     */
    var selected: T? = resolveInitialSelection(initialSelection)
        private set

    init {
        require(optionSet.isNotEmpty() || !selectionRequired) {
            "A required selection group must contain at least one option."
        }
    }

    private fun resolveInitialSelection(initialSelection: T?): T? {
        if (initialSelection != null) {
            require(initialSelection in optionSet) {
                "Initial selection '$initialSelection' is not an option."
            }

            return initialSelection
        }

        return if (selectionRequired) {
            optionSet.firstOrNull()
        } else {
            null
        }
    }

    /**
     * Selects [value].
     *
     * Returns true when the selection changed. Values not declared in
     * [options] are rejected.
     */
    fun select(value: T): Boolean {
        require(value in optionSet) {
            "Value '$value' is not an option in this selection group."
        }

        if (selected == value) return false

        selected = value
        notifyListeners()
        return true
    }

    /**
     * Clears an optional selection.
     *
     * Returns false when the group is required or already cleared.
     */
    fun clearSelection(): Boolean {
        if (selectionRequired || selected == null) return false

        selected = null
        notifyListeners()
        return true
    }

    /**
     * Registers a callback for subsequent selection changes.
     *
     * The callback is not invoked immediately. Dispose the returned handle
     * when the listener has a shorter lifetime than this group.
     */
    fun onSelectionChanged(
        listener: (T?) -> Unit
    ): StrataSelectionSubscription {
        listeners += listener

        return StrataSelectionSubscription {
            listeners -= listener
        }
    }

    internal fun attach(value: T) {
        require(value in optionSet) {
            "Value '$value' is not an option in this selection group."
        }

        check(attachments.add(value)) {
            "A selection control is already attached to '$value'."
        }
    }

    internal fun detach(value: T) {
        attachments -= value
    }

    private fun notifyListeners() {
        for (listener in listeners.toList()) {
            listener(selected)
        }
    }
}

/**
 * Removes a selection-change callback when disposed.
 */
fun interface StrataSelectionSubscription {
    fun dispose()
}
