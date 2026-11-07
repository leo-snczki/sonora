package dev.leosnczki.sonora.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.leosnczki.sonora.core.data.BackdropLibrary
import dev.leosnczki.sonora.core.data.SettingsRepository
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.BackdropSource
import dev.leosnczki.sonora.core.domain.Backdrops
import dev.leosnczki.sonora.core.media.NowPlayingRepository
import dev.leosnczki.sonora.feature.backdrops.BackdropLibraryState
import dev.leosnczki.sonora.feature.backdrops.MineBackdrop
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {
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
            val image = library.image(id)?.asImageBitmap() ?: return@launch
            images.update { it + (id to image) }
        }
    }

    fun choose(id: BackdropId) {
        viewModelScope.launch { settings.setBackdrop(id) }
    }

    /** Saves what is playing as a Backdrop in Mine, named after its album or track. */
    fun saveAlbumArt() {
        val playing = nowPlaying.nowPlaying.value ?: return
        val art = playing.art ?: return
        viewModelScope.launch {
            val image = withContext(Dispatchers.Default) { nowPlaying.art(art) } ?: return@launch
            val name = playing.track?.album ?: playing.track?.title ?: playing.player
            library.save(image, name, BackdropSource.AlbumArt)
        }
    }

    fun delete(id: BackdropId) {
        viewModelScope.launch {
            library.delete(id)
            images.update { it - id }
            reading -= id
        }
    }
}
