package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.mefabc24.strata.ui.StrataPanelStyle
import com.mefabc24.strata.ui.StrataSelectableButton
import com.mefabc24.strata.ui.StrataSelectionGroup
import com.mefabc24.strata.ui.StrataSeparatorStyle
import java.util.Locale
import kotlin.math.min

internal data class DebugToolSelectionItem<T>(
    val value: T,
    val displayName: String,
    val texture: TextureRegion,
    val secondaryText: String? = null,
    val trailingText: String? = null
)

internal fun <T> filterDebugToolSelectionItems(
    items: List<DebugToolSelectionItem<T>>,
    query: String
): List<DebugToolSelectionItem<T>> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    if (normalized.isEmpty()) return items
    return items.filter { it.displayName.lowercase(Locale.ROOT).contains(normalized) }
}

internal class DebugToolSelectionList<T>(
    items: List<DebugToolSelectionItem<T>>,
    private val selectionGroup: StrataSelectionGroup<T>,
    skin: Skin,
    searchHint: String,
    showSearch: Boolean = true,
    private val onLayoutChanged: () -> Unit = {}
) : Table(skin) {
    private val allItems = items.toList()
    private val rows = Table(skin)
    private val scroll = ScrollPane(rows, skin)
    private val scrollCell = add(scroll)
    private val visibleButtons = mutableListOf<StrataSelectableButton<T>>()
    private val separator = skin.get(
        "debug-separator",
        StrataSeparatorStyle::class.java
    ).drawable

    init {
        top()
        if (showSearch) {
            val searchRow = Table(skin).apply {
                background = skin.get(
                    "debug-setting-row",
                    StrataPanelStyle::class.java
                ).background
                pad(5f, 6f, 5f, 6f)
            }
            val search = TextField("", skin).apply {
                messageText = searchHint
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent, actor: Actor) {
                        showItems(filterDebugToolSelectionItems(allItems, text))
                    }
                })
            }
            searchRow.add(search).growX().height(28f)
            add(searchRow).growX().height(38f)
            row()
        }

        scroll.setScrollingDisabled(true, false)
        scroll.setFadeScrollBars(false)
        scroll.setOverscroll(false, false)
        scrollCell.growX().fillX().minHeight(0f)
        showItems(allItems)
    }

    private fun showItems(items: List<DebugToolSelectionItem<T>>) {
        visibleButtons.forEach(StrataSelectableButton<T>::detach)
        visibleButtons.clear()
        rows.clearChildren()
        rows.top()

        if (items.isEmpty()) {
            rows.add(Label("No matching entries", skin, "debug-secondary").apply {
                setAlignment(Align.center)
            }).growX().height(42f)
        } else {
            items.forEachIndexed { index, item ->
                val button = selectionButton(item)
                visibleButtons += button
                rows.add(button).growX().height(56f)
                rows.row()
                if (index != items.lastIndex) {
                    rows.add(Image(separator)).growX().height(1f)
                    rows.row()
                }
            }
        }

        val contentHeight = if (items.isEmpty()) 42f else items.size * 56f + items.lastIndex
        scrollCell.height(min(contentHeight, MAX_LIST_HEIGHT))
        rows.invalidateHierarchy()
        scroll.invalidateHierarchy()
        invalidateHierarchy()
        onLayoutChanged()
    }

    private fun selectionButton(
        item: DebugToolSelectionItem<T>
    ): StrataSelectableButton<T> {
        val button = StrataSelectableButton(
            "",
            item.value,
            selectionGroup,
            skin,
            "debug-tool-selection-row"
        )
        button.clearChildren()
        button.pad(5f, 7f, 5f, 7f)

        button.add(Image(TextureRegionDrawable(item.texture)).apply {
            setScaling(Scaling.fit)
        }).size(44f).left()

        val labels = Table(skin).apply {
            val name = Label(item.displayName, skin).apply {
                setEllipsis(true)
                setAlignment(Align.left)
            }
            add(name).growX().left().minWidth(0f)
            item.secondaryText?.let { secondary ->
                row()
                add(Label(secondary, skin, "debug-secondary").apply {
                    setEllipsis(true)
                    setAlignment(Align.left)
                }).growX().left().minWidth(0f)
            }
        }
        button.add(labels).growX().minWidth(0f).padLeft(8f).left()
        item.trailingText?.let { trailing ->
            button.add(Label(trailing, skin, "debug-secondary")).padLeft(6f).right()
        }
        return button
    }

    private companion object {
        const val MAX_LIST_HEIGHT = 228f
    }
}
