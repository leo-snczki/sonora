package dev.leosnczki.sonora.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.leosnczki.sonora.core.domain.ArtId
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Singleton

/**
 * The last Album Art, kept in private storage while "keep after rebooting" is on, so the wallpaper
 * can show it before any music plays. Writes happen on [io]; one file, replaced atomically.
 */
class LastAlbumArt(private val dir: File, private val store: AlbumArtStore, private val io: Executor) {
    private val file get() = File(dir, FILE_NAME)
    private var remembered: ArtId? = null

    @Synchronized
    fun remember(id: ArtId) {
        if (id == remembered) return
        remembered = id
        io.execute {
            val bitmap = store.scaled(id) ?: return@execute
            val partial = File(dir, "$FILE_NAME.partial")
            runCatching {
                partial.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                partial.renameTo(file)
            }.onFailure { partial.delete() }
        }
    }

    /** Turned off: the Album Art leaves private storage too. */
    @Synchronized
    fun forget() {
        remembered = null
        io.execute { file.delete() }
    }

    /** The saved Album Art, now known to [store], or null. Reads a file: call off the main thread. */
    fun load(): ArtId? {
        val bitmap = runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() ?: return null
        return store.idOf(bitmap).also { synchronized(this) { remembered = it } }
    }

    private companion object {
        const val FILE_NAME = "last_album_art.png"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object LastAlbumArtModule {
    @Provides
    @Singleton
    fun lastAlbumArt(@ApplicationContext context: Context, store: AlbumArtStore) =
        LastAlbumArt(context.filesDir, store, Executors.newSingleThreadExecutor())
}
