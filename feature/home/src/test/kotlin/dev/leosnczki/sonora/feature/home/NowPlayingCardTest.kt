package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NowPlayingCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = object : HomeActions {
        override fun allowNotificationAccess() { calls += "access" }
        override fun openPlayerChooser() { calls += "chooser" }
        override fun openPlayer() { calls += "player" }
        override fun restartListener() { calls += "restart" }
    }

    private fun show(card: NowPlayingCardState) = compose.setContent {
        HomeScreen(HomeScreenState.Preview.copy(wallpaperSet = true, notificationAccess = true, nowPlaying = card), actions)
    }

    @Test
    fun without_access_it_explains_and_offers_to_allow_it() {
        show(NowPlayingCardState.NoAccess)

        compose.onNodeWithText("needs notification access", substring = true).assertExists()
        compose.onNodeWithText("Allow access", ignoreCase = true).performScrollTo().performClick()

        assertEquals(listOf("access"), calls)
    }

    @Test
    fun with_nothing_playing_it_offers_to_open_a_music_app() {
        show(NowPlayingCardState.Idle(showDetectionHint = false))

        compose.onNodeWithText("Nothing is playing", substring = true).assertExists()
        compose.onNodeWithText("Open a music app", ignoreCase = true).performScrollTo().performClick()

        assertEquals(listOf("chooser"), calls)
    }

    @Test
    fun while_playing_it_shows_the_track_and_opens_the_player() {
        show(NowPlayingCardState.Playing(title = "Song", artist = "Band", album = "Record", playerName = "Spotify", art = null, paused = false))

        compose.onNodeWithContentDescription("Now playing: Song by Band, Record, Playing in Spotify").assertIsDisplayed()
        compose.onNodeWithText("Open Spotify", ignoreCase = true).performScrollTo().performClick()

        assertEquals(listOf("player"), calls)
    }

    @Test
    fun the_detection_hint_offers_to_restart_the_listener() {
        show(NowPlayingCardState.Idle(showDetectionHint = true))

        compose.onNodeWithText("Not detecting your music", substring = true).assertExists()
        compose.onNodeWithText("Restart listener", ignoreCase = true).performScrollTo().performClick()

        assertEquals(listOf("restart"), calls)
    }

    @Test
    fun a_message_is_shown_as_a_snackbar() {
        compose.setContent {
            HomeScreen(HomeScreenState.Preview.copy(message = HomeMessage.ListenerRestarted), actions)
        }

        compose.onNodeWithText("Listener restarted", substring = true).assertExists()
    }
}
