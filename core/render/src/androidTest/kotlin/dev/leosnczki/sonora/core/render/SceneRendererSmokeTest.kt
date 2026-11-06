package dev.leosnczki.sonora.core.render

import android.opengl.GLES30
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Local only (needs a GPU): the real pipeline draws something visible and survives a resize. */
@RunWith(AndroidJUnit4::class)
class SceneRendererSmokeTest {
    private val egl = EglCore()
    private var surface = egl.createOffscreenSurface(256, 256)

    @After
    fun tearDown() = egl.release()

    private fun brightPixels(width: Int, height: Int): Int {
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        var bright = 0
        for (i in 0 until width * height) {
            val r = buffer.get(i * 4).toInt() and 0xFF
            val g = buffer.get(i * 4 + 1).toInt() and 0xFF
            val b = buffer.get(i * 4 + 2).toInt() and 0xFF
            if (r + g + b > 30) bright++
        }
        return bright
    }

    @Test
    fun renders_a_non_black_frame_from_a_test_image_and_survives_a_resize() {
        egl.makeCurrent(surface)
        val renderer = SceneRenderer()
        renderer.init()
        renderer.setImages(current = TestImage.create(128), previous = null)
        renderer.resize(256, 256)
        renderer.draw(SceneFrame.Still)
        assertTrue(brightPixels(256, 256) > 256 * 256 / 2, "most pixels should show the image")

        egl.releaseSurface(surface)
        surface = egl.createOffscreenSurface(120, 300)
        egl.makeCurrent(surface)
        renderer.resize(120, 300)
        renderer.draw(SceneFrame.Still.copy(phase = 3f))
        assertTrue(brightPixels(120, 300) > 120 * 300 / 2, "still drawing after resize")
        renderer.release()
    }
}
