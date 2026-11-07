package dev.leosnczki.sonora.core.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** How the fluid turns the image into the wallpaper. */
enum class FluidStyle {
    /** The image dissolves into a flowing fluid of its own colours. */
    Flow,

    /** The image stays recognisable, distorted by the fluid. */
    Cover,
}

/** Which way Cover shows the image; both on turns it by 180°. Flow ignores it. */
data class CoverOrientation(val flipVertical: Boolean, val mirrorHorizontal: Boolean) {
    companion object {
        val Upright = CoverOrientation(flipVertical = false, mirrorHorizontal = false)
    }
}

/** How the fluid looks and moves. Speeds are in the scene's own units; [scale] runs from calm (0) to turbulent (1). */
data class FluidSettings(val style: FluidStyle, val scale: Float, val speedMin: Float, val speedMax: Float) {
    companion object {
        /** What each style moves with while the Motion group is off; Flow's are the original's. */
        fun defaultsFor(style: FluidStyle) = when (style) {
            FluidStyle.Flow -> FluidSettings(style, scale = 0.8f, speedMin = 0.05f, speedMax = 0.5f)
            FluidStyle.Cover -> FluidSettings(style, scale = 0.5f, speedMin = 0.15f, speedMax = 0.6f)
        }

        val Default = defaultsFor(FluidStyle.Flow)
    }
}

/** What the wallpaper should draw at one moment. */
data class SceneState(
    val style: FluidStyle,
    val orientation: CoverOrientation,
    val currentPicture: Picture,
    val previousPicture: Picture,
    /** 0 shows [previousPicture], 1 shows [currentPicture]. */
    val crossfade: Float,
    val phase: Float,
    val speed: Float,
    val scale: Float,
    /** 0 means don't render. */
    val targetFps: Int,
)

/**
 * Turns Now Playing, visibility and time into the scene each frame: crossfades between Pictures
 * (Album Art, or the Backdrop when there is none to show),
 * runs the fluid fast while playing and eases it down when paused, and picks a frame rate.
 * One per wallpaper engine; pure, so it is driven by the timestamps it is given.
 */
class SceneDirector(private var fluid: FluidSettings = FluidSettings.Default) {
    private var keep = AlbumArtKeep.Default
    private var savedArt: ArtId? = null
    private var backdrop = Backdrops.Default
    private var nowPlaying: NowPlaying? = null
    /** This run's last Album Art, kept after pausing even once the Player's session is gone. */
    private var lastArt: ArtId? = null
    /** Until the first rules arrive the Picture is a guess: the first rules apply without a fade. */
    private var rulesKnown = false
    private var current: Picture = picture()
    private var previous: Picture = current
    private var fadeStartedAt = Long.MIN_VALUE / 2 // long ago: settled, and no overflow when subtracted
    private var playing = false
    private var visible = false
    private var speedAtChange = fluid.speedMin
    private var playingChangedAt = 0L
    private var phase = 0f
    private var lastFrameAt: Long? = null

    fun setVisible(visible: Boolean) {
        this.visible = visible
    }

    fun setFluid(settings: FluidSettings) {
        fluid = settings
    }

    private var orientation = CoverOrientation.Upright

    fun setOrientation(orientation: CoverOrientation) {
        this.orientation = orientation
    }

    /** The Album Art group and the chosen Backdrop: what to show when there is no music to show. */
    fun setPictureRules(keep: AlbumArtKeep, savedArt: ArtId?, backdrop: BackdropId, atMillis: Long) {
        this.keep = keep
        this.savedArt = savedArt
        this.backdrop = backdrop
        if (rulesKnown) {
            show(picture(), atMillis)
        } else {
            rulesKnown = true
            current = picture()
            previous = current
        }
    }

    private fun picture() = pictureFor(nowPlaying, keep, lastArt, savedArt, backdrop)

    fun onNowPlaying(nowPlaying: NowPlaying?, atMillis: Long) {
        this.nowPlaying = nowPlaying
        nowPlaying?.art?.let { lastArt = it }
        val nowPlayingState = nowPlaying?.playing == true
        if (nowPlayingState != playing) {
            speedAtChange = speedAt(atMillis)
            playingChangedAt = atMillis
            playing = nowPlayingState
        }
        show(picture(), atMillis)
    }

    private fun show(picture: Picture, atMillis: Long) {
        if (picture == current) return
        if (fadeProgress(atMillis) < 0.5f) {
            // The outgoing image is still mostly on screen: keep it and swap only the faint
            // incoming one, so the fade carries on without a visible jump.
            current = picture
        } else {
            previous = current
            current = picture
            fadeStartedAt = atMillis
        }
    }

    fun frame(atMillis: Long): SceneState {
        val seconds = (atMillis - (lastFrameAt ?: atMillis)).coerceAtLeast(0) / 1000f
        lastFrameAt = atMillis

        val speed = speedAt(atMillis)
        phase += speed * seconds

        val crossfade = fadeProgress(atMillis)
        val fading = crossfade < 1f
        val fps = when {
            !visible -> 0
            playing || fading -> 60
            else -> 30
        }
        return SceneState(fluid.style, orientation, current, previous, crossfade, phase, speed, fluid.scale, fps)
    }

    /** When [flowStep] was last called; null before the first drawn frame. */
    private var lastFlowStepAt: Long? = null

    /**
     * How far, as a distance in RGB, Flow's colours may move towards a new image since the last
     * call: the original's 0.01 per frame at 60 fps, made time-based. Call once per drawn frame;
     * the first call returns [FLOW_SNAP], so the first image shows at once.
     */
    fun flowStep(atMillis: Long): Float {
        val since = lastFlowStepAt
        lastFlowStepAt = atMillis
        if (since == null) return FLOW_SNAP
        return (atMillis - since).coerceAtLeast(0) / 1000f * FLOW_TRAVEL_PER_SECOND
    }

    private fun fadeProgress(atMillis: Long): Float =
        ((atMillis - fadeStartedAt).toFloat() / CROSSFADE_MILLIS).coerceIn(0f, 1f)

    /**
     * Speed moves from where it was at the last play/pause change towards its target. Flow closes
     * in on it like the original, 1% of the gap per frame at 60 fps, made time-based; Cover ramps
     * linearly.
     */
    private fun speedAt(atMillis: Long): Float {
        val target = if (playing) fluid.speedMax else fluid.speedMin
        val elapsed = (atMillis - playingChangedAt).coerceAtLeast(0) / 1000f
        if (fluid.style == FluidStyle.Flow) return target + (speedAtChange - target) * FLOW_SPEED_KEPT_PER_FRAME.pow(elapsed * 60f)
        val step = abs(fluid.speedMax - fluid.speedMin) / SPEED_RAMP_SECONDS * elapsed
        return if (speedAtChange < target) min(speedAtChange + step, target) else max(speedAtChange - step, target)
    }

    companion object {
        const val CROSSFADE_MILLIS = 1_500L

        /** A step longer than any distance between two colours (√3): lands on the image at once. */
        const val FLOW_SNAP = 2f
        private const val FLOW_TRAVEL_PER_SECOND = 0.6f
        private const val FLOW_SPEED_KEPT_PER_FRAME = 0.99f
        private const val SPEED_RAMP_SECONDS = 2f
    }
}
