package dev.leosnczki.sonora

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import dev.leosnczki.sonora.feature.home.HomeActions
import dev.leosnczki.sonora.feature.home.HomeScreen
import dev.leosnczki.sonora.feature.home.HomeScreenState
import dev.leosnczki.sonora.feature.home.LicencesScreen

private const val PROJECT_URL = "https://github.com/leo-snczki/sonora"

/** The settings screen. Also the live wallpaper's "settings" entry in the system picker. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var showLicences by rememberSaveable { mutableStateOf(false) }
            val actions = object : HomeActions {
                override fun openProjectPage() = startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_URL)))
                override fun openLicences() {
                    showLicences = true
                }
            }
            if (showLicences) {
                LicencesScreen(onBack = { showLicences = false })
            } else {
                HomeScreen(HomeScreenState(versionName = BuildConfig.VERSION_NAME), actions)
            }
        }
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No app can handle it (e.g. no browser); nothing useful to do.
        }
    }
}
