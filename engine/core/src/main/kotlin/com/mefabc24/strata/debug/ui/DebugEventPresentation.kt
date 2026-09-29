package com.mefabc24.strata.debug.ui

import com.mefabc24.strata.debug.DebugEventRecord

internal data class DebugEventPresentation(
    val sequence: Long,
    val eventType: String,
    val fields: List<DebugDiagnosticRow>
)

internal fun presentDebugEvent(record: DebugEventRecord): DebugEventPresentation {
    val parsed = parseNamedValue(record.value)
    val fields = if (parsed != null && parsed.name.substringAfterLast('.') == record.eventType) {
        parsed.fields.map { (name, value) ->
            DebugDiagnosticRow(name.toDisplayLabel(), readableValue(value))
        }.ifEmpty { listOf(DebugDiagnosticRow("Value", record.value.toDisplayValue())) }
    } else {
        listOf(DebugDiagnosticRow("Value", record.value.toDisplayValue()))
    }
    return DebugEventPresentation(record.sequence, record.eventType, fields)
}

private data class NamedValue(
    val name: String,
    val fields: List<Pair<String, String>>
)

private fun parseNamedValue(value: String): NamedValue? {
    val open = value.indexOf('(')
    if (open <= 0 || !value.endsWith(')')) return null
    val name = value.substring(0, open).trim()
    if (name.isEmpty()) return null
    val content = value.substring(open + 1, value.lastIndex)
    if (content.isBlank()) return NamedValue(name, emptyList())
    val fields = splitTopLevel(content).mapNotNull { part ->
        val separator = topLevelEquals(part)
        if (separator <= 0) null else {
            part.substring(0, separator).trim() to part.substring(separator + 1).trim()
        }
    }
    return NamedValue(name, fields).takeIf { fields.isNotEmpty() }
}

private fun readableValue(value: String): String {
    val nested = parseNamedValue(value)
    if (nested != null) {
        val map = nested.fields.toMap()
        if (map.keys == setOf("x", "y")) {
            return "(${map.getValue("x")}, ${map.getValue("y")})"
        }
        return nested.fields.joinToString(", ") { (key, fieldValue) ->
            "${key.toDisplayLabel()}: ${readableValue(fieldValue)}"
        }.toDisplayValue()
    }
    return value.toDisplayValue()
}

private fun splitTopLevel(value: String): List<String> {
    val result = mutableListOf<String>()
    var start = 0
    var depth = 0
    var quoted = false
    var escaped = false
    value.forEachIndexed { index, character ->
        when {
            escaped -> escaped = false
            character == '\\' && quoted -> escaped = true
            character == '"' -> quoted = !quoted
            !quoted && character in "([{<" -> depth++
            !quoted && character in ")]}>" -> depth--
            !quoted && depth == 0 && character == ',' -> {
                result += value.substring(start, index).trim()
                start = index + 1
            }
        }
    }
    result += value.substring(start).trim()
    return result
}

private fun topLevelEquals(value: String): Int {
    var depth = 0
    var quoted = false
    value.forEachIndexed { index, character ->
        when (character) {
            '"' -> quoted = !quoted
            '(', '[', '{', '<' -> if (!quoted) depth++
            ')', ']', '}', '>' -> if (!quoted) depth--
            '=' -> if (!quoted && depth == 0) return index
        }
    }
    return -1
}

private fun String.toDisplayLabel(): String =
    replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar(Char::titlecase)

private fun String.toDisplayValue(): String =
    if (length <= MAX_DISPLAY_LENGTH) this else take(MAX_DISPLAY_LENGTH - 1) + "…"

private const val MAX_DISPLAY_LENGTH = 500
