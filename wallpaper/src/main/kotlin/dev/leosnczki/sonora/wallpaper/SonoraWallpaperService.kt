package dev.leosnczki.sonora.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import dagger.hilt.android.AndroidEntryPoint
import dev.leosnczki.sonora.core.data.SettingsRepository
import dev.leosnczki.sonora.core.media.NowPlayingRepository
import dev.leosnczki.sonora.core.render.TestImage
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The live wallpaper. Every engine (home, lock, system preview) gets its own GL thread and
 * scene; they all follow the same Now Playing.
 */
@AndroidEntryPoint
class SonoraWallpaperService : WallpaperService() {
    @Inject lateinit var nowPlaying: NowPlayingRepository
    @Inject lateinit var settings: SettingsRepository

    /** Shown until #11 brings Backdrops: drawn once, shared by every engine. */
    private val placeholder by lazy { TestImage.create(256) }

    override fun onCreateEngine(): Engine = SonoraEngine()

    private inner class SonoraEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val scene = NowPlayingScene(artFor = nowPlaying::art, fallbackImage = { placeholder })
        private val loop = RenderLoop(scene, resources.displayMetrics.density)

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            nowPlaying.start()
            scope.launch {
                nowPlaying.nowPlaying.collect {
                    scene.onNowPlaying(it)
                    loop.poke()
                }
            }
            scope.launch {
                settings.settings.collect {
                    scene.setFluid(it.effectiveFluid)
                    loop.poke()
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) = loop.attach(holder.surface)

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) =
            loop.resize(width, height)

        override fun onVisibilityChanged(visible: Boolean) {
            scene.setVisible(visible)
            loop.setVisible(visible)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) = loop.detach()

        override fun onDestroy() {
            scope.cancel()
            loop.quit()
        }
    }
}
