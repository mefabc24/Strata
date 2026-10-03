package com.mefabc24.strata.debug

import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.WorldEntity
import java.util.IdentityHashMap

/** One entity's bounded movement history stored without per-frame list copies. */
internal class DebugEntityTrail internal constructor(capacity: Int) {
    private var xs = FloatArray(capacity)
    private var ys = FloatArray(capacity)
    private var times = DoubleArray(capacity)
    private var start = 0

    var size: Int = 0
        private set

    val capacity: Int
        get() = xs.size

    fun xAt(index: Int): Float = xs[offset(index)]
    fun yAt(index: Int): Float = ys[offset(index)]

    fun lastX(): Float = xs[offset(size - 1)]
    fun lastY(): Float = ys[offset(size - 1)]

    fun add(position: EntityPosition, time: Double) {
        if (size == capacity) {
            start = (start + 1) % capacity
            size--
        }
        val offset = offset(size)
        xs[offset] = position.x
        ys[offset] = position.y
        times[offset] = time
        size++
    }

    fun pruneBefore(cutoff: Double) {
        while (size > 1 && times[start] < cutoff) {
            start = (start + 1) % capacity
            size--
        }
    }

    fun resize(newCapacity: Int) {
        if (newCapacity == capacity) return
        val retained = minOf(size, newCapacity)
        val firstRetained = size - retained
        val newXs = FloatArray(newCapacity)
        val newYs = FloatArray(newCapacity)
        val newTimes = DoubleArray(newCapacity)
        for (index in 0 until retained) {
            val oldOffset = offset(firstRetained + index)
            newXs[index] = xs[oldOffset]
            newYs[index] = ys[oldOffset]
            newTimes[index] = times[oldOffset]
        }
        xs = newXs
        ys = newYs
        times = newTimes
        start = 0
        size = retained
    }

    private fun offset(index: Int): Int {
        require(index in 0 until size || index == size && size < capacity)
        return (start + index) % capacity
    }
}

/** Records optional entity movement history independently from simulation. */
internal class DebugEntityTrailRecorder {
    private val trails = IdentityHashMap<WorldEntity, DebugEntityTrail>()
    private var elapsedSeconds = 0.0
    private var appliedClearGeneration = 0L

    fun update(
        entities: Set<WorldEntity>,
        simulationDelta: Float,
        settings: DebugEntitySettings
    ) {
        require(simulationDelta.isFinite() && simulationDelta >= 0f) {
            "Entity trail delta must be finite and non-negative."
        }
        if (appliedClearGeneration != settings.trailClearGeneration) {
            clear()
            appliedClearGeneration = settings.trailClearGeneration
        }
        if (!settings.showMovementTrail) {
            if (trails.isNotEmpty()) clear()
            return
        }

        elapsedSeconds += simulationDelta.toDouble()
        retain(entities)
        val cutoff = elapsedSeconds - settings.trailHistoryDurationSeconds
        val minimumDistanceSquared =
            settings.trailMinimumDistance * settings.trailMinimumDistance
        for (entity in entities) {
            val trail = trails[entity] ?: DebugEntityTrail(
                settings.trailMaxPositions
            ).also { trails[entity] = it }
            trail.resize(settings.trailMaxPositions)
            val position = entity.position
            val shouldRecord = if (trail.size == 0) {
                true
            } else {
                val dx = position.x - trail.lastX()
                val dy = position.y - trail.lastY()
                (dx != 0f || dy != 0f) &&
                    (dx * dx + dy * dy >= minimumDistanceSquared || !entity.isMoving)
            }
            if (shouldRecord) trail.add(position, elapsedSeconds)
            trail.pruneBefore(cutoff)
        }
    }

    fun trail(entity: WorldEntity): DebugEntityTrail? = trails[entity]

    fun clear() {
        trails.clear()
        elapsedSeconds = 0.0
    }

    private fun retain(entities: Set<WorldEntity>) {
        val iterator = trails.keys.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() !in entities) iterator.remove()
        }
    }
}
