package dev.leosnczki.sonora.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import dev.leosnczki.sonora.core.domain.BuiltinBackdrop

/** Draws a built-in Backdrop: its colours as a diagonal gradient, top left to bottom right. */
object BackdropImage {
    fun create(backdrop: BuiltinBackdrop, size: Int = 256): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), backdrop.colors.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        Canvas(bitmap).drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return bitmap
    }
}
