package dev.leosnczki.sonora.wallpaper

import android.graphics.Bitmap
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import dagger.hilt.android.AndroidEntryPoint
import dev.leosnczki.sonora.core.data.BackdropLibrary
import dev.leosnczki.sonora.core.data.SettingsRepository
import dev.leosnczki.sonora.core.domain.ArtId
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.Backdrops
import dev.leosnczki.sonora.core.media.BeatListener
import dev.leosnczki.sonora.core.media.LastAlbumArt
import dev.leosnczki.sonora.core.media.NowPlayingRepository
import dev.leosnczki.sonora.core.render.BackdropImage
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The live wallpaper. Every engine (home, lock, system preview) gets its own GL thread and
 * scene; they all follow the same Now Playing.
 */
@AndroidEntryPoint
class SonoraWallpaperService : WallpaperService() {
    @Inject lateinit var nowPlaying: NowPlayingRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var beats: BeatListener
    @Inject lateinit var lastAlbumArt: LastAlbumArt
    @Inject lateinit var library: BackdropLibrary

    /** Backdrops drawn or read once, shared by every engine. */
    private val backdropImages = mutableMapOf<BackdropId, Bitmap>()
    private val readingBackdrops = mutableSetOf<BackdropId>()

    /**
     * A built-in Backdrop is drawn on the spot; the user's own is read from private storage in the
     * background, so it is null until then (the scene shows the default meanwhile and tries again).
     * Only the last saved one stays in memory; one that can't be read draws the default.
     */
    private fun backdropImage(id: BackdropId): Bitmap? = synchronized(backdropImages) {
        backdropImages[id]?.let { return it }
        if (Backdrops.isBuiltin(id) || id in unreadableBackdrops) {
            val backdrop = Backdrops.resolve(id)
            return backdropImages.getOrPut(backdrop.id) { BackdropImage.create(backdrop) }
        }
        if (readingBackdrops.add(id)) {
            serviceScope.launch {
                val image = runCatching { library.image(id) }.getOrNull()
                synchronized(backdropImages) {
                    readingBackdrops -= id
                    if (image == null) {
                        unreadableBackdrops += id
                    } else {
                        backdropImages.keys.removeAll { !Backdrops.isBuiltin(it) }
                        backdropImages[id] = image
                    }
                }
            }
        }
        null
    }

    private val unreadableBackdrops = mutableSetOf<BackdropId>()

    /** Owns work that outlives one engine, such as reading the saved Album Art. */
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        // The Album Art saved by the last run, read once per process off the main thread.
        serviceScope.launch(Dispatchers.IO) { lastAlbumArt.load() }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** Album Art from the store, or the saved one read at start, which the store may have let go. */
    private fun artImage(id: ArtId): Bitmap? = nowPlaying.art(id) ?: lastAlbumArt.bitmap(id)

    override fun onCreateEngine(): Engine = SonoraEngine()

    private inner class SonoraEngine : Engine() {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val scene = NowPlayingScene(artFor = ::artImage, backdropFor = ::backdropImage, pulseAt = { beats.pulse.frame(it) })
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
                combine(settings.settings, lastAlbumArt.saved) { value, saved -> value to saved }.collect { (value, saved) ->
                    scene.setPictureRules(value.effectiveAlbumArtKeep, saved, value.backdrop)
                    loop.poke()
                }
            }
            scope.launch {
                // Keep the last Album Art in private storage only while asked to.
                combine(settings.settings, nowPlaying.nowPlaying) { value, playing -> value.effectiveAlbumArtKeep.afterReboot to playing?.art }
                    .distinctUntilChanged()
                    .collect { (keep, art) -> if (!keep) lastAlbumArt.forget() else if (art != null) lastAlbumArt.remember(art) }
            }
            scope.launch {
                settings.settings.collect {
                    scene.setFluid(it.effectiveFluid)
                    scene.setOrientation(it.coverOrientation)
                    scene.setLook(it.effectiveLook)
                    beats.setStrength(it.effectiveBeatStrength)
                    loop.poke()
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) = loop.attach(holder.surface)

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) =
            loop.resize(width, height)

        override fun onVisibilityChanged(visible: Boolean) {
            scene.setVisible(visible)
            beats.setVisible(this, visible)
            loop.setVisible(visible)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) = loop.detach()

        override fun onDestroy() {
            beats.setVisible(this, false)
            scope.cancel()
            loop.quit()
        }
    }
}
