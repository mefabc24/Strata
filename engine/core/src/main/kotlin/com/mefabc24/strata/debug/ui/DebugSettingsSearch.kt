package com.mefabc24.strata.debug.ui

import java.util.Locale

internal data class DebugSearchCategory(
    val title: String,
    val settingNames: Set<String>,
    var expanded: Boolean = false,
    val section: DebugPanelSection = DebugPanelSection.VISUALS
)

internal data class DebugSearchResult(
    val matchingIndices: Set<Int>,
    val hasMatches: Boolean,
    val matchingSections: Set<DebugPanelSection>
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
            return result(categories.indices.toSet())
        }
        if (savedExpansion == null) savedExpansion = categories.map { it.expanded }
        this.query = query
        val matches = categories.indices.filterTo(linkedSetOf()) { index ->
            val category = categories[index]
            category.title.contains(normalized, ignoreCase = true) ||
                    category.settingNames.any { it.contains(normalized, ignoreCase = true) }
        }
        matches.forEach { categories[it].expanded = true }
        return result(matches)
    }

    fun setAllExpanded(expanded: Boolean) {
        setExpanded(categories.indices.toSet(), expanded)
    }

    fun setExpanded(indices: Set<Int>, expanded: Boolean) {
        indices.forEach { categories[it].expanded = expanded }
        if (query.isNotBlank()) {
            val restored = savedExpansion?.toMutableList() ?: categories.map { it.expanded }.toMutableList()
            indices.forEach { restored[it] = expanded }
            savedExpansion = restored
        }
    }

    private fun result(indices: Set<Int>) = DebugSearchResult(
        matchingIndices = indices,
        hasMatches = indices.isNotEmpty(),
        matchingSections = indices.mapTo(linkedSetOf()) { categories[it].section }
    )
}
