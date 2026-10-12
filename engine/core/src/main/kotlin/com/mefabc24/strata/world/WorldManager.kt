package com.mefabc24.strata.world

import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.placement.PlacementController
import com.mefabc24.strata.scene.SceneWorldView
import com.mefabc24.strata.terrain.TerrainId

/** Stable identifier for a world registered in one Strata runtime. */
@JvmInline
value class WorldId(val value: String) {
    init {
        require(value.isNotBlank()) { "World ID must not be blank." }
    }

    override fun toString(): String = value
}

/** Determines which registered worlds receive simulation updates. */
enum class InactiveWorldPolicy {
    /** Only the displayed world advances. This is the default. */
    PAUSE,

    /** Every registered world advances, whether or not it is displayed. */
    UPDATE
}

internal data class WorldRuntime(
    val id: WorldId,
    val world: World,
    val terrainFor: (Tile) -> TerrainId,
    val view: SceneWorldView,
    val placement: PlacementController?
)

/**
 * Registers game-owned worlds and coordinates their engine-owned views.
 *
 * A registered [World] is never disposed by Strata. Its view and placement
 * controller are created once, retained while inactive, and disposed when the
 * registration is removed or the runtime ends. Retaining the view preserves
 * camera and presentation state across switches.
 * An unfinished placement path is cancelled when its world is deactivated.
 */
@Suppress("unused")
class WorldManager internal constructor(
    private val createRuntime: (WorldId, World, (Tile) -> TerrainId) -> WorldRuntime,
    private val activationChanged: (WorldRuntime?, WorldRuntime?) -> Unit
) {
    private val entries = linkedMapOf<WorldId, WorldRuntime>()
    private var disposed = false

    /** Controls simulation of worlds other than [activeWorld]. */
    var inactiveWorldPolicy: InactiveWorldPolicy = InactiveWorldPolicy.PAUSE

    /** Registered identifiers in declaration order. */
    val ids: Set<WorldId>
        get() = entries.keys.toSet()

    /** Identifier of the displayed world, or null on UI-only screens. */
    var activeId: WorldId? = null
        private set

    /** Displayed logical world, or null when no world is active. */
    val activeWorld: World?
        get() = activeRuntime?.world

    /** Displayed public view, or null when no world is active. */
    val activeView: IsoWorldView?
        get() = activeRuntime?.view?.publicView

    /** Placement controller for the displayed world, when configured. */
    val activePlacement: PlacementController?
        get() = activeRuntime?.placement

    internal val activeRuntime: WorldRuntime?
        get() = activeId?.let(entries::get)

    fun register(
        id: WorldId,
        world: World,
        terrainFor: (Tile) -> TerrainId
    ): World {
        checkActive()
        require(id !in entries) { "World '$id' is already registered." }
        require(entries.values.none { it.world === world }) {
            "This World instance is already registered as '${entries.values.first { it.world === world }.id}'."
        }

        entries[id] = createRuntime(id, world, terrainFor)
        return world
    }

    fun register(
        id: String,
        world: World,
        terrainFor: (Tile) -> TerrainId
    ): World = register(WorldId(id), world, terrainFor)

    operator fun get(id: WorldId): World = entries[id]?.world
        ?: error("World '$id' is not registered.")

    operator fun get(id: String): World = get(WorldId(id))

    fun find(id: WorldId): World? = entries[id]?.world

    fun contains(id: WorldId): Boolean = id in entries

    fun activate(id: WorldId): World {
        checkActive()
        val next = entries[id] ?: error("World '$id' is not registered.")
        val previous = activeRuntime
        if (previous === next) return next.world

        activationChanged(previous, next)
        previous?.placement?.path?.cancel()
        activeId = id
        return next.world
    }

    fun activate(id: String): World = activate(WorldId(id))

    /** Hides the current world without removing or resetting it. */
    fun deactivate() {
        checkActive()
        val previous = activeRuntime ?: return
        activationChanged(previous, null)
        previous.placement?.path?.cancel()
        activeId = null
    }

    /**
     * Removes a registration and disposes only its engine-owned view.
     *
     * The returned logical world remains owned by the game.
     */
    fun remove(id: WorldId): World? {
        checkActive()
        val runtime = entries[id] ?: return null
        if (activeId == id) deactivate()
        entries.remove(id)
        runtime.placement?.path?.cancel()
        runtime.view.dispose()
        return runtime.world
    }

    fun remove(id: String): World? = remove(WorldId(id))

    internal fun runtime(id: WorldId): WorldRuntime = entries[id]
        ?: error("World '$id' is not registered.")

    internal fun update(
        realDelta: Float,
        simulationDelta: Float,
        updateWorld: (World, Float, Boolean) -> Unit
    ) {
        val active = activeRuntime
        if (inactiveWorldPolicy == InactiveWorldPolicy.UPDATE) {
            entries.values.forEach { runtime ->
                updateWorld(runtime.world, simulationDelta, runtime === active)
            }
        } else if (active != null) {
            updateWorld(active.world, simulationDelta, true)
        }

        active?.let { runtime ->
            runtime.view.update(realDelta, simulationDelta)
            runtime.placement?.update(runtime.view.hoveredTile)
        }
    }

    internal fun render() {
        activeRuntime?.let { runtime ->
            runtime.view.render(runtime.placement?.previews.orEmpty())
        }
    }

    internal fun resize(width: Int, height: Int) {
        entries.values.forEach { it.view.resize(width, height) }
    }

    internal fun dispose() {
        if (disposed) return
        disposed = true
        val previous = activeRuntime
        if (previous != null) activationChanged(previous, null)
        activeId = null
        entries.values.toList().asReversed().forEach {
            it.placement?.path?.cancel()
            it.view.dispose()
        }
        entries.clear()
    }

    private fun checkActive() {
        check(!disposed) { "WorldManager has already been disposed." }
    }
}
