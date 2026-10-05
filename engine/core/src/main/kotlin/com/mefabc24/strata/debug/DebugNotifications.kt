package com.mefabc24.strata.debug

enum class DebugNotificationSeverity { INFO, SUCCESS, WARNING, ERROR }
enum class DebugNotificationPosition {
    TOP_CENTER,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_LEFT,
    BOTTOM_CENTER
}

data class DebugNotification(
    val id: Long,
    val message: String,
    val severity: DebugNotificationSeverity,
    internal var remainingSeconds: Float
)

/** Small bounded notification queue for developer feedback. */
class DebugNotifications(
    val maxVisible: Int = 4,
    val defaultDurationSeconds: Float = 3f
) {
    var enabled: Boolean = true
        set(value) {
            field = value

            if (!value) {
                clear()
            }
        }

    var position: DebugNotificationPosition = DebugNotificationPosition.TOP_CENTER
    init {
        require(maxVisible > 0) { "Maximum visible notifications must be positive." }
        require(defaultDurationSeconds.isFinite() && defaultDurationSeconds > 0f) {
            "Default notification duration must be finite and positive."
        }
    }

    private val notifications = ArrayDeque<DebugNotification>(maxVisible)
    private var nextId = 1L

    val visible: List<DebugNotification>
        get() = notifications.toList()

    fun emit(
        message: String,
        severity: DebugNotificationSeverity = DebugNotificationSeverity.INFO,
        durationSeconds: Float = defaultDurationSeconds
    ) {
        if (!enabled) return

        require(message.isNotBlank()) { "Notification message must not be blank." }
        require(durationSeconds.isFinite() && durationSeconds > 0f) {
            "Notification duration must be finite and positive."
        }

        if (notifications.size == maxVisible) notifications.removeFirst()
        notifications.addLast(
            DebugNotification(nextId++, message, severity, durationSeconds)
        )
    }

    internal fun update(realDelta: Float) {
        require(realDelta.isFinite() && realDelta >= 0f) {
            "Notification delta must be finite and non-negative."
        }
        notifications.forEach { it.remainingSeconds -= realDelta }
        notifications.removeAll { it.remainingSeconds <= 0f }
    }

    fun clear() {
        notifications.clear()
    }
}
