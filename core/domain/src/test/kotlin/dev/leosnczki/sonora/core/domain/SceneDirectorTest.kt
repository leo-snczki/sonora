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
        assertEquals(Picture.Art(ArtId("b")), start.currentPicture)
        assertEquals(Picture.Art(ArtId("a")), start.previousPicture)
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
        assertEquals(Picture.Art(ArtId("a")), frame.currentPicture)
    }

    @Test
    fun cover_runs_at_maximum_speed_while_playing_and_eases_to_minimum_when_paused() {
        val fluid = FluidSettings.defaultsFor(FluidStyle.Cover)
        director.setFluid(fluid)
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
    fun nothing_playing_shows_the_backdrop() {
        director.onNowPlaying(null, atMillis = 0)

        assertEquals(Picture.Backdrop(Backdrops.Default), director.frame(atMillis = 0).currentPicture)
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
        assertEquals(Picture.Art(ArtId("a")), frame.previousPicture)
        assertEquals(Picture.Art(ArtId("c")), frame.currentPicture)
        assertEquals(0.2f, frame.crossfade, 0.01f)
    }

    @Test
    fun skipping_again_late_in_a_crossfade_fades_from_the_image_that_is_mostly_on_screen() {
        director.onNowPlaying(playing("a"), atMillis = 0)
        director.frame(atMillis = 5_000)
        director.onNowPlaying(playing("b"), atMillis = 5_000)
        director.onNowPlaying(playing("c"), atMillis = 6_200) // b is 80% in

        val frame = director.frame(atMillis = 6_200)
        assertEquals(Picture.Art(ArtId("b")), frame.previousPicture)
        assertEquals(Picture.Art(ArtId("c")), frame.currentPicture)
        assertEquals(0f, frame.crossfade, 0.01f)
    }

    @Test
    fun the_scene_is_drawn_in_the_chosen_fluid_style() {
        director.setFluid(FluidSettings.defaultsFor(FluidStyle.Cover))

        assertEquals(FluidStyle.Cover, director.frame(atMillis = 0).style)
    }

    @Test
    fun the_scene_is_drawn_in_the_chosen_cover_orientation() {
        val mirrored = CoverOrientation(flipVertical = false, mirrorHorizontal = true)
        director.setOrientation(mirrored)

        assertEquals(mirrored, director.frame(atMillis = 0).orientation)
    }

    /** Total distance Flow's colours may travel over one second, drawn at [fps]. */
    private fun flowTravelOverOneSecond(fps: Int): Float {
        val director = SceneDirector()
        director.flowStep(atMillis = 0)
        return (1..fps).sumOf { director.flowStep(atMillis = it * 1000L / fps).toDouble() }.toFloat()
    }

    @Test
    fun flow_colours_travel_0_6_per_second_whatever_the_frame_rate() {
        assertEquals(0.6f, flowTravelOverOneSecond(fps = 60), 0.001f)
        assertEquals(0.6f, flowTravelOverOneSecond(fps = 120), 0.001f)
    }

    @Test
    fun the_first_flow_frame_snaps_to_the_image() {
        assertTrue(SceneDirector().flowStep(atMillis = 5_000) >= SceneDirector.FLOW_SNAP)
    }

    /** Flow's speed one second after the music starts, sampled at [fps]. */
    private fun flowSpeedOneSecondAfterPlay(fps: Int): Float {
        val director = SceneDirector(FluidSettings.defaultsFor(FluidStyle.Flow)).apply { setVisible(true) }
        director.onNowPlaying(playing("a"), atMillis = 0)
        return (0..fps).map { director.frame(atMillis = it * 1000L / fps).speed }.last()
    }

    @Test
    fun flow_speed_closes_in_on_its_target_like_the_original_whatever_the_frame_rate() {
        // The original moves 1% of the way each frame at 60 fps: after a second, 0.99^60 ≈ 0.547
        // of the gap from 0.05 to 0.5 is left.
        assertEquals(0.254f, flowSpeedOneSecondAfterPlay(fps = 60), 0.002f)
        assertEquals(0.254f, flowSpeedOneSecondAfterPlay(fps = 120), 0.002f)
    }

    @Test
    fun once_the_music_stops_the_backdrop_crossfades_in() {
        director.onNowPlaying(playing("a"), atMillis = 0)
        director.onNowPlaying(NowPlaying("spotify", null, ArtId("a"), playing = false, stopped = true), atMillis = 20_000)

        val frame = director.frame(atMillis = 20_000)
        assertEquals(Picture.Backdrop(Backdrops.Default), frame.currentPicture)
        assertEquals(Picture.Art(ArtId("a")), frame.previousPicture)
        assertEquals(0f, frame.crossfade, 0.001f)
    }

    @Test
    fun choosing_another_backdrop_crossfades_to_it() {
        val lagoon = BackdropId("builtin:lagoon")
        director.setPictureRules(AlbumArtKeep.Default, savedArt = null, backdrop = lagoon, atMillis = 5_000)

        assertEquals(Picture.Backdrop(lagoon), director.frame(atMillis = 5_000).currentPicture)
    }

    @Test
    fun keeping_the_album_art_after_a_reboot_shows_the_saved_one_before_any_music() {
        director.setPictureRules(AlbumArtKeep(afterPause = false, afterReboot = true), savedArt = ArtId("saved"), backdrop = Backdrops.Default, atMillis = 0)

        assertEquals(Picture.Art(ArtId("saved")), director.frame(atMillis = 0).currentPicture)
    }

    @Test
    fun the_first_rules_apply_at_once_without_a_fade() {
        val fresh = SceneDirector().apply { setVisible(true) }
        fresh.setPictureRules(AlbumArtKeep.Default, savedArt = null, backdrop = BackdropId("builtin:lagoon"), atMillis = 0)

        val frame = fresh.frame(atMillis = 0)
        assertEquals(Picture.Backdrop(BackdropId("builtin:lagoon")), frame.currentPicture)
        assertEquals(1f, frame.crossfade, 0.001f)
    }

    @Test
    fun the_album_art_kept_after_pausing_outlives_the_player_session() {
        director.setPictureRules(AlbumArtKeep(afterPause = true, afterReboot = false), savedArt = null, backdrop = Backdrops.Default, atMillis = 0)
        director.onNowPlaying(playing("a"), atMillis = 0)
        director.onNowPlaying(null, atMillis = 30_000)

        assertEquals(Picture.Art(ArtId("a")), director.frame(atMillis = 30_000).currentPicture)
    }
}
