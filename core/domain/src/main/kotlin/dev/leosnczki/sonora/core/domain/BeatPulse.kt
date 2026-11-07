package dev.leosnczki.sonora.core.domain

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The four bands the original listens to, from one FFT capture of the system visualiser
 * (Android's layout: bytes 0 and 1 are the DC and Nyquist reals, then real and imaginary pairs
 * from bin 1). Each band is (mean magnitude / 128)², as in the original, with its two bugs fixed:
 * the sampling rate is in milliHertz, and the bytes are signed.
 */
object BeatBands {
    /** Bass, low mids, mids, highs, in Hz. */
    private val ranges = listOf(60f to 250f, 250f to 500f, 500f to 2000f, 2000f to 16000f)

    fun from(fft: ByteArray, samplingRateMilliHz: Int): FloatArray {
        val bins = fft.size / 2
        val magnitudes = FloatArray(bins) { k -> if (k == 0) 0f else hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat()) }
        val hz = samplingRateMilliHz / 1000f
        fun bin(frequency: Float) = (frequency * fft.size / hz).toInt()
        return FloatArray(ranges.size) { band ->
            val (low, high) = ranges[band]
            val first = max(1, min(bin(low), bins - 1))
            val last = max(first, min(bin(high), bins - 1))
            val mean = (first..last).sumOf { magnitudes[it].toDouble() }.toFloat() / (last - first + 1) / 128f
            mean * mean
        }
    }
}

/**
 * Beat Pulse, as in the original: bass pushes the fluid's scale out, the other bands pull it in,
 * eased towards that target at (6·strength + 4)% of the gap per frame at 60 fps, made time-based.
 * Bands arrive on the visualiser's thread and frames on the render thread, so access is synchronised.
 */
class BeatPulse {
    /** 0..1; 0 means no pulse at all. */
    @get:Synchronized
    var strength = 0f
        private set
    private var target = 0f
    private var value = 0f
    private var lastFrameAt: Long? = null

    @Synchronized
    fun setStrength(strength: Float) {
        this.strength = strength.coerceIn(0f, 1f)
    }

    @Synchronized
    fun onBands(bands: FloatArray) {
        target = 1.5f * bands[0] - 0.1f * bands[1] - 0.2f * bands[2] - 0.3f * bands[3]
    }

    /**
     * The pulse to add to the fluid's scale now. Every visible engine calls it once per drawn
     * frame; the easing is exponential in time, so interleaved calls still add up to the same curve.
     */
    @Synchronized
    fun frame(atMillis: Long): Float {
        val seconds = lastFrameAt?.let { (atMillis - it).coerceAtLeast(0) / 1000f } ?: 0f
        lastFrameAt = atMillis
        val kept = (1f - (0.06f * strength + 0.04f)).pow(seconds * 60f)
        value = target * strength + (value - target * strength) * kept
        return value
    }
}

/**
 * Notices a Player Sonora cannot hear: music playing, yet nothing reaching the visualiser for
 * [holdMillis]. A pause or any sound starts the count again.
 */
class SilenceWatch(private val holdMillis: Long = 30_000) {
    private var silentSince: Long? = null

    /** Forget any silence so far, e.g. when nothing has been listening for a while. */
    fun reset() {
        silentSince = null
    }

    /** Whether the Player has been silent for long enough to say so. */
    fun onBands(bands: FloatArray, playing: Boolean, atMillis: Long): Boolean {
        val silent = playing && bands.all { it < SILENCE }
        if (!silent) {
            silentSince = null
            return false
        }
        val since = silentSince ?: atMillis.also { silentSince = it }
        return atMillis - since >= holdMillis
    }

    private companion object {
        /** Below this, a band is silence: about one unit of magnitude across the band. */
        const val SILENCE = 1e-4f
    }
}
