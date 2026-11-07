package dev.leosnczki.sonora.core.data

import android.graphics.Bitmap
import android.graphics.Color
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.leosnczki.sonora.core.domain.BackdropSource
import dev.leosnczki.sonora.core.domain.Backdrops
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackdropLibraryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), BackdropDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @After
    fun close() = database.close()

    private fun image() = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }

    @Test
    fun saving_the_album_art_adds_it_to_mine_with_its_image() = runTest {
        val (library, _) = library()

        val id = library.save(image(), name = "Blue Album", source = BackdropSource.AlbumArt)

        val saved = library.saved.first().single()
        assertEquals(id, saved.id)
        assertEquals("Blue Album", saved.name)
        assertTrue(library.imageFile(saved).exists())
        assertNotNull(library.image(id))
    }

    @Test
    fun deleting_removes_the_file_and_the_row() = runTest {
        val (library, _) = library()
        val id = library.save(image(), name = "Old", source = BackdropSource.Upload)
        val file = library.imageFile(library.saved.first().single())

        library.delete(id)

        assertTrue(library.saved.first().isEmpty())
        assertFalse(file.exists())
    }

    @Test
    fun deleting_the_chosen_backdrop_falls_back_to_the_first_built_in_one() = runTest {
        val (library, settings) = library()
        val id = library.save(image(), name = "Mine", source = BackdropSource.Upload)
        settings.setBackdrop(id)

        library.delete(id)

        assertEquals(Backdrops.Default, settings.settings.first().backdrop)
    }

    @Test
    fun deleting_another_backdrop_keeps_the_choice() = runTest {
        val (library, settings) = library()
        val kept = library.save(image(), name = "Kept", source = BackdropSource.Upload)
        val gone = library.save(image(), name = "Gone", source = BackdropSource.Upload)
        settings.setBackdrop(kept)

        library.delete(gone)

        assertEquals(kept, settings.settings.first().backdrop)
        assertEquals(listOf(kept), library.saved.first().map { it.id })
    }

    private fun kotlinx.coroutines.test.TestScope.library(): Pair<BackdropLibrary, SettingsRepository> {
        val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "s.preferences_pb") })
        return BackdropLibrary(database.backdrops(), settings, folder.newFolder(), clock = { 1_000L }) to settings
    }

}
