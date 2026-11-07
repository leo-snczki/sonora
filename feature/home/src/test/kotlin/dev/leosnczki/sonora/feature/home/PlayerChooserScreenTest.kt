package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerChooserScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lists_installed_players_and_opens_the_chosen_one() {
        var opened: String? = null
        val players = listOf(PlayerApp("Spotify", "com.spotify.music", icon = null), PlayerApp("YouTube Music", "app.revanced.android.apps.youtube.music", icon = null))
        compose.setContent { PlayerChooserScreen(players, onOpen = { opened = it.packageName }, onBack = {}) }

        compose.onNodeWithText("may still work", substring = true).assertExists()
        compose.onNodeWithText("YouTube Music").performClick()

        assertEquals("app.revanced.android.apps.youtube.music", opened)
    }

    @Test
    fun says_so_when_no_music_app_is_installed() {
        compose.setContent { PlayerChooserScreen(emptyList(), onOpen = {}, onBack = {}) }

        compose.onNodeWithText("No music apps found").assertExists()
    }
}
