package dev.leosnczki.sonora.core.media

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.util.concurrent.Executor
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // real pixels through PNG and back
class LastAlbumArtTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val now = Executor { it.run() }

    /** One run of the app: a fresh store and a fresh LastAlbumArt over the same private folder. */
    private fun run(dir: File) = AlbumArtStore().let { store -> store to LastAlbumArt(dir, store, now) }

    private fun cover() = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply {
        eraseColor(Color.rgb(200, 40, 90))
        setPixel(3, 3, Color.WHITE)
    }

    @Test
    fun the_last_album_art_is_there_after_a_restart() {
        val dir = folder.newFolder()
        val (store, last) = run(dir)
        val id = store.idOf(cover())
        last.remember(id)

        val (_, afterReboot) = run(dir)
        assertEquals(id, afterReboot.load())
    }

    @Test
    fun forgetting_it_deletes_it() {
        val dir = folder.newFolder()
        val (store, last) = run(dir)
        last.remember(store.idOf(cover()))
        last.forget()

        assertNull(run(dir).second.load())
    }

    @Test
    fun nothing_saved_loads_nothing() {
        assertNull(run(folder.newFolder()).second.load())
    }

    @Test
    fun a_write_that_could_not_happen_is_tried_again() {
        val dir = folder.newFolder()
        val (store, last) = run(dir)
        val id = store.idOf(cover())
        val (_, elsewhere) = run(dir)

        // This instance's store doesn't know the cover yet: nothing can be written.
        elsewhere.remember(id)
        assertNull(run(dir).second.load())

        last.remember(id)
        assertEquals(id, run(dir).second.load())
    }

    @Test
    fun the_kept_album_art_follows_what_is_saved_and_forgotten() {
        val (store, last) = run(folder.newFolder())
        val id = store.idOf(cover())

        last.remember(id)
        assertEquals(id, last.saved.value)
        last.forget()
        assertNull(last.saved.value)
    }

    @Test
    fun the_loaded_album_art_stays_drawable_after_the_store_forgets_it() {
        val dir = folder.newFolder()
        val (store, last) = run(dir)
        last.remember(store.idOf(cover()))
        val (_, afterReboot) = run(dir)
        val id = afterReboot.load()!!

        assertNotNull(afterReboot.bitmap(id))
    }
}
