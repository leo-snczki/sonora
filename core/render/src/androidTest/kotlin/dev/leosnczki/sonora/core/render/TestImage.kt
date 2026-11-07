package dev.leosnczki.sonora.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/** A colourful procedural image for tests and for the wallpaper before any Album Art exists. */
object TestImage {
    private val blobs = listOf(
        floatArrayOf(0.25f, 0.30f, 0.55f) to 0xFFE0576F.toInt(),
        floatArrayOf(0.75f, 0.25f, 0.50f) to 0xFFF2A65A.toInt(),
        floatArrayOf(0.30f, 0.80f, 0.55f) to 0xFF3F7CAC.toInt(),
        floatArrayOf(0.80f, 0.75f, 0.50f) to 0xFF6B4E9B.toInt(),
    )

    fun create(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0xFF2B2140.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        blobs.forEach { (circle, color) ->
            val (x, y, r) = circle.map { it * size }
            paint.shader = RadialGradient(x, y, r, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, r, paint)
        }
        return bitmap
    }
}
