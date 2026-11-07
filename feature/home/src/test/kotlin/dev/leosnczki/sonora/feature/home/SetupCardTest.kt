package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.leosnczki.sonora.core.domain.SetupStepKind
import dev.leosnczki.sonora.core.domain.SetupStepState
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = object : HomeActions {
        override fun setWallpaper() { calls += "wallpaper" }
        override fun allowNotificationAccess() { calls += "access" }
        override fun allowAudioAccess() { calls += "audio" }
        override fun openBatterySettings() { calls += "battery" }
    }

    private fun steps(wallpaper: Boolean, access: Boolean, audio: Boolean) = listOf(
        SetupStepState(SetupStepKind.Wallpaper, wallpaper, optional = false),
        SetupStepState(SetupStepKind.NotificationAccess, access, optional = false),
        SetupStepState(SetupStepKind.AudioAccess, audio, optional = true),
    )

    private fun show(state: HomeScreenState) = compose.setContent {
        HomeScreen(state.copy(nowPlaying = NowPlayingCardState.Idle(showDetectionHint = false)), actions)
    }

    @Test
    fun each_step_opens_its_system_screen() {
        show(HomeScreenState.Preview.copy(setup = steps(wallpaper = false, access = false, audio = false)))

        compose.onNodeWithText("Set wallpaper", ignoreCase = true).performScrollTo().performClick()
        compose.onNodeWithText("Allow access", ignoreCase = true).performScrollTo().performClick()
        compose.onNodeWithText("Allow audio", ignoreCase = true).performScrollTo().performClick()

        assertEquals(listOf("wallpaper", "access", "audio"), calls)
    }

    @Test
    fun a_finished_step_is_announced_as_done_and_the_audio_step_as_optional() {
        show(HomeScreenState.Preview.copy(setup = steps(wallpaper = true, access = false, audio = false)))

        compose.onNodeWithContentDescription("Step 1 of 3, Make Sonora your wallpaper")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "done"))
        compose.onNodeWithContentDescription("Step 3 of 3", substring = true).performScrollTo()
        compose.onNodeWithText("optional", substring = true).assertExists()
        compose.onNodeWithText("never the microphone", substring = true).assertExists()
    }

    @Test
    fun the_card_is_gone_when_setup_is_done() {
        show(HomeScreenState.Preview.copy(setup = null))

        compose.onNodeWithText("Get started", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun a_single_missing_step_is_step_one_of_one() {
        show(HomeScreenState.Preview.copy(setup = listOf(SetupStepState(SetupStepKind.NotificationAccess, done = false, optional = false))))

        compose.onNodeWithContentDescription("Step 1 of 1, Let Sonora see what is playing").assertExists()
        compose.onNodeWithText("Set wallpaper", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun the_battery_hint_shows_only_when_asked_and_opens_the_battery_settings() {
        show(HomeScreenState.Preview.copy(setup = null, showBatteryHint = true))

        compose.onNodeWithText("Battery settings", ignoreCase = true).performScrollTo().performClick()
        assertEquals(listOf("battery"), calls)
    }

    @Test
    fun no_battery_hint_by_default() {
        show(HomeScreenState.Preview.copy(setup = null, showBatteryHint = false))

        compose.onNodeWithText("Battery settings", ignoreCase = true).assertDoesNotExist()
    }
}
