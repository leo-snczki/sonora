package dev.leosnczki.sonora.core.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.BackdropSource
import dev.leosnczki.sonora.core.domain.Backdrops
import dev.leosnczki.sonora.core.domain.SavedBackdrop
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** One saved Backdrop (`replica/architecture.md`): times are UTC epoch millis, the image path is filesDir-relative. */
@Entity(tableName = "backdrops", indices = [Index(value = ["sort_order", "created_at"], name = "backdrops_order")])
data class BackdropEntity(
    @PrimaryKey val id: String,
    val name: String,
    val source: String,
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Dao
interface BackdropDao {
    @Query("select * from backdrops order by sort_order, created_at")
    fun all(): Flow<List<BackdropEntity>>

    @Query("select * from backdrops where id = :id")
    suspend fun find(id: String): BackdropEntity?

    @Upsert
    suspend fun upsert(backdrop: BackdropEntity)

    @Query("delete from backdrops where id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [BackdropEntity::class], version = 1, exportSchema = false)
abstract class BackdropDatabase : RoomDatabase() {
    abstract fun backdrops(): BackdropDao
}

/**
 * The user's own Backdrops ("Mine"): a row each, and its image in private storage under [dir].
 * Deleting falls back to the first built-in Backdrop if it was the chosen one, then removes the
 * file, then the row (an orphan file is harmless, a row pointing at nothing is not).
 */
class BackdropLibrary(
    private val dao: BackdropDao,
    private val settings: SettingsRepository,
    private val dir: File,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val saved: Flow<List<SavedBackdrop>> = dao.all().map { rows -> rows.map { it.toBackdrop() } }

    fun imageFile(backdrop: SavedBackdrop) = File(dir, backdrop.imagePath)

    suspend fun save(image: Bitmap, name: String, source: BackdropSource): BackdropId = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val path = "$id.png"
        dir.mkdirs()
        File(dir, path).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val now = clock()
        dao.upsert(BackdropEntity(id, name, source.key, path, sortOrder = 0, createdAt = now, updatedAt = now))
        BackdropId(id)
    }

    /** The saved Backdrop's image, or null if it isn't one (or its file is gone). Reads a file. */
    suspend fun image(id: BackdropId): Bitmap? = withContext(Dispatchers.IO) {
        if (Backdrops.isBuiltin(id)) return@withContext null
        val row = dao.find(id.value) ?: return@withContext null
        runCatching { BitmapFactory.decodeFile(File(dir, row.imagePath).path) }.getOrNull()
    }

    suspend fun delete(id: BackdropId) = withContext(Dispatchers.IO) {
        val row = dao.find(id.value) ?: return@withContext
        // The choice moves first, so the wallpaper never points at a Backdrop that is gone.
        if (settings.settings.first().backdrop == id) settings.setBackdrop(Backdrops.Default)
        File(dir, row.imagePath).delete()
        dao.delete(id.value)
    }

    private fun BackdropEntity.toBackdrop() = SavedBackdrop(
        id = BackdropId(id),
        name = name,
        source = BackdropSource.entries.firstOrNull { it.key == source } ?: BackdropSource.Upload,
        imagePath = imagePath,
        createdAt = createdAt,
    )
}

/** As stored in the source column (`'album_art'` or `'upload'`). */
private val BackdropSource.key
    get() = when (this) {
        BackdropSource.AlbumArt -> "album_art"
        BackdropSource.Upload -> "upload"
    }
