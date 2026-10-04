package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.mefabc24.strata.ui.StrataPanelStyle

internal data class DebugToolPreview(
    val key: Any,
    val displayName: String,
    val texture: TextureRegion,
    val details: List<Pair<String, String>>
)

internal class DebugToolPreviewCard(
    skin: Skin
) : Table(skin) {
    private val image = Image().apply { setScaling(Scaling.fit) }
    private val information = Table(skin)
    private var displayedKey: Any? = null

    init {
        background = skin.get(
            "debug-tool-preview",
            StrataPanelStyle::class.java
        ).background
        pad(8f)
        add(image).size(94f).left().top()
        add(information).grow().minWidth(0f).padLeft(10f).top()
    }

    fun show(preview: DebugToolPreview) {
        if (displayedKey === preview.key) return
        displayedKey = preview.key
        image.drawable = TextureRegionDrawable(preview.texture)
        information.clearChildren()
        information.top().left()

        information.add(Label(preview.displayName, skin).apply {
            setEllipsis(true)
            setAlignment(Align.left)
        }).colspan(2).growX().minWidth(0f).left().height(24f)

        preview.details.forEach { (label, value) ->
            information.row()
            information.add(Label(label, skin, "debug-secondary"))
                .growX().minWidth(0f).left().height(18f)
            information.add(Label(value, skin).apply {
                setEllipsis(true)
                setAlignment(Align.right)
            }).minWidth(0f).right().height(18f)
        }
        invalidateHierarchy()
    }
}
