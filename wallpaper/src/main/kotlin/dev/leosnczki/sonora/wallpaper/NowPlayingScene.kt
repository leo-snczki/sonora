package dev.leosnczki.sonora.wallpaper

import android.graphics.Bitmap
import dev.leosnczki.sonora.core.domain.AlbumArtKeep
import dev.leosnczki.sonora.core.domain.ArtId
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.Backdrops
import dev.leosnczki.sonora.core.domain.CoverOrientation
import dev.leosnczki.sonora.core.domain.FluidSettings
import dev.leosnczki.sonora.core.domain.LookSettings
import dev.leosnczki.sonora.core.domain.NowPlaying
import dev.leosnczki.sonora.core.domain.Picture
import dev.leosnczki.sonora.core.domain.SceneDirector
import dev.leosnczki.sonora.core.media.monotonicMillis
import dev.leosnczki.sonora.core.render.SceneFrame

/**
 * Adapts the pure [SceneDirector] to the render loop: Now Playing and settings arrive on the
 * main thread, frames are pulled on the render thread, so access is synchronised.
 * [artFor] turns an [ArtId] into a bitmap (null while it isn't ready: the Backdrop shows meanwhile);
 * [backdropFor] draws a Backdrop (null while a saved one is being read: the default shows meanwhile).
 * [pulseAt] gives Beat Pulse at a time in milliseconds.
 */
class NowPlayingScene(
    private val artFor: (ArtId) -> Bitmap?,
    private val backdropFor: (BackdropId) -> Bitmap?,
    private val pulseAt: (Long) -> Float = { 0f },
) : WallpaperScene {
    private val director = SceneDirector()
    private var uploaded: Pair<Picture, Picture>? = null

    @Synchronized
    fun onNowPlaying(nowPlaying: NowPlaying?) = director.onNowPlaying(nowPlaying, monotonicMillis())

    @Synchronized
    fun setVisible(visible: Boolean) = director.setVisible(visible)

    @Synchronized
    fun setFluid(settings: FluidSettings) = director.setFluid(settings)

    /** What to show when there is no music to show: see [SceneDirector.setPictureRules]. */
    @Synchronized
    fun setPictureRules(keep: AlbumArtKeep, savedArt: ArtId?, backdrop: BackdropId) {
        this.backdrop = backdrop
        director.setPictureRules(keep, savedArt, backdrop, monotonicMillis())
    }

    private var backdrop = Backdrops.Default

    @Synchronized
    fun setOrientation(orientation: CoverOrientation) = director.setOrientation(orientation)

    /** How the fluid is finished on screen; the director has no say in it. */
    private var look = LookSettings.Default

    @Synchronized
    fun setLook(look: LookSettings) {
        this.look = look
    }

    /** When art was last missing; null when nothing is waiting to be retried. */
    private var lastMissAt: Long? = null

    /**
     * Render thread only. Bitmaps are resolved outside the lock: scaling a large cover must not
     * hold up the main thread. Art that is not available yet is retried about once a second.
     */
    override fun takeImages(nanos: Long): SceneImages? {
        val (wanted, fallback) = synchronized(this) {
            director.frame(nanos / 1_000_000).let { it.currentPicture to it.previousPicture } to backdrop
        }
        if (wanted == uploaded) return null
        lastMissAt?.let { if (nanos - it < RETRY_NANOS) return null }
        val current = imageOf(wanted.first)
        val previous = imageOf(wanted.second)
        val complete = current != null && previous != null
        lastMissAt = if (complete) null else nanos
        if (complete) uploaded = wanted
        val stopgap = backdropFor(fallback) ?: backdropFor(Backdrops.Default)
        return SceneImages(current ?: stopgap, previous ?: stopgap)
    }

    private fun imageOf(picture: Picture): Bitmap? = when (picture) {
        is Picture.Art -> artFor(picture.id)
        is Picture.Backdrop -> backdropFor(picture.id)
    }

    /** Called once per drawn frame, which is what [SceneDirector.flowStep] counts on. */
    @Synchronized
    override fun frameAt(nanos: Long): SceneFrame {
        val millis = nanos / 1_000_000
        val state = director.frame(millis)
        return SceneFrame(
            style = state.style,
            effect = look.effect,
            orientation = state.orientation,
            phase = state.phase,
            crossfade = state.crossfade,
            scale = state.scale,
            brightness = look.brightness,
            flowStep = director.flowStep(millis),
            pulse = pulseAt(millis),
        )
    }

    @Synchronized
    override fun targetFps(nanos: Long): Int = director.frame(nanos / 1_000_000).targetFps

    private companion object {
        const val RETRY_NANOS = 1_000_000_000L
    }
}
