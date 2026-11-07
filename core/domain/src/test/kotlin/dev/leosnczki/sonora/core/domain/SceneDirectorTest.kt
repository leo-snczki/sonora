package dev.leosnczki.sonora.core.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SceneDirectorTest {
    private val director = SceneDirector().apply { setVisible(true) }

    private fun playing(art: String, playing: Boolean = true) =
        NowPlaying(player = "spotify", track = null, art = ArtId(art), playing = playing, stopped = false)

    @Test
    fun new_album_art_crossfades_in_over_one_and_a_half_seconds() {
        director.onNowPlaying(playing("a"), atMillis = 0)
        director.frame(atMillis = 0)
        director.onNowPlaying(playing("b"), atMillis = 10_000)

        val start = director.frame(atMillis = 10_000)
        assertEquals(ArtId("b"), start.currentArt)
        assertEquals(ArtId("a"), start.previousArt)
        assertEquals(0f, start.crossfade, 0.001f)
        assertEquals(0.5f, director.frame(atMillis = 10_750).crossfade, 0.001f)
        assertEquals(1f, director.frame(atMillis = 11_500).crossfade, 0.001f)
        assertEquals(1f, director.frame(atMillis = 20_000).crossfade, 0.001f)
    }

    @Test
    fun the_same_album_art_posted_again_does_not_restart_the_crossfade() {
        director.onNowPlaying(playing("a"), atMillis = 0)
        director.frame(atMillis = 2_000)
        director.onNowPlaying(playing("a"), atMillis = 2_000)

        val frame = director.frame(atMillis = 2_100)
        assertEquals(1f, frame.crossfade, 0.001f)
        assertEquals(ArtId("a"), frame.currentArt)
    }

    @Test
    fun motion_runs_at_maximum_speed_while_playing_and_eases_to_minimum_when_paused() {
        val fluid = FluidSettings.Default
        director.onNowPlaying(playing("a"), atMillis = 0)
        assertEquals(fluid.speedMax, director.frame(atMillis = 5_000).speed, 0.001f)

        director.onNowPlaying(playing("a", playing = false), atMillis = 5_000)
        val easing = director.frame(atMillis = 5_500).speed
        assertTrue(easing < fluid.speedMax && easing > fluid.speedMin, "eases, no jump: $easing")
        assertEquals(fluid.speedMin, director.frame(atMillis = 10_000).speed, 0.001f)
    }

    @Test
    fun the_fluid_keeps_moving_forward_at_the_current_speed() {
        director.onNowPlaying(playing("a"), atMillis = 0)
        val first = director.frame(atMillis = 5_000).phase
        val second = director.frame(atMillis = 6_000).phase

        assertTrue(second > first)
    }

    @Test
    fun frame_rate_is_zero_when_invisible_full_while_playing_and_half_while_paused() {
        director.onNowPlaying(playing("a"), atMillis = 0)
        assertEquals(60, director.frame(atMillis = 5_000).targetFps)

        director.onNowPlaying(playing("a", playing = false), atMillis = 5_000)
        assertEquals(30, director.frame(atMillis = 10_000).targetFps)

        director.setVisible(false)
        assertEquals(0, director.frame(atMillis = 11_000).targetFps)
    }

    @Test
    fun nothing_playing_shows_no_album_art() {
        director.onNowPlaying(null, atMillis = 0)

        assertEquals(null, director.frame(atMillis = 0).currentArt)
    }

    @Test
    fun before_any_music_the_scene_is_settled_and_idles_at_half_rate() {
        val frame = director.frame(atMillis = 1_000_000)

        assertEquals(1f, frame.crossfade, 0.001f)
        assertEquals(30, frame.targetFps)
    }

    @Test
    fun skipping_again_early_in_a_crossfade_keeps_the_image_that_is_mostly_on_screen() {
        director.onNowPlaying(playing("a"), atMillis = 0)
        director.frame(atMillis = 5_000)
        director.onNowPlaying(playing("b"), atMillis = 5_000)
        director.frame(atMillis = 5_300) // b is 20% in, a 80%
        director.onNowPlaying(playing("c"), atMillis = 5_300)

        val frame = director.frame(atMillis = 5_300)
        assertEquals(ArtId("a"), frame.previousArt)
        assertEquals(ArtId("c"), frame.currentArt)
        assertEquals(0.2f, frame.crossfade, 0.01f)
    }

    @Test
    fun skipping_again_late_in_a_crossfade_fades_from_the_image_that_is_mostly_on_screen() {
        director.onNowPlaying(playing("a"), atMillis = 0)
        director.frame(atMillis = 5_000)
        director.onNowPlaying(playing("b"), atMillis = 5_000)
        director.onNowPlaying(playing("c"), atMillis = 6_200) // b is 80% in

        val frame = director.frame(atMillis = 6_200)
        assertEquals(ArtId("b"), frame.previousArt)
        assertEquals(ArtId("c"), frame.currentArt)
        assertEquals(0f, frame.crossfade, 0.01f)
    }
}
