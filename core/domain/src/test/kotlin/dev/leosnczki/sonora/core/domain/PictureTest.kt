package dev.leosnczki.sonora.core.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class PictureTest {
    private val backdrop = Backdrops.Default
    private val saved = ArtId("saved")
    private val cover = ArtId("cover")

    private fun nowPlaying(playing: Boolean = true, stopped: Boolean = false, art: ArtId? = cover) =
        NowPlaying(player = "p", track = null, art = art, playing = playing, stopped = stopped)

    private fun picture(nowPlaying: NowPlaying?, afterPause: Boolean, afterReboot: Boolean) =
        pictureFor(nowPlaying, AlbumArtKeep(afterPause, afterReboot), savedArt = saved, backdrop = backdrop)

    @Test
    fun playing_or_briefly_paused_music_shows_its_album_art_whatever_is_kept() {
        for (afterPause in listOf(false, true)) for (afterReboot in listOf(false, true)) {
            assertEquals(Picture.Art(cover), picture(nowPlaying(), afterPause, afterReboot))
            assertEquals(Picture.Art(cover), picture(nowPlaying(playing = false), afterPause, afterReboot), "within the grace")
        }
    }

    @Test
    fun once_stopped_the_backdrop_shows_unless_the_album_art_is_kept_after_pausing() {
        val stopped = nowPlaying(playing = false, stopped = true)

        assertEquals(Picture.Backdrop(backdrop), picture(stopped, afterPause = false, afterReboot = false))
        assertEquals(Picture.Backdrop(backdrop), picture(stopped, afterPause = false, afterReboot = true))
        assertEquals(Picture.Art(cover), picture(stopped, afterPause = true, afterReboot = false))
        assertEquals(Picture.Art(cover), picture(stopped, afterPause = true, afterReboot = true))
    }

    @Test
    fun before_any_music_the_saved_album_art_shows_only_when_kept_after_rebooting() {
        assertEquals(Picture.Backdrop(backdrop), picture(null, afterPause = false, afterReboot = false))
        assertEquals(Picture.Backdrop(backdrop), picture(null, afterPause = true, afterReboot = false))
        assertEquals(Picture.Art(saved), picture(null, afterPause = false, afterReboot = true))
        assertEquals(Picture.Art(saved), picture(null, afterPause = true, afterReboot = true))
    }

    @Test
    fun music_without_album_art_shows_the_backdrop() {
        assertEquals(Picture.Backdrop(backdrop), picture(nowPlaying(art = null), afterPause = true, afterReboot = true))
    }

    @Test
    fun with_nothing_saved_the_backdrop_shows_before_any_music() {
        assertEquals(
            Picture.Backdrop(backdrop),
            pictureFor(null, AlbumArtKeep(afterPause = true, afterReboot = true), savedArt = null, backdrop = backdrop),
        )
    }

    @Test
    fun there_are_eight_built_in_backdrops_of_two_to_four_colours_and_the_first_is_the_default() {
        assertEquals(8, Backdrops.Builtin.size)
        assertTrue(Backdrops.Builtin.all { it.colors.size in 2..4 })
        assertEquals(Backdrops.Builtin.first().id, Backdrops.Default)
        assertEquals(Backdrops.Builtin.size, Backdrops.Builtin.map { it.id }.toSet().size, "ids are unique")
    }
}
