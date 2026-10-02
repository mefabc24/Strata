package com.mefabc24.strata.debug.ui

import java.util.Locale

internal data class DebugSearchCategory(
    val title: String,
    val settingNames: Set<String>,
    var expanded: Boolean = false
)

internal data class DebugSearchResult(
    val matchingIndices: Set<Int>,
    val hasMatches: Boolean
)

/** Search state independent from Scene2D so filtering never rebuilds controls. */
internal class DebugSettingsSearch(private val categories: List<DebugSearchCategory>) {
    private var savedExpansion: List<Boolean>? = null

    var query: String = ""
        private set

    fun update(query: String): DebugSearchResult {
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) {
            savedExpansion?.forEachIndexed { index, expanded -> categories[index].expanded = expanded }
            savedExpansion = null
            this.query = ""
            return DebugSearchResult(categories.indices.toSet(), categories.isNotEmpty())
        }
        if (savedExpansion == null) savedExpansion = categories.map { it.expanded }
        this.query = query
        val matches = categories.indices.filterTo(linkedSetOf()) { index ->
            val category = categories[index]
            category.title.contains(normalized, ignoreCase = true) ||
                    category.settingNames.any { it.contains(normalized, ignoreCase = true) }
        }
        matches.forEach { categories[it].expanded = true }
        return DebugSearchResult(matches, matches.isNotEmpty())
    }

    fun setAllExpanded(expanded: Boolean) {
        categories.forEach { it.expanded = expanded }
        if (query.isNotBlank()) savedExpansion = categories.map { expanded }
    }
}
