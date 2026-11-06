package dev.leosnczki.sonora.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import dev.leosnczki.sonora.core.render.SceneFrame
import dev.leosnczki.sonora.core.render.TestImage

/**
 * The live wallpaper. Every engine (home, lock, system preview) gets its own GL thread;
 * they all draw the same scene.
 */
class SonoraWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SonoraEngine()

    private inner class SonoraEngine : Engine() {
        private val loop = RenderLoop(TestScene())

        override fun onSurfaceCreated(holder: SurfaceHolder) = loop.attach(holder.surface)

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) =
            loop.resize(width, height)

        override fun onVisibilityChanged(visible: Boolean) = loop.setVisible(visible)

        override fun onSurfaceDestroyed(holder: SurfaceHolder) = loop.detach()

        override fun onDestroy() = loop.quit()
    }
}

/** Until Now Playing is wired in: the procedural test image, drifting slowly. */
private class TestScene : WallpaperScene {
    private var delivered = false
    private val start = System.nanoTime()

    override fun takeImages(): SceneImages? {
        if (delivered) return null
        delivered = true
        val image = TestImage.create(256)
        return SceneImages(image, image)
    }

    override fun frameAt(nanos: Long) =
        SceneFrame.Still.copy(phase = (nanos - start) / 1_000_000_000f * 0.08f)

    override fun targetFps(nanos: Long) = 30
}
