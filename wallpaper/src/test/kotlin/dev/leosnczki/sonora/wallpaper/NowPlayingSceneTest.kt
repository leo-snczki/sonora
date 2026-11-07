package dev.leosnczki.sonora.wallpaper

import android.graphics.Bitmap
import dev.leosnczki.sonora.core.domain.ArtId
import dev.leosnczki.sonora.core.domain.NowPlaying
import dev.leosnczki.sonora.core.domain.SceneDirector
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NowPlayingSceneTest {
    private val placeholder = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
    private val cover = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
    private val available = mutableMapOf<ArtId, Bitmap>()
    private val scene = NowPlayingScene(artFor = { available[it] }, fallbackImage = { placeholder })

    private fun playing(art: String) = NowPlaying("player", null, ArtId(art), playing = true, stopped = false)

    @Test
    fun flow_lands_on_the_first_image_then_walks_by_the_time_between_drawn_frames() {
        assertEquals(SceneDirector.FLOW_SNAP, scene.frameAt(nanos = 5_000_000_000L).flowStep)
        // Frame-rate and image checks in between must not eat into the next step.
        scene.targetFps(nanos = 5_008_000_000L)
        scene.takeImages(nanos = 5_008_000_000L)

        assertEquals(0.6f * 0.016f, scene.frameAt(nanos = 5_016_000_000L).flowStep, 0.0001f)
    }

    @Test
    fun each_frame_carries_the_beat_pulse() {
        val pulsing = NowPlayingScene(artFor = { null }, fallbackImage = { placeholder }, pulseAt = { 0.25f })

        assertEquals(0.25f, pulsing.frameAt(nanos = 5_000_000_000L).pulse)
    }

    @Test
    fun the_first_frame_gets_images_to_draw() {
        assertNotNull(scene.takeImages(nanos = 5_000_000_000L))
    }

    @Test
    fun the_album_art_is_handed_over_once_and_not_again() {
        available[ArtId("a")] = cover
        scene.onNowPlaying(playing("a"))

        assertSame(cover, scene.takeImages(nanos = 5_000_000_000L)?.current)
        assertNull(scene.takeImages(nanos = 5_016_000_000L))
    }

    @Test
    fun art_that_was_not_ready_is_retried_a_second_later() {
        scene.onNowPlaying(playing("a"))
        assertSame(placeholder, scene.takeImages(nanos = 5_000_000_000L)?.current)

        available[ArtId("a")] = cover
        assertNull(scene.takeImages(nanos = 5_500_000_000L), "not hammering every frame")
        assertSame(cover, scene.takeImages(nanos = 6_100_000_000L)?.current)
    }
}
