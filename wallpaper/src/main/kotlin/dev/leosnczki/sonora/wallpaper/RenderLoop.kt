package dev.leosnczki.sonora.wallpaper

import android.graphics.Bitmap
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Choreographer
import android.view.Surface
import dev.leosnczki.sonora.core.render.EglCore
import dev.leosnczki.sonora.core.render.SceneFrame
import dev.leosnczki.sonora.core.render.SceneRenderer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The images the renderer should show: [current] fading in over [previous]. */
data class SceneImages(val current: Bitmap?, val previous: Bitmap?)

/** Where a render loop gets its frames from. Called on the render thread. */
interface WallpaperScene {
    /** New images to upload since the last call, or null when unchanged. */
    fun takeImages(nanos: Long): SceneImages?

    fun frameAt(nanos: Long): SceneFrame

    /** Frames per second wanted right now; 0 stops rendering. */
    fun targetFps(nanos: Long): Int
}

/**
 * One GL thread per wallpaper engine. Draws only while visible and attached to a surface,
 * paced by the display's vsync and throttled to the scene's target frame rate.
 */
class RenderLoop(private val scene: WallpaperScene) : Choreographer.FrameCallback {
    private val thread = HandlerThread("sonora-render").apply { start() }
    private val handler = Handler(thread.looper)
    private lateinit var choreographer: Choreographer

    private var egl: EglCore? = null
    private var surface: EGLSurface? = null
    private var renderer: SceneRenderer? = null
    private var visible = false
    private var scheduled = false
    private var lastFrameNanos = 0L

    init {
        handler.post { choreographer = Choreographer.getInstance() }
    }

    fun attach(window: Surface) = post {
        val core = egl ?: EglCore().also { egl = it }
        surface?.let { core.releaseSurface(it) }
        surface = core.createWindowSurface(window).also { core.makeCurrent(it) }
        if (renderer == null) renderer = SceneRenderer().apply { init() }
        scene.takeImages(System.nanoTime())?.let { renderer?.setImages(it.current, it.previous) }
        schedule()
    }

    fun resize(width: Int, height: Int) = post {
        renderer?.resize(width, height)
        schedule()
    }

    fun setVisible(visible: Boolean) = post {
        this.visible = visible
        if (visible) schedule()
    }

    /** Wakes the loop, e.g. after the scene changed while the frame rate was 0. */
    fun poke() = post { schedule() }

    /** Blocks until the surface is released: the system destroys it right after we return. */
    fun detach() {
        val done = CountDownLatch(1)
        handler.post {
            surface?.let { egl?.releaseSurface(it) }
            surface = null
            done.countDown()
        }
        done.await(2, TimeUnit.SECONDS)
    }

    fun quit() {
        handler.post {
            val core = egl
            if (core != null) {
                runCatching {
                    // A context needs some surface to release GL objects against.
                    val scratch = core.createOffscreenSurface(1, 1)
                    core.makeCurrent(scratch)
                    renderer?.release()
                    core.releaseSurface(scratch)
                }
                core.release()
            }
            egl = null
            renderer = null
            thread.quitSafely()
        }
    }

    private fun post(block: () -> Unit) {
        handler.post(block)
    }

    private fun schedule() {
        if (scheduled || !visible || surface == null) return
        scheduled = true
        choreographer.postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        scheduled = false
        val core = egl ?: return
        val target = surface ?: return
        val draw = renderer ?: return
        if (!visible) return

        val fps = scene.targetFps(frameTimeNanos)
        if (fps <= 0) return
        // Skip vsyncs to hold the target rate (with a little slack so 60 Hz targets don't drop to 30).
        val interval = 1_000_000_000L / fps
        if (frameTimeNanos - lastFrameNanos >= interval - interval / 4) {
            lastFrameNanos = frameTimeNanos
            try {
                scene.takeImages(frameTimeNanos)?.let { draw.setImages(it.current, it.previous) }
                draw.draw(scene.frameAt(frameTimeNanos))
                core.swapBuffers(target)
            } catch (e: RuntimeException) {
                Log.w(TAG, "frame failed", e)
            }
        }
        schedule()
    }

    private companion object {
        const val TAG = "SonoraRender"
    }
}
