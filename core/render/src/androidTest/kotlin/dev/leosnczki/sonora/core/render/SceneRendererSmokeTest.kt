package dev.leosnczki.sonora.core.render

import android.opengl.GLES30
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals
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

    private fun renderAt(width: Int, height: Int, phase: Float = 0f) {
        egl.releaseSurface(surface)
        surface = egl.createOffscreenSurface(width, height)
        egl.makeCurrent(surface)
        renderer.setImages(current = TestImage.create(128), previous = null)
        renderer.resize(width, height)
        renderer.draw(SceneFrame.Still.copy(phase = phase))
    }

    /** Pixels that are pure black, i.e. that the final pass never wrote. */
    private fun blackPixels(width: Int, height: Int): Int {
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        var black = 0
        for (i in 0 until width * height) {
            val r = buffer.get(i * 4).toInt() and 0xFF
            val g = buffer.get(i * 4 + 1).toInt() and 0xFF
            val b = buffer.get(i * 4 + 2).toInt() and 0xFF
            if (r + g + b < 6) black++
        }
        return black
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
}
