package dev.leosnczki.sonora.core.render

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES30
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.leosnczki.sonora.core.domain.CoverOrientation
import dev.leosnczki.sonora.core.domain.FluidStyle
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Local only (needs a GPU): both styles' pipelines cover the whole surface, at a small size and
 * at the reference tablet's size, and survive a resize; Flow shows the image's colours and walks
 * them to a new image.
 */
@RunWith(AndroidJUnit4::class)
class SceneRendererSmokeTest {
    private val egl = EglCore()
    private var surface = egl.createOffscreenSurface(256, 256)
    private val renderer by lazy { SceneRenderer().apply { init() } }

    @After
    fun tearDown() {
        renderer.release()
        egl.release()
    }

    private fun renderAt(
        width: Int,
        height: Int,
        phase: Float = 0f,
        style: FluidStyle = FluidStyle.Cover,
        image: Bitmap = TestImage.create(128),
        orientation: CoverOrientation = CoverOrientation.Upright,
    ) {
        egl.releaseSurface(surface)
        surface = egl.createOffscreenSurface(width, height)
        egl.makeCurrent(surface)
        renderer.setImages(current = image, previous = null)
        renderer.resize(width, height, density = TABLET_DENSITY)
        renderer.draw(SceneFrame.Still.copy(phase = phase, style = style, orientation = orientation))
    }

    private fun readPixels(width: Int, height: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        return buffer
    }

    /** Pixels that are pure black, i.e. that the final pass never wrote. */
    private fun blackPixels(width: Int, height: Int): Int {
        val buffer = readPixels(width, height)
        var black = 0
        for (i in 0 until width * height) {
            val r = buffer.get(i * 4).toInt() and 0xFF
            val g = buffer.get(i * 4 + 1).toInt() and 0xFF
            val b = buffer.get(i * 4 + 2).toInt() and 0xFF
            if (r + g + b < 6) black++
        }
        return black
    }

    /** The largest difference of any channel of any pixel from [color]. */
    private fun maxDistanceFrom(color: Int, width: Int, height: Int): Int {
        val buffer = readPixels(width, height)
        var worst = 0
        for (i in 0 until width * height) {
            worst = maxOf(
                worst,
                abs((buffer.get(i * 4).toInt() and 0xFF) - Color.red(color)),
                abs((buffer.get(i * 4 + 1).toInt() and 0xFF) - Color.green(color)),
                abs((buffer.get(i * 4 + 2).toInt() and 0xFF) - Color.blue(color)),
            )
        }
        return worst
    }

    @Test
    fun covers_every_pixel_of_a_small_square_surface() {
        renderAt(256, 256)
        assertEquals(0, blackPixels(256, 256))
    }

    @Test
    fun covers_every_pixel_of_the_reference_tablet_screen() {
        renderAt(2944, 1840)
        assertEquals(0, blackPixels(2944, 1840))
    }

    @Test
    fun still_covers_everything_after_a_resize_to_portrait() {
        renderAt(256, 256)
        renderAt(1840, 2944, phase = 3f)
        assertEquals(0, blackPixels(1840, 2944))
    }

    @Test
    fun flow_covers_every_pixel_of_the_reference_tablet_screen() {
        renderAt(2944, 1840, phase = 7f, style = FluidStyle.Flow)
        assertEquals(0, blackPixels(2944, 1840))
    }

    @Test
    fun flow_still_covers_everything_after_a_resize_to_portrait() {
        renderAt(256, 256, style = FluidStyle.Flow)
        renderAt(1840, 2944, phase = 3f, style = FluidStyle.Flow)
        assertEquals(0, blackPixels(1840, 2944))
    }

    @Test
    fun flow_of_a_single_colour_album_art_is_that_colour() {
        val terracotta = Color.rgb(200, 90, 40)
        val solid = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(terracotta) }

        renderAt(640, 400, phase = 5f, style = FluidStyle.Flow, image = solid)

        assertTrue(maxDistanceFrom(terracotta, 640, 400) <= 2, "only the dither may move it")
    }

    @Test
    fun on_a_new_album_art_flow_walks_its_colours_towards_it_by_the_given_step() {
        val solid = { color: Int -> Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(color) } }
        renderAt(640, 400, style = FluidStyle.Flow, image = solid(Color.RED))

        renderer.setImages(current = solid(Color.BLUE), previous = null)
        renderer.draw(SceneFrame.Still.copy(style = FluidStyle.Flow, flowStep = 0.1f))

        // 0.1 along the straight line from red (1, 0, 0) to blue (0, 0, 1), whose length is √2.
        assertTrue(maxDistanceFrom(Color.rgb(237, 0, 18), 640, 400) <= 3, "one step from red towards blue")
    }

    /** An Android bitmap (row 0 is the top) red on one half and blue on the other. */
    private fun halves(redOnTop: Boolean = false, redOnLeft: Boolean = false) = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
        for (y in 0 until 64) for (x in 0 until 64) {
            val red = if (redOnTop) y < 32 else if (redOnLeft) x < 32 else false
            setPixel(x, y, if (red) Color.RED else Color.BLUE)
        }
    }

    /** Average red minus average blue over a band of the screen, as seen (row 0 at the top). */
    private fun redness(width: Int, height: Int, rows: IntRange = 0 until height, columns: IntRange = 0 until width): Float {
        val buffer = readPixels(width, height)
        var sum = 0L
        for (row in rows) for (x in columns) {
            val i = ((height - 1 - row) * width + x) * 4 // glReadPixels starts at the bottom
            sum += (buffer.get(i).toInt() and 0xFF) - (buffer.get(i + 2).toInt() and 0xFF)
        }
        return sum.toFloat() / (rows.count() * columns.count())
    }

    @Test
    fun cover_shows_the_top_of_the_album_art_at_the_top_of_the_screen() {
        renderAt(256, 256, style = FluidStyle.Cover, image = halves(redOnTop = true))

        assertTrue(redness(256, 256, rows = 0 until 26) > 100, "red on top")
        assertTrue(redness(256, 256, rows = 230 until 256) < -100, "blue at the bottom")
    }

    @Test
    fun flip_vertical_turns_cover_upside_down() {
        renderAt(256, 256, image = halves(redOnTop = true), orientation = CoverOrientation(flipVertical = true, mirrorHorizontal = false))

        assertTrue(redness(256, 256, rows = 0 until 26) < -100, "blue on top")
        assertTrue(redness(256, 256, rows = 230 until 256) > 100, "red at the bottom")
    }

    @Test
    fun mirror_horizontal_swaps_cover_left_and_right() {
        val redLeftBlueRight = halves(redOnLeft = true)
        renderAt(256, 256, image = redLeftBlueRight)
        assertTrue(redness(256, 256, columns = 0 until 26) > 100, "upright: red on the left")

        renderAt(256, 256, image = redLeftBlueRight, orientation = CoverOrientation(flipVertical = false, mirrorHorizontal = true))
        assertTrue(redness(256, 256, columns = 0 until 26) < -100, "mirrored: blue on the left")
        assertTrue(redness(256, 256, columns = 230 until 256) > 100, "mirrored: red on the right")
    }

    private companion object {
        /** The reference tablet's display density (340 dpi). */
        const val TABLET_DENSITY = 340f / 160f
    }
}
