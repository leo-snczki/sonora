package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import dev.leosnczki.sonora.core.domain.SonoraSettings
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BeatPulseGroupTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = object : HomeActions {
        override fun allowAudioAccess() { calls += "allow" }
        override fun setBeatOverride(enabled: Boolean) { calls += "beat=$enabled" }
        override fun setBeatStrength(strength: Float) { calls += "strength=$strength" }
    }

    private fun show(settings: SonoraSettings, audioAccess: Boolean, playerSilent: Boolean = false) = compose.setContent {
        HomeScreen(
            HomeScreenState.Preview.copy(setup = null, settings = settings, audioAccess = audioAccess, playerSilent = playerSilent),
            actions,
        )
    }

    @Test
    fun without_audio_access_the_group_is_locked_with_the_reason_and_an_allow_action() {
        show(SonoraSettings.Default.copy(beatOverride = true), audioAccess = false)

        compose.onNodeWithText("Needs audio access. Sonora only hears what your device plays, not the mic").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Strength").assertDoesNotExist()
        compose.onNodeWithText("Allow", ignoreCase = true).performScrollTo().performClick()

        assertEquals(listOf("allow"), calls)
    }

    @Test
    fun with_audio_access_the_switch_turns_it_on_and_the_strength_changes_it() {
        show(SonoraSettings.Default, audioAccess = true)
        compose.onNodeWithText("Beat Pulse").performScrollTo().performClick()
        assertEquals(listOf("beat=true"), calls)
    }

    @Test
    fun strength_speaks_as_a_percentage() {
        show(SonoraSettings.Default.copy(beatOverride = true, beatStrength = 0.5f), audioAccess = true)

        compose.onNodeWithContentDescription("Strength").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "50%"))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.8f) }

        assertEquals("strength=0.8", calls.last())
    }

    @Test
    fun a_player_that_cannot_be_heard_is_explained() {
        show(SonoraSettings.Default.copy(beatOverride = true), audioAccess = true, playerSilent = true)

        compose.onNodeWithText("Sonora can't hear this Player").performScrollTo().assertIsDisplayed()
    }
}
