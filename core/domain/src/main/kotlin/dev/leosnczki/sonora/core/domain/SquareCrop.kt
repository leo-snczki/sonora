package dev.leosnczki.sonora.core.domain

import kotlin.math.min

/** A square in image pixels: [left], [top] and the side [size]. */
data class CropRect(val left: Float, val top: Float, val size: Float)

/**
 * The square cropper (S05) for an uploaded Backdrop: the image turned by [rotation] (quarter
 * turns) and [flipped] left to right, then a square window [zoom] times smaller than its short
 * side, centred on [centerX], [centerY] and never leaving the image. Coordinates are in the
 * turned and flipped image, [width] × [height].
 */
data class SquareCrop(
    val imageWidth: Int,
    val imageHeight: Int,
    val rotation: Int = 0,
    val flipped: Boolean = false,
    val zoom: Float = 1f,
    val centerX: Float = (if (rotation % 180 == 0) imageWidth else imageHeight) / 2f,
    val centerY: Float = (if (rotation % 180 == 0) imageHeight else imageWidth) / 2f,
) {
    val width get() = if (rotation % 180 == 0) imageWidth else imageHeight
    val height get() = if (rotation % 180 == 0) imageHeight else imageWidth

    private val size get() = min(width, height) / zoom

    fun sourceRect(): CropRect = CropRect(centerX - size / 2, centerY - size / 2, size)

    fun zoomIn(factor: Float) = withZoom(zoom * factor)

    fun zoomOut(factor: Float) = withZoom(zoom / factor)

    /** Moves the window by [dx], [dy] image pixels, as far as the image allows. */
    fun pan(dx: Float, dy: Float) = copy(centerX = centerX + dx, centerY = centerY + dy).clamped()

    /** A quarter turn; the window goes back to the middle of the turned image. */
    fun rotateRight() = turned((rotation + 90) % 360)

    fun rotateLeft() = turned((rotation + 270) % 360)

    fun flip() = copy(flipped = !flipped, centerX = width - centerX)

    private fun turned(rotation: Int) = SquareCrop(imageWidth, imageHeight, rotation, flipped, zoom)

    private fun withZoom(zoom: Float) = copy(zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)).clamped()

    private fun clamped(): SquareCrop {
        val half = size / 2
        return copy(centerX = centerX.coerceIn(half, width - half), centerY = centerY.coerceIn(half, height - half))
    }

    private companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 4f
    }
}
