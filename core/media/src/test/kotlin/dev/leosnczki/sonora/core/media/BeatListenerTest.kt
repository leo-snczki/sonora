package dev.leosnczki.sonora.core.media

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class BeatListenerTest {
    /** A visualiser that records whether it is open and lets the test feed it captures. */
    private class FakeTap(var fails: Boolean = false) : AudioTap {
        var onFft: ((ByteArray, Int) -> Unit)? = null
        var opened = 0

        override fun open(onFft: (ByteArray, Int) -> Unit): AutoCloseable? {
            if (fails) throw IllegalStateException("no visualiser")
            opened++
            this.onFft = onFft
            return AutoCloseable { this.onFft = null }
        }

        val isOpen get() = onFft != null
    }

    private val tap = FakeTap()
    private var permission = true
    private var playing = true
    private var now = 0L
    private val listener = BeatListener(tap, hasPermission = { permission }, isPlaying = { playing }, clock = { now })
    private val home = Any()
    private val lock = Any()

    @Test
    fun listens_only_while_an_engine_is_visible_and_beat_pulse_is_on() {
        listener.setStrength(0.5f)
        assertFalse(tap.isOpen)

        listener.setVisible(home, true)
        listener.setVisible(lock, true)
        assertTrue(tap.isOpen)
        assertEquals(1, tap.opened, "one visualiser shared by every engine")

        listener.setVisible(home, false)
        assertTrue(tap.isOpen, "the lock screen still shows")
        listener.setVisible(lock, false)
        assertFalse(tap.isOpen, "released when no engine is visible")
    }

    @Test
    fun turning_beat_pulse_off_releases_the_visualiser() {
        listener.setVisible(home, true)
        listener.setStrength(0.5f)
        assertTrue(tap.isOpen)

        listener.setStrength(0f)
        assertFalse(tap.isOpen)
    }

    @Test
    fun without_audio_access_it_never_listens() {
        permission = false
        listener.setStrength(0.5f)
        listener.setVisible(home, true)

        assertFalse(tap.isOpen)
    }

    @Test
    fun a_visualiser_that_fails_just_means_no_pulse() {
        tap.fails = true
        listener.setStrength(0.5f)
        listener.setVisible(home, true)

        assertNull(tap.onFft)
        assertEquals(0f, listener.pulse.frame(atMillis = 1_000))
    }

    @Test
    fun a_silent_player_is_reported_after_thirty_seconds_of_music() {
        listener.setStrength(0.5f)
        listener.setVisible(home, true)

        tap.onFft!!(ByteArray(1024), 44_100_000)
        now = 30_000
        tap.onFft!!(ByteArray(1024), 44_100_000)

        assertTrue(listener.playerSilent.value)
        listener.setVisible(home, false)
        assertFalse(listener.playerSilent.value, "no longer listening, so no longer sure")
    }

    @Test
    fun beats_reach_the_pulse() {
        listener.setStrength(1f)
        listener.setVisible(home, true)
        listener.pulse.frame(atMillis = 0)

        tap.onFft!!(ByteArray(1024).apply { this[4] = 100 }, 44_100_000)

        assertTrue(listener.pulse.frame(atMillis = 5_000) > 0f)
    }
}
