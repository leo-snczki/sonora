package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.leosnczki.sonora.core.domain.Backdrops
import dev.leosnczki.sonora.core.domain.SonoraSettings
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumArtGroupTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = object : HomeActions {
        override fun setAlbumArtOverride(enabled: Boolean) { calls += "album=$enabled" }
        override fun setKeepAfterPause(enabled: Boolean) { calls += "pause=$enabled" }
        override fun setKeepAfterReboot(enabled: Boolean) { calls += "reboot=$enabled" }
    }

    private fun show(state: HomeScreenState) = compose.setContent { HomeScreen(state, actions) }

    @Test
    fun the_group_keeps_the_album_art_after_pausing_and_after_rebooting() {
        show(HomeScreenState.Preview.copy(setup = null, settings = SonoraSettings.Default.copy(albumArtOverride = true)))

        compose.onNodeWithText("Keep after pausing").performScrollTo().performClick()
        compose.onNodeWithText("Keep after rebooting").performScrollTo().performClick()

        assertEquals(listOf("pause=true", "reboot=true"), calls)
    }

    @Test
    fun the_group_switch_turns_it_on() {
        show(HomeScreenState.Preview.copy(setup = null))

        compose.onNodeWithText("Album Art").performScrollTo().performClick()

        assertEquals(listOf("album=true"), calls)
    }

    @Test
    fun when_sonora_is_not_the_wallpaper_the_screen_draws_the_backdrop_itself() {
        show(HomeScreenState.Preview.copy(setup = null, backdropBehind = Backdrops.Builtin.first().colors))
        compose.onNodeWithTag(BACKDROP_TAG).assertExists()
    }

    @Test
    fun when_sonora_is_the_wallpaper_the_screen_shows_it_through() {
        show(HomeScreenState.Preview.copy(setup = null, backdropBehind = null))
        compose.onNodeWithTag(BACKDROP_TAG).assertDoesNotExist()
    }
}
