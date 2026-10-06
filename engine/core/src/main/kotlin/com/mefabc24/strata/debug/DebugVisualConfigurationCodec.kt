package com.mefabc24.strata.debug

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter

/**
 * Converts visual debug snapshots to and from versioned JSON.
 */
internal object DebugVisualConfigurationCodec {

    fun encode(configuration: DebugVisualConfiguration): String {
        val root = JsonValue(JsonValue.ValueType.`object`)
        val values = JsonValue(JsonValue.ValueType.`object`)

        configuration.values.forEach { (key, value) ->
            values.addChild(key, JsonValue(encodeValue(value)))
        }

        root.addChild(
            "version",
            JsonValue(configuration.version.toLong())
        )

        root.addChild("values", values)

        return root.toJson(JsonWriter.OutputType.json)
    }

    fun decode(json: String): DebugVisualConfiguration {
        val root = JsonReader().parse(json)

        val version = root.getInt("version")
        require(version in 1..DebugVisualConfigurationBindings.VERSION) {
            "Unsupported debug configuration version: $version"
        }

        val stored = requireNotNull(root.get("values")) {
            "Missing visual configuration values."
        }

        val defaults = DebugSettings()
            .captureVisualConfiguration()
            .values

        val values = mutableMapOf<String, Any?>()

        var entry = stored.child

        while (entry != null) {
            if (entry.name in defaults) {
                values[entry.name] = decodeValue(
                    entry.asString(),
                    defaults[entry.name]
                )
            } else if (version == 1 && entry.name in LEGACY_FEATURE_GATES) {
                values[entry.name] = decodeValue(entry.asString(), false)
            } else if (version < 7 && entry.name == DebugVisualConfigurationBindings.LEGACY_HISTORY_METRIC) {
                values[entry.name] = decodeValue(entry.asString(), DebugPerformanceMetric.FRAME_TIME)
            }

            entry = entry.next
        }

        return DebugVisualConfiguration(version, values)
    }

    private fun encodeValue(value: Any?): String = when (value) {
        null -> "null:"
        is Boolean -> "boolean:$value"
        is Int -> "int:$value"
        is Float -> "float:$value"
        is Color -> {
            "color:${value.r},${value.g},${value.b},${value.a}"
        }
        is Enum<*> -> "enum:${value.name}"
        is Set<*> -> "set:${encodeStrings(value, "sets")}"
        is List<*> -> "list:${encodeStrings(value, "lists")}"

        else -> error(
            "Unsupported debug setting type: ${value::class.simpleName}"
        )
    }

    private fun decodeValue(encoded: String, default: Any?): Any? {
        val type = encoded.substringBefore(':')
        val value = encoded.substringAfter(':')

        return when (type) {
            "null" -> null
            "boolean" -> value.toBooleanStrict()
            "int" -> value.toInt()
            "float" -> value.toFloat()

            "color" -> {
                val channels = value.split(',').map(String::toFloat)

                require(channels.size == 4) {
                    "Invalid debug color."
                }

                Color(
                    channels[0],
                    channels[1],
                    channels[2],
                    channels[3]
                )
            }

            "enum" -> {
                val prototype = requireNotNull(default as? Enum<*>) {
                    "Missing enum type information."
                }

                prototype.javaClass.enumConstants
                    .first { (it as Enum<*>).name == value }
            }

            "set" -> decodeStrings(value).toMutableSet()
            "list" -> decodeStrings(value)

            else -> error("Unsupported debug value type: $type")
        }
    }

    private fun encodeStrings(values: Collection<*>, kind: String): String {
        val array = JsonValue(JsonValue.ValueType.array)

        values.forEach { element ->
            require(element is String) {
                "Debug configuration $kind must contain strings."
            }

            array.addChild(JsonValue(element))
        }

        return array.toJson(JsonWriter.OutputType.json)
    }

    private fun decodeStrings(json: String): List<String> {
        val result = mutableListOf<String>()

        var item = JsonReader().parse(json).child
        while (item != null) {
            result += item.asString()
            item = item.next
        }

        return result
    }

    private val LEGACY_FEATURE_GATES = setOf(
        "worldInfo.enabled",
        "objects.enabled",
        "entities.enabled",
        "picking.enabled",
        "render.enabled",
        "culling.enabled",
        "camera.enabled"
    )
}
