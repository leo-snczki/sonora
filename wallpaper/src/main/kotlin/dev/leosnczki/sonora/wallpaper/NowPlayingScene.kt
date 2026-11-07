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

    /** When art was last missing; null when nothing is waiting to be retried. */
    private var lastMissAt: Long? = null

    /**
     * Render thread only. Bitmaps are resolved outside the lock: scaling a large cover must not
     * hold up the main thread. Art that is not available yet is retried about once a second.
     */
    override fun takeImages(nanos: Long): SceneImages? {
        val wanted = synchronized(this) { director.frame(nanos / 1_000_000).let { it.currentArt to it.previousArt } }
        if (wanted == uploaded) return null
        lastMissAt?.let { if (nanos - it < RETRY_NANOS) return null }
        val current = wanted.first?.let(artFor)
        val previous = wanted.second?.let(artFor)
        val complete = (wanted.first == null || current != null) && (wanted.second == null || previous != null)
        lastMissAt = if (complete) null else nanos
        if (complete) uploaded = wanted
        return SceneImages(current ?: fallbackImage(), previous ?: fallbackImage())
    }

    @Synchronized
    override fun frameAt(nanos: Long): SceneFrame {
        val state = director.frame(nanos / 1_000_000)
        return SceneFrame(style = state.style, phase = state.phase, crossfade = state.crossfade, scale = state.scale, brightness = 0f)
    }

    @Synchronized
    override fun targetFps(nanos: Long): Int = director.frame(nanos / 1_000_000).targetFps

    private companion object {
        const val RETRY_NANOS = 1_000_000_000L
    }
}
