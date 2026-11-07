package dev.leosnczki.sonora.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.leosnczki.sonora.core.data.BackdropLibrary
import dev.leosnczki.sonora.core.data.SettingsRepository
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.BackdropSource
import dev.leosnczki.sonora.core.domain.Backdrops
import dev.leosnczki.sonora.core.domain.SquareCrop
import dev.leosnczki.sonora.core.media.NowPlayingRepository
import dev.leosnczki.sonora.feature.backdrops.BackdropLibraryState
import dev.leosnczki.sonora.feature.backdrops.MineBackdrop
import dev.leosnczki.sonora.feature.backdrops.cropped
import dev.leosnczki.sonora.feature.backdrops.R as BackdropsR
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The Backdrop Library (S04): the chosen Backdrop, the user's own ones with their images, and what can be saved. */
@HiltViewModel
class BackdropLibraryViewModel @Inject constructor(
    private val library: BackdropLibrary,
    private val settings: SettingsRepository,
    private val nowPlaying: NowPlayingRepository,
    private val device: DeviceStatus,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _cropping = MutableStateFlow<Picked?>(null)

    /** An image picked for upload, waiting to be cropped (S05); null otherwise. */
    val cropping: StateFlow<Picked?> = _cropping.asStateFlow()

    /** An image the user picked, the name it came with, and the crop it starts from (its EXIF turn). */
    class Picked(val image: Bitmap, val name: String, val crop: SquareCrop)

    /** Images of the user's Backdrops, read once each. */
    private val images = MutableStateFlow<Map<BackdropId, ImageBitmap>>(emptyMap())
    private val reading = mutableSetOf<BackdropId>()

    val state: StateFlow<BackdropLibraryState> = combine(
        settings.settings,
        library.saved.onEach { saved -> saved.forEach { readImage(it.id) } },
        nowPlaying.nowPlaying,
        images,
    ) { settings, saved, playing, images ->
        BackdropLibraryState(
            chosen = settings.backdrop,
            mine = saved.map { MineBackdrop(it.id, it.name, images[it.id]) },
            canSaveAlbumArt = playing?.art != null && !playing.stopped,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackdropLibraryState(chosen = Backdrops.Default))

    private fun readImage(id: BackdropId) {
        if (!reading.add(id)) return
        viewModelScope.launch {
            val image = runCatching { library.image(id) }.getOrNull()?.asImageBitmap()
            // Unreadable for now: the next change of the list tries again.
            if (image == null) reading -= id else images.update { it + (id to image) }
        }
    }

    fun choose(id: BackdropId) {
        viewModelScope.launch { settings.setBackdrop(id) }
    }

    private var saving = false

    /** Saves what is playing as a Backdrop in Mine, named after its album, its track or its Player. */
    fun saveAlbumArt() {
        val playing = nowPlaying.nowPlaying.value ?: return
        val art = playing.art ?: return
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                val image = withContext(Dispatchers.Default) { nowPlaying.art(art) } ?: return@launch
                val name = listOfNotNull(playing.track?.album, playing.track?.title).firstOrNull { it.isNotBlank() }
                    ?: device.appName(playing.player)
                library.save(image, name, BackdropSource.AlbumArt)
            } catch (e: IOException) {
                Log.w(TAG, "could not save the Album Art as a Backdrop", e)
            } finally {
                saving = false
            }
        }
    }

    fun delete(id: BackdropId) {
        viewModelScope.launch {
            try {
                library.delete(id)
            } catch (e: IOException) {
                Log.w(TAG, "could not delete a Backdrop", e)
            }
            images.update { it - id }
            reading -= id
        }
    }

    /** Reads the picked image, no larger than needed for a crop, and opens the cropper. */
    fun picked(uri: Uri) {
        viewModelScope.launch {
            val picked = withContext(Dispatchers.IO) { runCatching { read(uri) }.getOrNull() }
            if (picked == null) Log.w(TAG, "could not read the picked image") else _cropping.value = picked
        }
    }

    private fun read(uri: Uri): Picked? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        // Small enough to hold and turn (long side), large enough for a 512 px Backdrop at 4× zoom (short side).
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_LONG_SIDE ||
            minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MIN_SHORT_SIDE
        ) sample *= 2
        val image = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.substringBeforeLast('.') else null
        }
        // Picker names are often bare ids ("1000012345"): those say nothing in Mine.
        val readable = name?.takeIf { it.isNotBlank() && !it.all(Char::isDigit) }
            ?: context.getString(BackdropsR.string.upload_default_name, DateFormat.getDateInstance().format(Date()))
        return Picked(image, readable, SquareCrop.fromExif(image.width, image.height, orientation))
    }

    fun useCrop(crop: SquareCrop) {
        val picked = _cropping.value ?: return
        _cropping.value = null
        viewModelScope.launch {
            try {
                val image = withContext(Dispatchers.Default) { cropped(picked.image, crop) }
                library.save(image, picked.name, BackdropSource.Upload)
            } catch (e: Exception) {
                // Disk, database or bitmap errors alike: the upload is lost, the app carries on.
                Log.w(TAG, "could not save the uploaded image", e)
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "the uploaded image was too large to crop", e)
            }
        }
    }

    fun cancelCrop() {
        _cropping.value = null
    }

    private companion object {
        const val TAG = "SonoraBackdrops"

        /** A picked image is read with its long side at most this… */
        const val MAX_LONG_SIDE = 4096

        /** …and its short side at least this, when the image has it: plenty for 512 px at 4× zoom. */
        const val MIN_SHORT_SIDE = 2048
    }
}
