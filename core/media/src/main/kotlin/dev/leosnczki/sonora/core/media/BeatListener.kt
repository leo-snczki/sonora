package dev.leosnczki.sonora.core.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.leosnczki.sonora.core.domain.BeatBands
import dev.leosnczki.sonora.core.domain.BeatPulse
import dev.leosnczki.sonora.core.domain.SilenceWatch
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Opens a tap on the device's audio output that hands over FFT captures (bytes, rate in mHz). */
fun interface AudioTap {
    /** Null, or a throw, when no tap can be opened: Beat Pulse then just stays still. */
    fun open(onFft: (ByteArray, Int) -> Unit): AutoCloseable?
}

/**
 * Beat Pulse's ears, one per process and shared by every wallpaper engine: listens to the output
 * mix (never the microphone) only while some engine is visible, Beat Pulse is on and audio access
 * is granted, and releases the visualiser otherwise. Any failure just leaves the pulse still.
 */
class BeatListener(
    private val tap: AudioTap,
    private val hasPermission: () -> Boolean,
    private val isPlaying: () -> Boolean,
    private val clock: () -> Long = ::monotonicMillis,
) {
    /** What the engines add to the fluid's scale each frame. */
    val pulse = BeatPulse()

    private val silence = SilenceWatch()
    private val _playerSilent = MutableStateFlow(false)

    /** Music is playing, yet nothing has reached the visualiser for 30 s: the Player can't be heard. */
    val playerSilent: StateFlow<Boolean> = _playerSilent.asStateFlow()

    private val visibleEngines = mutableSetOf<Any>()
    private var strength = 0f
    private var open: AutoCloseable? = null

    @Synchronized
    fun setVisible(engine: Any, visible: Boolean) {
        if (visible) visibleEngines += engine else visibleEngines -= engine
        update()
    }

    @Synchronized
    fun setStrength(strength: Float) {
        this.strength = strength
        pulse.setStrength(strength)
        update()
    }

    /** Call when audio access may have changed. */
    @Synchronized
    fun update() {
        val wanted = visibleEngines.isNotEmpty() && strength > 0f && hasPermission()
        if (wanted && open == null) {
            open = runCatching { tap.open(::onFft) }.getOrNull()
        } else if (!wanted && open != null) {
            runCatching { open?.close() }
            open = null
            pulse.onBands(FloatArray(4))
            _playerSilent.value = false
        }
    }

    private fun onFft(fft: ByteArray, samplingRateMilliHz: Int) {
        val bands = runCatching { BeatBands.from(fft, samplingRateMilliHz) }.getOrNull() ?: return
        pulse.onBands(bands)
        _playerSilent.value = silence.onBands(bands, isPlaying(), clock())
    }
}

/** The system visualiser on output mix 0, set up like the original: largest capture, fastest rate, normalised. */
private class VisualizerTap : AudioTap {
    override fun open(onFft: (ByteArray, Int) -> Unit): AutoCloseable {
        val visualizer = Visualizer(0)
        try {
            visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]
            visualizer.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            visualizer.measurementMode = Visualizer.MEASUREMENT_MODE_NONE
            val listener = object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) = Unit

                override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) = onFft(fft, samplingRate)
            }
            visualizer.setDataCaptureListener(listener, Visualizer.getMaxCaptureRate(), false, true)
            visualizer.enabled = true
        } catch (e: RuntimeException) {
            visualizer.release()
            throw e
        }
        return AutoCloseable {
            runCatching { visualizer.enabled = false }
            visualizer.release()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object BeatModule {
    @Provides
    @Singleton
    fun beatListener(@ApplicationContext context: Context, nowPlaying: NowPlayingRepository) = BeatListener(
        tap = VisualizerTap(),
        hasPermission = {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        },
        isPlaying = { nowPlaying.nowPlaying.value?.playing == true },
    )
}
