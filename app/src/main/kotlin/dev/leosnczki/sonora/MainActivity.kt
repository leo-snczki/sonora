package dev.leosnczki.sonora

import android.Manifest
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.leosnczki.sonora.core.domain.FluidStyle
import dev.leosnczki.sonora.core.domain.GraphicsEffect
import dev.leosnczki.sonora.core.media.NowPlayingListenerService
import dev.leosnczki.sonora.feature.home.HomeActions
import dev.leosnczki.sonora.feature.home.HomeScreen
import dev.leosnczki.sonora.feature.home.LicencesScreen
import dev.leosnczki.sonora.feature.home.PlayerChooserScreen
import dev.leosnczki.sonora.home.DeviceStatus
import dev.leosnczki.sonora.home.HomeViewModel
import dev.leosnczki.sonora.wallpaper.SonoraWallpaperService
import javax.inject.Inject

private const val PROJECT_URL = "https://github.com/leo-snczki/sonora"
private const val KEY_LICENCES = "licences"

/** The settings screen. Also the live wallpaper's "settings" entry in the system picker. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var device: DeviceStatus

    private val viewModel: HomeViewModel by viewModels()
    private val requestAudio = registerForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.onResume() }
    private var showLicences by mutableStateOf(false)

    private val actions = object : HomeActions {
        override fun setWallpaper() {
            val component = ComponentName(this@MainActivity, SonoraWallpaperService::class.java)
            val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
            if (!startSafely(direct)) startSafely(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
        }

        override fun allowNotificationAccess() {
            val listener = ComponentName(this@MainActivity, NowPlayingListenerService::class.java)
            val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
            } else {
                null
            }
            if (detail == null || !startSafely(detail)) startSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        override fun allowAudioAccess() = requestAudio.launch(Manifest.permission.RECORD_AUDIO)

        override fun openBatterySettings() {
            startSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        override fun openPlayerChooser() = viewModel.openPlayerChooser()

        override fun openPlayer() {
            val player = viewModel.nowPlayingPlayer() ?: return
            device.launchIntent(player)?.let(::startSafely)
        }

        override fun restartListener() = viewModel.restartListener()

        override fun messageShown() = viewModel.messageShown()

        override fun setFluidStyle(style: FluidStyle) = viewModel.setFluidStyle(style)

        override fun setCoverFlipVertical(enabled: Boolean) = viewModel.setCoverFlipVertical(enabled)

        override fun setCoverMirrorHorizontal(enabled: Boolean) = viewModel.setCoverMirrorHorizontal(enabled)

        override fun setFluidOverride(enabled: Boolean) = viewModel.setFluidOverride(enabled)

        override fun setGraphicsOverride(enabled: Boolean) = viewModel.setGraphicsOverride(enabled)

        override fun setGraphicsEffect(effect: GraphicsEffect) = viewModel.setGraphicsEffect(effect)

        override fun setMiscOverride(enabled: Boolean) = viewModel.setMiscOverride(enabled)

        override fun setBrightness(brightness: Float) = viewModel.setBrightness(brightness)

        override fun setBeatOverride(enabled: Boolean) = viewModel.setBeatOverride(enabled)

        override fun setBeatStrength(strength: Float) = viewModel.setBeatStrength(strength)

        override fun setFluidScale(scale: Float) = viewModel.setFluidScale(scale)

        override fun setSpeedRange(min: Float, max: Float) = viewModel.setSpeedRange(min, max)

        override fun openProjectPage() {
            startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL)))
        }

        override fun openLicences() {
            showLicences = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        showLicences = savedInstanceState?.getBoolean(KEY_LICENCES) == true
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val players by viewModel.players.collectAsStateWithLifecycle()
            val chooser = players
            when {
                showLicences -> LicencesScreen(onBack = { showLicences = false })
                chooser != null -> PlayerChooserScreen(
                    chooser,
                    onOpen = { player ->
                        viewModel.closePlayerChooser()
                        device.launchIntent(player.packageName)?.let(::startSafely)
                    },
                    onBack = viewModel::closePlayerChooser,
                )
                else -> HomeScreen(state, actions)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LICENCES, showLicences)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }

    private fun startSafely(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
