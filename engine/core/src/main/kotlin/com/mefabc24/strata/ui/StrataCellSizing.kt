package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Value

/**
 * Keeps the actor at its live preferred width instead of claiming extra room.
 * Existing minimum and maximum constraints remain in effect.
 */
fun <A : Actor> Cell<A>.hugX(): Cell<A> = apply {
    prefWidth(Value.prefWidth)
    expand(0, expandY)
    fill(0f, fillY)
}

/**
 * Keeps the actor at its live preferred height instead of claiming extra room.
 * Existing minimum and maximum constraints remain in effect.
 */
fun <A : Actor> Cell<A>.hugY(): Cell<A> = apply {
    prefHeight(Value.prefHeight)
    expand(expandX, 0)
    fill(fillX, 0f)
}

/** Keeps the actor at its live preferred size on both axes. */
fun <A : Actor> Cell<A>.hug(): Cell<A> = apply {
    prefWidth(Value.prefWidth)
    prefHeight(Value.prefHeight)
    expand(0, 0)
    fill(0f, 0f)
}

/** Uses the preferred width as a baseline and consumes extra horizontal room. */
fun <A : Actor> Cell<A>.fillAvailableX(): Cell<A> = apply {
    prefWidth(Value.prefWidth)
    expand(1, expandY)
    fill(1f, fillY)
}

/** Uses the preferred height as a baseline and consumes extra vertical room. */
fun <A : Actor> Cell<A>.fillAvailableY(): Cell<A> = apply {
    prefHeight(Value.prefHeight)
    expand(expandX, 1)
    fill(fillX, 1f)
}

/** Uses the preferred size as a baseline and consumes extra room on both axes. */
fun <A : Actor> Cell<A>.fillAvailable(): Cell<A> = apply {
    prefWidth(Value.prefWidth)
    prefHeight(Value.prefHeight)
    expand(1, 1)
    fill(1f, 1f)
}

/** Fixes the actor width while leaving vertical sizing unchanged. */
fun <A : Actor> Cell<A>.fixedWidth(width: Float): Cell<A> = apply {
    require(width.isFinite() && width >= 0f) {
        "Fixed width must be finite and non-negative."
    }

    width(width)
    expand(0, expandY)
    fill(0f, fillY)
}

/** Fixes the actor height while leaving horizontal sizing unchanged. */
fun <A : Actor> Cell<A>.fixedHeight(height: Float): Cell<A> = apply {
    require(height.isFinite() && height >= 0f) {
        "Fixed height must be finite and non-negative."
    }

    height(height)
    expand(expandX, 0)
    fill(fillX, 0f)
}

/** Fixes the actor size on both axes. */
fun <A : Actor> Cell<A>.fixedSize(
    width: Float,
    height: Float
): Cell<A> = apply {
    fixedWidth(width)
    fixedHeight(height)
}
