package dev.leosnczki.sonora.core.domain

import kotlin.test.assertEquals
import org.junit.Test

class NowPlayingTrackerTest {
    private val tracker = NowPlayingTracker()

    private fun session(player: String, playing: Boolean, art: String = "$player-art") =
        MediaSessionSnapshot(
            id = "$player-session",
            player = player,
            playing = playing,
            track = Track(title = "$player song", artist = "Artist", album = "Album"),
            art = ArtId(art),
        )

    @Test
    fun a_single_playing_session_is_now_playing() {
        tracker.update(listOf(session("spotify", playing = true)), atMillis = 0)

        val now = tracker.nowPlaying(atMillis = 0)

        assertEquals("spotify", now?.player)
        assertEquals(true, now?.playing)
        assertEquals(ArtId("spotify-art"), now?.art)
    }

    @Test
    fun the_player_that_started_playing_most_recently_wins() {
        tracker.update(listOf(session("spotify", playing = true)), atMillis = 0)
        tracker.update(listOf(session("spotify", playing = true), session("ytmusic", playing = true)), atMillis = 5_000)

        assertEquals("ytmusic", tracker.nowPlaying(atMillis = 5_000)?.player)
    }

    @Test
    fun a_playing_session_beats_a_more_recent_paused_one() {
        tracker.update(listOf(session("spotify", playing = true), session("ytmusic", playing = true)), atMillis = 0)
        tracker.update(listOf(session("spotify", playing = true), session("ytmusic", playing = false)), atMillis = 1_000)

        assertEquals("spotify", tracker.nowPlaying(atMillis = 1_000)?.player)
    }

    @Test
    fun when_nothing_plays_the_last_player_that_played_stays_as_paused() {
        tracker.update(listOf(session("spotify", playing = true), session("ytmusic", playing = false)), atMillis = 0)
        tracker.update(listOf(session("spotify", playing = false), session("ytmusic", playing = false)), atMillis = 2_000)

        val now = tracker.nowPlaying(atMillis = 2_000)

        assertEquals("spotify", now?.player)
        assertEquals(false, now?.playing)
    }

    @Test
    fun a_player_that_never_played_is_not_now_playing() {
        tracker.update(listOf(session("spotify", playing = false)), atMillis = 0)

        assertEquals(null, tracker.nowPlaying(atMillis = 0))
    }

    @Test
    fun closing_the_now_playing_player_falls_back_to_the_last_other_one_that_played() {
        tracker.update(listOf(session("spotify", playing = true)), atMillis = 0)
        tracker.update(listOf(session("spotify", playing = false), session("ytmusic", playing = true)), atMillis = 1_000)
        tracker.update(listOf(session("spotify", playing = false)), atMillis = 2_000)

        assertEquals("spotify", tracker.nowPlaying(atMillis = 2_000)?.player)
    }

    @Test
    fun closing_every_player_leaves_nothing_playing() {
        tracker.update(listOf(session("spotify", playing = true)), atMillis = 0)
        tracker.update(emptyList(), atMillis = 1_000)

        assertEquals(null, tracker.nowPlaying(atMillis = 1_000))
    }

    @Test
    fun a_pause_shorter_than_ten_seconds_does_not_count_as_stopped() {
        tracker.update(listOf(session("spotify", playing = true)), atMillis = 0)
        tracker.update(listOf(session("spotify", playing = false)), atMillis = 1_000)

        assertEquals(false, tracker.nowPlaying(atMillis = 10_999)?.stopped)
        assertEquals(true, tracker.nowPlaying(atMillis = 11_000)?.stopped)
    }

    @Test
    fun resuming_within_the_grace_never_counts_as_stopped() {
        tracker.update(listOf(session("spotify", playing = true)), atMillis = 0)
        tracker.update(listOf(session("spotify", playing = false, art = "a")), atMillis = 1_000)
        tracker.update(listOf(session("spotify", playing = true, art = "b")), atMillis = 4_000)

        val now = tracker.nowPlaying(atMillis = 30_000)
        assertEquals(false, now?.stopped)
        assertEquals(ArtId("b"), now?.art)
    }
}
