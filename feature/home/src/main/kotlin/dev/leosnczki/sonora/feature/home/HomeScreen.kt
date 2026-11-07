package dev.leosnczki.sonora.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.leosnczki.sonora.core.designsystem.SonoraSize
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraTheme
import dev.leosnczki.sonora.core.designsystem.component.PageScrim
import dev.leosnczki.sonora.core.designsystem.component.SonoraHeader
import dev.leosnczki.sonora.core.designsystem.component.SonoraSnackbarHost
import dev.leosnczki.sonora.core.domain.FluidStyle
import dev.leosnczki.sonora.core.domain.GraphicsEffect
import dev.leosnczki.sonora.core.domain.SetupStatus
import dev.leosnczki.sonora.core.domain.SetupStepState
import dev.leosnczki.sonora.core.domain.SonoraSettings
import dev.leosnczki.sonora.core.domain.setupSteps

/** Tags the Backdrop the screen draws behind itself when Sonora is not the wallpaper. */
internal const val BACKDROP_TAG = "backdrop"

/** Everything the settings screen shows. Filled by [HomeViewModel] in the app. */
data class HomeScreenState(
    val versionName: String,
    /** Steps for the setup card, or null to hide it. */
    val setup: List<SetupStepState>? = null,
    val showBatteryHint: Boolean = false,
    val nowPlaying: NowPlayingCardState = NowPlayingCardState.NoAccess,
    val message: HomeMessage? = null,
    val settings: SonoraSettings = SonoraSettings.Default,
    /** Whether Beat Pulse may listen to the device's audio. */
    val audioAccess: Boolean = false,
    /** Music plays but Beat Pulse hears nothing: the Player can't be heard. */
    val playerSilent: Boolean = false,
    /** The Backdrop's colours to draw behind the screen when Sonora is not the wallpaper; null shows the wallpaper through. */
    val backdropBehind: List<Int>? = null,
) {
    companion object {
        val Preview = HomeScreenState(
            versionName = "0.1.0",
            setup = setupSteps(SetupStatus(wallpaperSet = false, notificationAccess = false, audioAccess = false), completedOnce = false),
        )
    }
}

/** One-off confirmations, shown as a snackbar. */
enum class HomeMessage { ListenerRestarted }

/** Everything the settings screen can ask for. The activity turns these into intents. */
interface HomeActions {
    fun setWallpaper() {}
    fun allowNotificationAccess() {}
    fun allowAudioAccess() {}
    fun openBatterySettings() {}
    fun openPlayerChooser() {}
    fun openPlayer() {}
    fun restartListener() {}
    fun messageShown() {}
    fun setFluidStyle(style: FluidStyle) {}
    fun setCoverFlipVertical(enabled: Boolean) {}
    fun setCoverMirrorHorizontal(enabled: Boolean) {}
    fun setFluidOverride(enabled: Boolean) {}
    fun setFluidScale(scale: Float) {}
    fun setSpeedRange(min: Float, max: Float) {}
    fun setGraphicsOverride(enabled: Boolean) {}
    fun setGraphicsEffect(effect: GraphicsEffect) {}
    fun setMiscOverride(enabled: Boolean) {}
    fun setBrightness(brightness: Float) {}
    fun setBeatOverride(enabled: Boolean) {}
    fun setBeatStrength(strength: Float) {}
    fun setAlbumArtOverride(enabled: Boolean) {}
    fun setKeepAfterPause(enabled: Boolean) {}
    fun setKeepAfterReboot(enabled: Boolean) {}
    fun openProjectPage() {}
    fun openLicences() {}

    object None : HomeActions
}

@Composable
fun HomeScreen(state: HomeScreenState, actions: HomeActions, modifier: Modifier = Modifier) {
    SonoraTheme {
        val behind = state.backdropBehind?.let { colours ->
            modifier.testTag(BACKDROP_TAG).background(Brush.linearGradient(colours.map(::Color)))
        } ?: modifier
        PageScrim(behind) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                val twoColumns = maxWidth >= SonoraSize.TwoColumnBreakpoint
                Column(
                    Modifier
                        .widthIn(max = SonoraSize.MaxContentWidth)
                        .fillMaxWidth()
                        .padding(SonoraSpace.Md),
                    verticalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
                ) {
                    SonoraHeader(stringResource(R.string.home_title), stringResource(R.string.home_tagline))
                    val primary: List<@Composable () -> Unit> = listOfNotNull(
                        state.setup?.let { steps -> { SetupCard(steps, actions) } },
                        { NowPlayingCard(state.nowPlaying, actions) },
                        { LookCard(state, actions) },
                    )
                    val secondary: List<@Composable () -> Unit> = listOf(
                        { HelpCard(state.showBatteryHint, actions) },
                        { AboutCard(state.versionName, actions) },
                    )
                    if (twoColumns) {
                        Row(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Md)) {
                            CardColumn(primary, Modifier.weight(1f))
                            CardColumn(secondary, Modifier.weight(1f))
                        }
                    } else {
                        CardColumn(primary + secondary)
                    }
                }
            }
            MessageSnackbar(
                state.message,
                actions,
                Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(SonoraSpace.Md),
            )
        }
    }
}

@Composable
private fun MessageSnackbar(message: HomeMessage?, actions: HomeActions, modifier: Modifier = Modifier) {
    val host = remember { SnackbarHostState() }
    val text = when (message) {
        HomeMessage.ListenerRestarted -> stringResource(R.string.message_listener_restarted)
        null -> null
    }
    LaunchedEffect(message) {
        if (text != null) {
            host.showSnackbar(text)
            actions.messageShown()
        }
    }
    SonoraSnackbarHost(host, modifier)
}

@Composable
private fun CardColumn(cards: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SonoraSpace.Sm)) {
        cards.forEach { it() }
    }
}
