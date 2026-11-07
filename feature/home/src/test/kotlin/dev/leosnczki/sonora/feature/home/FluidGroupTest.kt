package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
class FluidGroupTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = object : HomeActions {
        override fun setFluidOverride(enabled: Boolean) { calls += "override=$enabled" }
        override fun setFluidScale(scale: Float) { calls += "scale=$scale" }
        override fun setSpeedRange(min: Float, max: Float) { calls += "speed=$min..$max" }
    }

    private fun show(settings: SonoraSettings) = compose.setContent {
        HomeScreen(HomeScreenState.Preview.copy(setup = null, settings = settings), actions)
    }

    @Test
    fun with_the_group_off_its_controls_are_hidden_and_the_switch_turns_it_on() {
        show(SonoraSettings.Default.copy(fluidOverride = false))

        compose.onNodeWithContentDescription("Fluid scale").assertDoesNotExist()
        compose.onNodeWithText("Motion").performScrollTo().performClick()

        assertEquals(listOf("override=true"), calls)
    }

    @Test
    fun with_the_group_on_the_sliders_speak_in_words() {
        show(SonoraSettings.Default.copy(fluidOverride = true, fluidScale = 0.9f, speedMin = 0.2f, speedMax = 0.8f))

        compose.onNodeWithContentDescription("Fluid scale").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Turbulent"))
        compose.onNodeWithContentDescription("Speed when paused")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "20%"))
        compose.onNodeWithContentDescription("Speed while playing")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "80%"))
    }

    @Test
    fun moving_the_scale_slider_changes_the_setting() {
        show(SonoraSettings.Default.copy(fluidOverride = true))

        compose.onNodeWithContentDescription("Fluid scale").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }

        assertEquals("scale=0.25", calls.last())
    }
}
