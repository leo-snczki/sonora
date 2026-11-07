package dev.leosnczki.sonora.feature.backdrops

import android.graphics.Bitmap
import android.graphics.Color
import dev.leosnczki.sonora.core.domain.SquareCrop
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CropBitmapTest {
    /** 400 × 200: red on the left half, blue on the right. */
    private val image = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888).apply {
        for (y in 0 until 200) for (x in 0 until 400) setPixel(x, y, if (x < 200) Color.RED else Color.BLUE)
    }

    private fun Bitmap.at(x: Int, y: Int) = getPixel(x, y)

    @Test
    fun the_centred_square_comes_out_at_the_backdrop_size() {
        val out = cropped(image, SquareCrop(400, 200), size = 100)

        assertEquals(100 to 100, out.width to out.height)
        assertEquals(Color.RED, out.at(10, 50))
        assertEquals(Color.BLUE, out.at(90, 50))
    }

    @Test
    fun panned_fully_left_it_is_all_red_and_flipped_the_sides_swap() {
        val left = SquareCrop(400, 200).pan(dx = -1_000f, dy = 0f)
        assertEquals(Color.RED, cropped(image, left, size = 100).at(90, 50))

        val flipped = SquareCrop(400, 200).flip()
        val out = cropped(image, flipped, size = 100)
        assertEquals(Color.BLUE, out.at(10, 50))
        assertEquals(Color.RED, out.at(90, 50))
    }

    @Test
    fun a_quarter_turn_right_puts_the_left_side_on_top() {
        val out = cropped(image, SquareCrop(400, 200).rotateRight().pan(dx = 0f, dy = -1_000f), size = 100)

        assertEquals(Color.RED, out.at(50, 50))
    }
}
