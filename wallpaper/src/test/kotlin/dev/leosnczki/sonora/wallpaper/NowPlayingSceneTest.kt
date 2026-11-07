package dev.leosnczki.sonora.wallpaper

import android.graphics.Bitmap
import dev.leosnczki.sonora.core.domain.ArtId
import dev.leosnczki.sonora.core.domain.NowPlaying
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
