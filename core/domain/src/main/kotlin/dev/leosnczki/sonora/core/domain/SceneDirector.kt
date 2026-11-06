package dev.leosnczki.sonora.core.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** How the fluid moves. Speeds are in the scene's own units; [scale] runs from calm (0) to turbulent (1). */
data class FluidSettings(val scale: Float, val speedMin: Float, val speedMax: Float) {
    companion object {
        val Default = FluidSettings(scale = 0.5f, speedMin = 0.15f, speedMax = 0.6f)
    }
}

/** What the wallpaper should draw at one moment. */
data class SceneState(
    val currentArt: ArtId?,
    val previousArt: ArtId?,
    /** 0 shows [previousArt], 1 shows [currentArt]. */
    val crossfade: Float,
    val phase: Float,
    val speed: Float,
    val scale: Float,
    /** 0 means don't render. */
    val targetFps: Int,
)

/**
 * Turns Now Playing, visibility and time into the scene each frame: crossfades between Album Art,
 * runs the fluid fast while playing and eases it down when paused, and picks a frame rate.
 * One per wallpaper engine; pure, so it is driven by the timestamps it is given.
 */
class SceneDirector(private var fluid: FluidSettings = FluidSettings.Default) {
    private var current: ArtId? = null
    private var previous: ArtId? = null
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

    fun onNowPlaying(nowPlaying: NowPlaying?, atMillis: Long) {
        val nowPlayingState = nowPlaying?.playing == true
        if (nowPlayingState != playing) {
            speedAtChange = speedAt(atMillis)
            playingChangedAt = atMillis
            playing = nowPlayingState
        }
        val art = nowPlaying?.art
        if (art != current) {
            previous = current
            current = art
            fadeStartedAt = atMillis
        }
    }

    fun frame(atMillis: Long): SceneState {
        val seconds = (atMillis - (lastFrameAt ?: atMillis)).coerceAtLeast(0) / 1000f
        lastFrameAt = atMillis

        val speed = speedAt(atMillis)
        phase += speed * seconds

        val crossfade = ((atMillis - fadeStartedAt).toFloat() / CROSSFADE_MILLIS).coerceIn(0f, 1f)
        val fading = crossfade < 1f
        val fps = when {
            !visible -> 0
            playing || fading -> 60
            else -> 30
        }
        return SceneState(current, previous, crossfade, phase, speed, fluid.scale, fps)
    }

    /** Speed ramps linearly from where it was at the last play/pause change towards its target. */
    private fun speedAt(atMillis: Long): Float {
        val target = if (playing) fluid.speedMax else fluid.speedMin
        val elapsed = (atMillis - playingChangedAt).coerceAtLeast(0) / 1000f
        val step = abs(fluid.speedMax - fluid.speedMin) / SPEED_RAMP_SECONDS * elapsed
        return if (speedAtChange < target) min(speedAtChange + step, target) else max(speedAtChange - step, target)
    }

    companion object {
        const val CROSSFADE_MILLIS = 1_500L
        private const val SPEED_RAMP_SECONDS = 2f
    }
}
