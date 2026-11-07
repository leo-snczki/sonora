package dev.leosnczki.sonora.core.domain

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class BeatPulseTest {
    /** The rate Android reports: milliHertz. */
    private val rate = 44_100_000

    /** A 1024-sample FFT capture, Android's layout, with one bin at [magnitude] (signed bytes). */
    private fun fftWith(bin: Int, magnitude: Int): ByteArray = ByteArray(1024).apply {
        this[bin * 2] = magnitude.toByte()
    }

    @Test
    fun silence_reads_as_no_energy_in_any_band() {
        // The original read the bytes unsigned minus 128, so silence came out near 2.0.
        assertTrue(BeatBands.from(ByteArray(1024), rate).all { it == 0f })
    }

    @Test
    fun a_bass_tone_lands_in_the_bass_band_only() {
        // 100 Hz at 44.1 kHz over 1024 samples is bin 2; the bass band (60–250 Hz) spans bins 1..5.
        val bands = BeatBands.from(fftWith(bin = 2, magnitude = 64), rate)

        // Mean magnitude 64 / 5 bins = 12.8, over 128 is 0.1, squared 0.01.
        assertEquals(0.01f, bands[0], 0.0001f)
        assertEquals(listOf(0f, 0f, 0f), bands.drop(1))
    }

    @Test
    fun the_sampling_rate_is_read_in_millihertz() {
        // 1 kHz is bin 23: the 500–2000 Hz band. Read as Hz, every band would collapse onto bin 1.
        val bands = BeatBands.from(fftWith(bin = 23, magnitude = 100), rate)

        assertTrue(bands[2] > 0f)
        assertEquals(0f, bands[0])
    }

    private fun settledPulse(bands: FloatArray, strength: Float = 1f): Float {
        val pulse = BeatPulse().apply { setStrength(strength) }
        pulse.onBands(bands)
        pulse.frame(atMillis = 0)
        return pulse.frame(atMillis = 30_000)
    }

    @Test
    fun the_pulse_is_the_originals_mix_of_bass_against_the_rest() {
        // (1.5·bass − 0.1·low mids − 0.2·mids − 0.3·highs) × strength
        assertEquals(0.5f * (1.5f * 0.4f - 0.1f * 0.1f - 0.2f * 0.1f - 0.3f * 0.1f), settledPulse(floatArrayOf(0.4f, 0.1f, 0.1f, 0.1f), strength = 0.5f), 0.0001f)
    }

    @Test
    fun loud_and_quiet_beats_both_pulse_and_silence_does_not() {
        val loud = settledPulse(BeatBands.from(fftWith(bin = 2, magnitude = 120), rate))
        val quiet = settledPulse(BeatBands.from(fftWith(bin = 2, magnitude = 30), rate))

        assertTrue(loud > quiet && quiet > 0f, "loud $loud, quiet $quiet")
        assertEquals(0f, settledPulse(BeatBands.from(ByteArray(1024), rate)))
    }

    @Test
    fun with_no_strength_there_is_no_pulse() {
        assertEquals(0f, settledPulse(floatArrayOf(1f, 0f, 0f, 0f), strength = 0f))
    }

    /** The pulse a second after a beat arrives, drawn at [fps]. */
    private fun pulseOneSecondIn(fps: Int): Float {
        val pulse = BeatPulse().apply { setStrength(0.5f) }
        pulse.frame(atMillis = 0)
        pulse.onBands(floatArrayOf(1f, 0f, 0f, 0f))
        return (1..fps).map { pulse.frame(atMillis = it * 1000L / fps) }.last()
    }

    @Test
    fun the_pulse_eases_in_like_the_original_whatever_the_frame_rate() {
        // The original moves (0.06·strength + 0.04) = 7% of the gap per frame at 60 fps: after a
        // second, 1 − 0.93^60 ≈ 0.987 of the target 0.75 (1.5 × 0.5).
        assertEquals(0.740f, pulseOneSecondIn(fps = 60), 0.002f)
        assertEquals(0.740f, pulseOneSecondIn(fps = 120), 0.002f)
    }

    @Test
    fun thirty_seconds_of_music_with_silence_means_the_player_cannot_be_heard() {
        val watch = SilenceWatch()
        val silence = FloatArray(4)

        watch.onBands(silence, playing = true, atMillis = 0)
        assertFalse(watch.onBands(silence, playing = true, atMillis = 29_000))
        assertTrue(watch.onBands(silence, playing = true, atMillis = 30_000))
    }

    @Test
    fun any_sound_or_a_pause_restarts_the_silence_count() {
        val watch = SilenceWatch()
        val silence = FloatArray(4)

        watch.onBands(silence, playing = true, atMillis = 0)
        watch.onBands(floatArrayOf(0.2f, 0f, 0f, 0f), playing = true, atMillis = 20_000)
        assertFalse(watch.onBands(silence, playing = true, atMillis = 45_000))
        watch.onBands(silence, playing = false, atMillis = 46_000)
        assertFalse(watch.onBands(silence, playing = true, atMillis = 70_000))
        assertTrue(watch.onBands(silence, playing = true, atMillis = 100_000))
    }
}
