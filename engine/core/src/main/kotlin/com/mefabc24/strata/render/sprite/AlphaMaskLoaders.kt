package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap

internal fun alphaMaskFromClasspath(path: String): AlphaMask {
    val pixmap = Pixmap(Gdx.files.classpath(path))

    return try {
        AlphaMask.fromPixmap(pixmap)
    } finally {
        pixmap.dispose()
    }
}

internal fun alphaMasksFromSpriteSheetClasspath(
    path: String,
    frameWidth: Int,
    frameHeight: Int,
    frameCount: Int?
): List<AlphaMask?> {
    val pixmap = Pixmap(Gdx.files.classpath(path))

    return try {
        SpriteSheetGrid.cells(
            sheetWidth = pixmap.width,
            sheetHeight = pixmap.height,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            frameCount = frameCount
        ).map { cell ->
            AlphaMask.fromPixmap(
                pixmap = pixmap,
                x = cell.x,
                y = cell.y,
                width = cell.width,
                height = cell.height
            )
        }
    } finally {
        pixmap.dispose()
    }
}
