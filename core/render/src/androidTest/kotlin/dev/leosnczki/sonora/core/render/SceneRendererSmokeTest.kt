package dev.leosnczki.sonora.core.render

import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLES30
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.ByteBuffer
import dev.leosnczki.sonora.core.domain.FluidStyle
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Local only (needs a GPU): the real pipeline covers the whole surface, at a small size and at
 * the reference tablet's size, and survives a resize.
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
    ) {
        egl.releaseSurface(surface)
        surface = egl.createOffscreenSurface(width, height)
        egl.makeCurrent(surface)
        renderer.setImages(current = image, previous = null)
        renderer.resize(width, height, density = TABLET_DENSITY)
        renderer.draw(SceneFrame.Still.copy(phase = phase, style = style))
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

    private companion object {
        /** The reference tablet's display density (340 dpi). */
        const val TABLET_DENSITY = 340f / 160f
    }
}
