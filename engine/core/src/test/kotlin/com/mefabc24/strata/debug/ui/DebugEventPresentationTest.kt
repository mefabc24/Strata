package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugEventRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DebugEventPresentationTest {
    @Test
    fun `simple event falls back to readable value row`() {
        val presentation = presentDebugEvent(
            DebugEventRecord(1, "Heartbeat", "Heartbeat")
        )

        assertEquals(1, presentation.sequence)
        assertEquals("Heartbeat", presentation.eventType)
        assertEquals(listOf(DebugDiagnosticRow("Value", "Heartbeat")), presentation.fields)
    }

    @Test
    fun `data class style event is split into labeled fields`() {
        val presentation = presentDebugEvent(
            DebugEventRecord(
                12,
                "SandboxDebugEntitySpawned",
                "SandboxDebugEntitySpawned(entityType=Wolf, position=TilePosition(x=3, y=19))"
            )
        )

        assertEquals(
            listOf(
                DebugDiagnosticRow("Entity type", "Wolf"),
                DebugDiagnosticRow("Position", "(3, 19)")
            ),
            presentation.fields
        )
    }

    @Test
    fun `arbitrary and long values fall back safely`() {
        val longValue = "unstructured " + "x".repeat(700)
        val presentation = presentDebugEvent(
            DebugEventRecord(3, "ExternalEvent", longValue)
        )

        assertEquals("Value", presentation.fields.single().key)
        assertTrue(presentation.fields.single().value.endsWith("…"))
        assertEquals(500, presentation.fields.single().value.length)
    }
}
