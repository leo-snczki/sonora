package dev.leosnczki.sonora.wallpaper

import android.graphics.Bitmap
import dev.leosnczki.sonora.core.domain.ArtId
import dev.leosnczki.sonora.core.domain.FluidSettings
import dev.leosnczki.sonora.core.domain.NowPlaying
import dev.leosnczki.sonora.core.domain.SceneDirector
import dev.leosnczki.sonora.core.media.monotonicMillis
import dev.leosnczki.sonora.core.render.SceneFrame

/**
 * Adapts the pure [SceneDirector] to the render loop: Now Playing and settings arrive on the
 * main thread, frames are pulled on the render thread, so access is synchronised.
 * [artFor] turns an [ArtId] into a bitmap; null art draws [fallbackImage] until #11 brings Backdrops.
 */
class NowPlayingScene(
    private val artFor: (ArtId) -> Bitmap?,
    private val fallbackImage: () -> Bitmap,
) : WallpaperScene {
    private val director = SceneDirector()
    private var uploaded: Pair<ArtId?, ArtId?>? = null

    @Synchronized
    fun onNowPlaying(nowPlaying: NowPlaying?) = director.onNowPlaying(nowPlaying, monotonicMillis())

    @Synchronized
    fun setVisible(visible: Boolean) = director.setVisible(visible)

    @Synchronized
    fun setFluid(settings: FluidSettings) = director.setFluid(settings)

    @Synchronized
    override fun takeImages(nanos: Long): SceneImages? {
        val state = director.frame(nanos / 1_000_000)
        val wanted = state.currentArt to state.previousArt
        if (wanted == uploaded) return null
        uploaded = wanted
        return SceneImages(image(state.currentArt), image(state.previousArt))
    }

    private fun image(art: ArtId?): Bitmap = art?.let(artFor) ?: fallbackImage()

    @Synchronized
    override fun frameAt(nanos: Long): SceneFrame {
        val state = director.frame(nanos / 1_000_000)
        return SceneFrame(phase = state.phase, crossfade = state.crossfade, scale = state.scale, brightness = 0f)
    }

    @Synchronized
    override fun targetFps(nanos: Long): Int = director.frame(nanos / 1_000_000).targetFps
}
