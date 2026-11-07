package dev.leosnczki.sonora.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.leosnczki.sonora.BuildConfig
import dev.leosnczki.sonora.core.data.SettingsRepository
import dev.leosnczki.sonora.core.domain.ArtId
import dev.leosnczki.sonora.core.domain.DetectionWatch
import dev.leosnczki.sonora.core.domain.FluidStyle
import dev.leosnczki.sonora.core.domain.GraphicsEffect
import dev.leosnczki.sonora.core.domain.NowPlaying
import dev.leosnczki.sonora.core.domain.SetupStatus
import dev.leosnczki.sonora.core.domain.setupSteps
import dev.leosnczki.sonora.core.domain.shouldShowBatteryHint
import dev.leosnczki.sonora.core.media.BeatListener
import dev.leosnczki.sonora.core.media.NowPlayingRepository
import dev.leosnczki.sonora.core.media.monotonicMillis
import dev.leosnczki.sonora.feature.home.HomeMessage
import dev.leosnczki.sonora.feature.home.HomeScreenState
import dev.leosnczki.sonora.feature.home.NowPlayingCardState
import dev.leosnczki.sonora.feature.home.PlayerApp
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Builds the settings screen state from Now Playing and the device's status. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NowPlayingRepository,
    private val settings: SettingsRepository,
    private val device: DeviceStatus,
    private val beats: BeatListener,
) : ViewModel() {
    private val detection = DetectionWatch()
    private var status = SetupStatus(wallpaperSet = false, notificationAccess = false, audioAccess = false)
    private var art: Pair<ArtId, ImageBitmap?>? = null

    private val _state = MutableStateFlow(HomeScreenState(versionName = BuildConfig.VERSION_NAME))
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    private val _players = MutableStateFlow<List<PlayerApp>?>(null)

    /** Non-null while the Player chooser is open. */
    val players: StateFlow<List<PlayerApp>?> = _players.asStateFlow()

    init {
        viewModelScope.launch { repository.nowPlaying.collect { refreshCard() } }
        viewModelScope.launch { repository.sessionSeen.collect { refreshCard() } }
        viewModelScope.launch { beats.playerSilent.collect { silent -> _state.update { it.copy(playerSilent = silent) } } }
        viewModelScope.launch {
            settings.settings.collect { value ->
                settingsLoaded = true
                _state.update { it.copy(settings = value) }
                refreshSetup()
            }
        }
    }

    private var ticker: Job? = null
    private var settingsLoaded = false

    /** The detection hint depends on time passing: look again every few seconds, only while visible. */
    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                delay(5_000)
                refreshCard()
            }
        }
    }

    fun onPause() {
        ticker?.cancel()
        ticker = null
    }

    /** Call on resume: permissions and the wallpaper may have changed in system settings. */
    fun onResume() {
        val access = repository.hasAccess()
        if (access) repository.start()
        status = SetupStatus(device.isSonoraTheWallpaper(), access, device.hasAudioAccess())
        // Audio access may have just been granted: Beat Pulse can start listening.
        beats.update()
        val battery = shouldShowBatteryHint(device.manufacturer(), device.isExemptFromBatteryOptimisation())
        _state.update { it.copy(showBatteryHint = battery, audioAccess = status.audioAccess) }
        refreshSetup()
        refreshCard()
        startTicker()
    }

    private fun refreshSetup() {
        // Until the settings are read we don't know if setup was completed before: show nothing yet.
        if (!settingsLoaded) return
        val completedOnce = _state.value.settings.setupCompletedOnce
        if (status.requiredDone && !completedOnce) save { settings.markSetupCompleted() }
        _state.update { it.copy(setup = setupSteps(status, completedOnce)) }
    }

    private fun refreshCard() {
        val access = status.notificationAccess
        val now = monotonicMillis()
        detection.update(access, repository.sessionSeen.value, now)
        val hint = detection.showHint(now)
        val nowPlaying = repository.nowPlaying.value
        val card = when {
            !access -> NowPlayingCardState.NoAccess
            nowPlaying == null || nowPlaying.stopped -> NowPlayingCardState.Idle(showDetectionHint = hint)
            else -> playing(nowPlaying, hint)
        }
        _state.update { it.copy(nowPlaying = card) }
    }

    private fun playing(nowPlaying: NowPlaying, hint: Boolean): NowPlayingCardState.Playing {
        val id = nowPlaying.art
        val cached = art?.takeIf { it.first == id }?.second
        if (id != null && art?.first != id) loadArt(id)
        return NowPlayingCardState.Playing(
            title = nowPlaying.track?.title,
            artist = nowPlaying.track?.artist,
            album = nowPlaying.track?.album,
            playerName = device.appName(nowPlaying.player),
            art = cached,
            paused = !nowPlaying.playing,
            showDetectionHint = hint,
        )
    }

    private fun loadArt(id: ArtId) {
        art = id to null
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) { repository.art(id)?.asImageBitmap() }
            if (art?.first == id) {
                // A cover that is not available yet is retried on the next refresh.
                art = if (bitmap != null) id to bitmap else null
                refreshCard()
            }
        }
    }

    fun nowPlayingPlayer(): String? = repository.nowPlaying.value?.player

    fun restartListener() {
        repository.restartListener()
        _state.update { it.copy(message = HomeMessage.ListenerRestarted) }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    fun setFluidStyle(style: FluidStyle) = save { settings.setFluidStyle(style) }

    fun setCoverFlipVertical(enabled: Boolean) = save { settings.setCoverFlipVertical(enabled) }

    fun setCoverMirrorHorizontal(enabled: Boolean) = save { settings.setCoverMirrorHorizontal(enabled) }

    fun setFluidOverride(enabled: Boolean) = save { settings.setFluidOverride(enabled) }

    fun setFluidScale(scale: Float) = saveSoon("scale") { settings.setFluidScale(scale) }

    fun setSpeedRange(min: Float, max: Float) = saveSoon("speed") { settings.setSpeedRange(min, max) }

    fun setGraphicsOverride(enabled: Boolean) = save { settings.setGraphicsOverride(enabled) }

    fun setGraphicsEffect(effect: GraphicsEffect) = save { settings.setGraphicsEffect(effect) }

    fun setMiscOverride(enabled: Boolean) = save { settings.setMiscOverride(enabled) }

    fun setBeatOverride(enabled: Boolean) = save { settings.setBeatOverride(enabled) }

    fun setBeatStrength(strength: Float) = saveSoon("beat") { settings.setBeatStrength(strength) }

    fun setBrightness(brightness: Float) = saveSoon("brightness") { settings.setBrightness(brightness) }

    private fun save(write: suspend () -> Unit) {
        viewModelScope.launch { write() }
    }

    private val pendingWrites = mutableMapOf<String, Job>()

    /** Sliders report every drag step: write the latest value once it settles for a moment. */
    private fun saveSoon(key: String, write: suspend () -> Unit) {
        pendingWrites.remove(key)?.cancel()
        pendingWrites[key] = viewModelScope.launch {
            delay(SLIDER_WRITE_DELAY_MILLIS)
            write()
        }
    }

    fun openPlayerChooser() {
        viewModelScope.launch { _players.value = withContext(Dispatchers.Default) { device.installedPlayers() } }
    }

    fun closePlayerChooser() {
        _players.value = null
    }

    private companion object {
        const val SLIDER_WRITE_DELAY_MILLIS = 120L
    }
}
