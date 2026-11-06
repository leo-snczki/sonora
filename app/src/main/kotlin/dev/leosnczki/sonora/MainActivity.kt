package dev.leosnczki.sonora

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.leosnczki.sonora.core.media.NowPlayingListenerService
import dev.leosnczki.sonora.core.media.NowPlayingRepository
import dev.leosnczki.sonora.feature.home.HomeActions
import dev.leosnczki.sonora.feature.home.HomeScreen
import dev.leosnczki.sonora.feature.home.HomeScreenState
import dev.leosnczki.sonora.feature.home.LicencesScreen
import dev.leosnczki.sonora.wallpaper.SonoraWallpaperService
import javax.inject.Inject

private const val PROJECT_URL = "https://github.com/leo-snczki/sonora"
private const val KEY_LICENCES = "licences"

/** The settings screen. Also the live wallpaper's "settings" entry in the system picker. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var nowPlaying: NowPlayingRepository

    private var state by mutableStateOf(HomeScreenState(versionName = BuildConfig.VERSION_NAME))

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

        override fun openProjectPage() {
            startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL)))
        }

        override fun openLicences() {
            showLicences = true
        }
    }

    private var showLicences by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        showLicences = savedInstanceState?.getBoolean(KEY_LICENCES) == true
        setContent {
            if (showLicences) {
                LicencesScreen(onBack = { showLicences = false })
            } else {
                HomeScreen(state, actions)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LICENCES, showLicences)
    }

    override fun onResume() {
        super.onResume()
        val access = nowPlaying.hasAccess()
        if (access) nowPlaying.start()
        state = state.copy(wallpaperSet = isSonoraTheWallpaper(), notificationAccess = access)
    }

    private fun isSonoraTheWallpaper(): Boolean =
        WallpaperManager.getInstance(this).wallpaperInfo?.packageName == packageName

    private fun startSafely(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
