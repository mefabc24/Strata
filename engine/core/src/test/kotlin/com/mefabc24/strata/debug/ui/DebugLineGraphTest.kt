package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.mefabc24.strata.debug.DebugPerformanceMetric
import com.mefabc24.strata.debug.DebugPerformanceUnit
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.testing.defaultValue
import com.mefabc24.strata.testing.proxy
import com.mefabc24.strata.ui.StrataUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugLineGraphTest {
    @Test
    fun `axis labels distinguish small timing ranges`() {
        assertEquals("0.10", performanceGraphAxisLabel(0.1, 0.1))
        assertEquals("0.05", performanceGraphAxisLabel(0.05, 0.1))
        assertEquals("1.0", performanceGraphAxisLabel(1.0, 1.0))
        assertEquals("0.5", performanceGraphAxisLabel(0.5, 1.0))
        assertEquals("200", performanceGraphAxisLabel(200.0, 200.0))
    }

    @Test
    fun `axis labels follow the metric unit and keep large counts compact`() {
        val count = DebugPerformanceUnit.COUNT
        assertEquals("2", performanceGraphAxisLabel(2.0, 2.0, count))
        assertEquals("1", performanceGraphAxisLabel(1.0, 2.0, count))
        assertEquals("2.5", performanceGraphAxisLabel(2.5, 5.0, count))
        assertEquals("500", performanceGraphAxisLabel(500.0, 1_000.0, count))
        assertEquals("20k", performanceGraphAxisLabel(20_000.0, 20_000.0, count))
        assertEquals("25k", performanceGraphAxisLabel(25_000.0, 50_000.0, count))
        assertEquals("2M", performanceGraphAxisLabel(2_000_000.0, 2_000_000.0, count))
        assertEquals("2.5M", performanceGraphAxisLabel(2_500_000.0, 5_000_000.0, count))
        assertEquals("60.0", performanceGraphAxisLabel(60.0, 10.0, DebugPerformanceUnit.FRAMES_PER_SECOND))
        assertEquals("100", performanceGraphAxisLabel(100.0, 100.0, DebugPerformanceUnit.FRAMES_PER_SECOND))
    }

    @Test
    fun `frame rate and count graphs draw within the same batch lifecycle`() {
        TestGdxEnvironment.install()
        val skin = DebugPanelSkin.create()
        val ui = StrataUi(skin, DebugPanelSkin.theme())
        val style = skin.get("debug-secondary", Label.LabelStyle::class.java)
        var lineDraws = 0
        val calls = mutableListOf<String>()
        val batch = proxy(Batch::class.java) { _, method, arguments ->
            calls += method.name
            if (method.name == "draw" && arguments!!.size == 10) lineDraws++
            defaultValue(method.returnType)
        }
        try {
            val graph = DebugLineGraph(TextureRegion(skin.get("debug-white", Texture::class.java)),
                skin.get("title", Label.LabelStyle::class.java).fontColor, style)
            ui.stage.addActor(graph)
            graph.setBounds(20f, 20f, 440f, 180f)
            for (metric in listOf(DebugPerformanceMetric.FRAMES_PER_SECOND, DebugPerformanceMetric.ENTITIES_DRAWN)) {
                lineDraws = 0
                val data = DebugPerformanceGraphData().apply {
                    update(3, 380, metric.unit.minimumGraphRange) { 40.0 + it * 10 }
                }
                graph.show(data, metric)
                graph.draw(batch, 1f)
                assertEquals(2, lineDraws, metric.name)
                assertTrue("begin" !in calls && "end" !in calls)
                assertNull(ScissorStack.peekScissors())
            }
        } finally {
            ui.dispose()
            skin.dispose()
        }
    }

    @Test
    fun `line drawing clips and preserves the stage batch lifecycle and shared font`() {
        TestGdxEnvironment.install()
        val skin = DebugPanelSkin.create()
        val ui = StrataUi(skin, DebugPanelSkin.theme())
        val style = skin.get("debug-secondary", Label.LabelStyle::class.java)
        val fontColor = Color(style.font.color)
        val batchColor = Color(0.2f, 0.4f, 0.6f, 0.8f)
        val initialPacked = batchColor.toFloatBits()
        val calls = mutableListOf<String>()
        var lineDraws = 0
        val batch = proxy(Batch::class.java) { _, method, arguments ->
            calls += method.name
            when (method.name) {
                "getPackedColor" -> batchColor.toFloatBits()
                "getColor" -> batchColor
                "setPackedColor" -> { Color.abgr8888ToColor(batchColor, arguments!![0] as Float); null }
                "setColor" -> {
                    if (arguments!!.size == 1) batchColor.set(arguments[0] as Color)
                    else batchColor.set(arguments[0] as Float, arguments[1] as Float,
                        arguments[2] as Float, arguments[3] as Float)
                    null
                }
                "draw" -> {
                    if (arguments!!.size == 10) lineDraws++
                    null
                }
                else -> defaultValue(method.returnType)
            }
        }
        try {
            val graph = DebugLineGraph(TextureRegion(skin.get("debug-white", Texture::class.java)),
                skin.get("title", Label.LabelStyle::class.java).fontColor, style)
            ui.stage.addActor(graph)
            graph.setBounds(20f, 20f, 440f, 180f)
            val data = DebugPerformanceGraphData().apply { update(3, 380) { 20.0 + it * 10 } }
            graph.show(data, DebugPerformanceMetric.FRAME_TIME)
            graph.draw(batch, 0.5f)
            assertEquals(2, lineDraws)
            assertTrue(calls.count { it == "flush" } >= 2)
            assertTrue("begin" !in calls && "end" !in calls)
            assertNull(ScissorStack.peekScissors())
            assertEquals(initialPacked, batchColor.toFloatBits())
            assertEquals(fontColor, style.font.color)
            data.update(0, 380) { error("Empty series") }
            graph.show(data, DebugPerformanceMetric.RENDER_TIME)
            graph.draw(batch, 1f)
            assertEquals(2, lineDraws)
            assertNull(ScissorStack.peekScissors())
        } finally {
            ui.dispose()
            skin.dispose()
        }
    }
}
