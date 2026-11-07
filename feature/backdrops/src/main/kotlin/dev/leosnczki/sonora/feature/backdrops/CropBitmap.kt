package dev.leosnczki.sonora.feature.backdrops

import android.graphics.Bitmap
import android.graphics.Matrix
import dev.leosnczki.sonora.core.domain.SquareCrop
import kotlin.math.roundToInt

/** [image] turned and flipped as [crop] says, so its pixels match the crop's coordinates. */
fun turned(image: Bitmap, crop: SquareCrop): Bitmap {
    if (crop.rotation == 0 && !crop.flipped) return image
    val matrix = Matrix().apply {
        postRotate(crop.rotation.toFloat())
        if (crop.flipped) postScale(-1f, 1f)
    }
    return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
}

/** The square [crop] of [image], [size] pixels a side: what an uploaded Backdrop keeps. */
fun cropped(image: Bitmap, crop: SquareCrop, size: Int = BACKDROP_SIZE): Bitmap {
    val source = turned(image, crop)
    val rect = crop.sourceRect()
    val side = rect.size.roundToInt().coerceIn(1, minOf(source.width, source.height))
    val left = rect.left.roundToInt().coerceIn(0, source.width - side)
    val top = rect.top.roundToInt().coerceIn(0, source.height - side)
    val square = Bitmap.createBitmap(source, left, top, side, side)
    return Bitmap.createScaledBitmap(square, size, size, true)
}

/** As large as the renderer ever uses: it squashes Backdrops to 512 px. */
const val BACKDROP_SIZE = 512
