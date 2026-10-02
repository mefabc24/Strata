package com.mefabc24.strata.debug

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Graphics
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.testing.defaultValue
import com.mefabc24.strata.testing.proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DebugPerformanceLoggerTest {
    private val logs = mutableListOf<Pair<String, String>>()

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
        Gdx.app = proxy(Application::class.java) { _, method, arguments ->
            when (method.name) {
                "log" -> {
                    logs += arguments!![0] as String to arguments[1] as String
                    null
                }
                "getType" -> Application.ApplicationType.HeadlessDesktop
                else -> defaultValue(method.returnType)
            }
        }
        Gdx.graphics = proxy(Graphics::class.java) { _, method, _ ->
            when (method.name) {
                "getFramesPerSecond" -> 77
                else -> defaultValue(method.returnType)
            }
        }
    }

    @Test
    fun `disabled logger ignores frames`() {
        val logger = DebugPerformanceLogger().apply { intervalSeconds = 0.01f }

        logger.record(RenderStats(), 1f)

        assertTrue(logs.isEmpty())
    }

    @Test
    fun `overlay activation does not enable terminal output`() {
        val performance = DebugPerformanceSettings().apply {
            overlayEnabled = true
            terminalLoggingIntervalSeconds = 0.01f
        }

        performance.record(RenderStats(), 0.01f)

        assertTrue(performance.overlayEnabled)
        assertFalse(performance.terminalLoggingEnabled)
        assertTrue(logs.isEmpty())
    }

    @Test
    fun `terminal output remains active when overlay is disabled`() {
        val performance = DebugPerformanceSettings().apply {
            terminalLoggingEnabled = true
            terminalLoggingIntervalSeconds = 0.02f
        }

        performance.record(RenderStats(), 0.01f)
        performance.overlayEnabled = true
        performance.overlayEnabled = false
        performance.record(RenderStats(), 0.01f)

        assertFalse(performance.overlayEnabled)
        assertTrue(performance.terminalLoggingEnabled)
        assertEquals(1, logs.size)
    }

    @Test
    fun `terminal output can be disabled while overlay remains active`() {
        val performance = DebugPerformanceSettings().apply {
            overlayEnabled = true
            terminalLoggingEnabled = true
            terminalLoggingIntervalSeconds = 0.01f
        }
        performance.record(RenderStats(), 0.01f)
        logs.clear()

        performance.terminalLoggingEnabled = false
        performance.record(RenderStats(), 0.01f)

        assertTrue(performance.overlayEnabled)
        assertFalse(performance.terminalLoggingEnabled)
        assertTrue(logs.isEmpty())
    }

    @Test
    fun `logger aggregates frame and renderer metrics at interval`() {
        val logger = DebugPerformanceLogger().apply {
            enabled = true
            intervalSeconds = 0.03f
        }
        val first = stats(
            renderMs = 2.0,
            staticMs = 1.0,
            staticUpdates = 1,
            relationChecks = 3,
            dynamicMs = 0.5
        )
        val second = stats(
            renderMs = 4.0,
            staticMs = 3.0,
            staticUpdates = 2,
            relationChecks = 5,
            dynamicMs = 1.5
        ).apply {
            terrainChecked = 10
            terrainDrawn = 7
            objectsChecked = 6
            objectsDrawn = 4
            entitiesChecked = 3
            entitiesDrawn = 2
            previewsDrawn = 1
            drawCalls = 9
        }

        logger.record(first, 0.01f)
        assertTrue(logs.isEmpty())
        logger.record(second, 0.02f)

        assertEquals("StrataPerf", logs.single().first)
        val message = normalizeDecimals(logs.single().second)
        assertTrue(message.contains("FPS: 77"))
        assertTrue(message.contains("Frame: 15.00 avg / 10.00 p95 / 20.00 max ms"))
        assertTrue(message.contains("World: 3.00 avg / 4.00 max ms"))
        assertTrue(message.contains("Static plan: 3.00 max ms (3 updates, 8 checks)"))
        assertTrue(message.contains("Dynamic plan: 1.00 avg / 1.50 max ms"))
        assertTrue(message.contains("Tiles: 7/10"))
        assertTrue(message.contains("Objects: 4/6"))
        assertTrue(message.contains("Entities: 2/3"))
        assertTrue(message.contains("Preview: 1 | Draw calls: 9"))
    }

    @Test
    fun `samples reset after each emitted interval`() {
        val logger = DebugPerformanceLogger().apply {
            enabled = true
            intervalSeconds = 0.01f
        }

        logger.record(stats(renderMs = 8.0), 0.01f)
        logger.record(stats(renderMs = 2.0), 0.01f)

        assertEquals(2, logs.size)
        assertTrue(normalizeDecimals(logs[0].second).contains("World: 8.00 avg"))
        assertTrue(normalizeDecimals(logs[1].second).contains("World: 2.00 avg"))
    }

    @Test
    fun `toggling enabled state clears partial samples`() {
        val logger = DebugPerformanceLogger().apply {
            enabled = true
            intervalSeconds = 0.02f
        }
        logger.record(stats(renderMs = 10.0), 0.01f)

        logger.enabled = false
        logger.enabled = true
        logger.record(stats(renderMs = 2.0), 0.01f)
        assertTrue(logs.isEmpty())
        logger.record(stats(renderMs = 4.0), 0.01f)

        val message = normalizeDecimals(logs.single().second)
        assertTrue(message.contains("World: 3.00 avg"))
        assertTrue(!message.contains("World: 10.00 avg"))
    }

    @Test
    fun `interval rejects non positive and non finite values`() {
        val logger = DebugPerformanceLogger()

        for (value in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> {
                logger.intervalSeconds = value
            }
        }
    }

    private fun stats(
        renderMs: Double,
        staticMs: Double = 0.0,
        staticUpdates: Int = 0,
        relationChecks: Int = 0,
        dynamicMs: Double = 0.0
    ) = RenderStats().apply {
        cpuRenderMs = renderMs
        staticPlanMs = staticMs
        staticPlanUpdates = staticUpdates
        staticPlanRelationChecks = relationChecks
        dynamicPlanMs = dynamicMs
    }

    private fun normalizeDecimals(value: String): String =
        value.replace(Regex("(\\d),(\\d)"), "$1.$2")
}
