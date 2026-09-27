package com.mefabc24.strata.render.sprite

import java.lang.ref.WeakReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VisualStateTest {
    @Test
    fun `local playback follows a stable identity across transient contexts`() {
        val runtimeObject = Any()
        val prepared = localDefinition().prepare { it }

        val entered = prepared.resolve(
            RuntimeContext(runtimeObject, TestState.FIRST),
            animationTime = 4f
        )
        val continued = prepared.resolve(
            RuntimeContext(runtimeObject, TestState.FIRST),
            animationTime = 4.75f
        )

        assertEquals(0f, entered.stateTime)
        assertEquals(0.75f, continued.stateTime)
    }

    @Test
    fun `changing state resets local playback for the same identity`() {
        val runtimeObject = Any()
        val prepared = localDefinition().prepare { it }

        prepared.resolve(RuntimeContext(runtimeObject, TestState.FIRST), 2f)
        assertEquals(
            1f,
            prepared.resolve(
                RuntimeContext(runtimeObject, TestState.FIRST),
                3f
            ).stateTime
        )
        assertEquals(
            0f,
            prepared.resolve(
                RuntimeContext(runtimeObject, TestState.SECOND),
                3.5f
            ).stateTime
        )
    }

    @Test
    fun `equal but distinct identities keep independent local clocks`() {
        val first = EqualIdentity()
        val second = EqualIdentity()
        val prepared = localDefinition().prepare { it }

        prepared.resolve(RuntimeContext(first, TestState.FIRST), 1f)
        prepared.resolve(RuntimeContext(second, TestState.FIRST), 2f)

        assertEquals(
            2f,
            prepared.resolve(RuntimeContext(first, TestState.FIRST), 3f).stateTime
        )
        assertEquals(
            1f,
            prepared.resolve(RuntimeContext(second, TestState.FIRST), 3f).stateTime
        )
    }

    @Test
    fun `synchronized states use global time without tracking identities`() {
        val prepared = VisualDefinition.Stateful<RuntimeContext, String>(
            states = mapOf(
                TestState.FIRST to VisualStateDefinition(
                    source = "first",
                    playback = VisualPlayback.SYNCHRONIZED
                )
            ),
            stateFor = RuntimeContext::state,
            playbackIdentityFor = RuntimeContext::identity
        ).prepare { it } as PreparedVisualDefinition.Stateful<RuntimeContext, String>

        assertEquals(
            8f,
            prepared.resolve(
                RuntimeContext(Any(), TestState.FIRST),
                animationTime = 8f
            ).stateTime
        )
        assertEquals(0, prepared.trackedIdentityCount)
    }

    @Test
    fun `synchronized state between local states resets local playback`() {
        val runtimeObject = Any()
        val prepared = VisualDefinition.Stateful<RuntimeContext, String>(
            states = mapOf(
                TestState.FIRST to VisualStateDefinition(
                    source = "local",
                    playback = VisualPlayback.LOCAL
                ),
                TestState.SECOND to VisualStateDefinition(
                    source = "synchronized",
                    playback = VisualPlayback.SYNCHRONIZED
                )
            ),
            stateFor = RuntimeContext::state,
            playbackIdentityFor = RuntimeContext::identity
        ).prepare { it }

        prepared.resolve(RuntimeContext(runtimeObject, TestState.FIRST), 2f)
        assertEquals(
            1f,
            prepared.resolve(
                RuntimeContext(runtimeObject, TestState.FIRST),
                3f
            ).stateTime
        )
        assertEquals(
            4f,
            prepared.resolve(
                RuntimeContext(runtimeObject, TestState.SECOND),
                4f
            ).stateTime
        )
        assertEquals(
            5f,
            prepared.resolve(
                RuntimeContext(runtimeObject, TestState.SECOND),
                5f
            ).stateTime
        )
        assertEquals(
            0f,
            prepared.resolve(
                RuntimeContext(runtimeObject, TestState.FIRST),
                6f
            ).stateTime
        )
    }

    @Test
    fun `weak identity map releases collected keys`() {
        val map = WeakIdentityMap<String>()
        var key: Any? = Any()
        val reference = WeakReference(key)
        map[key!!] = "value"
        key = null

        repeat(100) {
            if (reference.get() == null) return@repeat
            System.gc()
            Thread.sleep(5)
        }

        assertNull(reference.get())
        assertEquals(0, map.size)
    }

    private fun localDefinition(): VisualDefinition<RuntimeContext, String> {
        return VisualDefinition.Stateful(
            states = TestState.entries.associateWith { state ->
                VisualStateDefinition(
                    source = state.toString(),
                    playback = VisualPlayback.LOCAL
                )
            },
            stateFor = RuntimeContext::state,
            playbackIdentityFor = RuntimeContext::identity
        )
    }

    private data class RuntimeContext(
        val identity: Any,
        val state: TestState
    )

    private class EqualIdentity {
        override fun equals(other: Any?): Boolean = other is EqualIdentity
        override fun hashCode(): Int = 1
    }

    private enum class TestState : VisualStateId {
        FIRST,
        SECOND
    }
}
