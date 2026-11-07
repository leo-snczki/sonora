package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import dev.leosnczki.sonora.core.domain.GraphicsEffect
import dev.leosnczki.sonora.core.domain.SonoraSettings
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GraphicsMiscGroupsTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = object : HomeActions {
        override fun setGraphicsOverride(enabled: Boolean) { calls += "graphics=$enabled" }
        override fun setGraphicsEffect(effect: GraphicsEffect) { calls += "effect=$effect" }
        override fun setMiscOverride(enabled: Boolean) { calls += "misc=$enabled" }
        override fun setBrightness(brightness: Float) { calls += "brightness=$brightness" }
    }

    private fun show(settings: SonoraSettings) = compose.setContent {
        HomeScreen(HomeScreenState.Preview.copy(setup = null, settings = settings), actions)
    }

    @Test
    fun with_the_groups_off_their_controls_are_hidden_and_their_switches_turn_them_on() {
        show(SonoraSettings.Default)

        compose.onNodeWithText("Fluted glass", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Brightness").assertDoesNotExist()
        compose.onNodeWithText("Graphics").performScrollTo().performClick()
        compose.onNodeWithText("Misc").performScrollTo().performClick()

        assertEquals(listOf("graphics=true", "misc=true"), calls)
    }

    @Test
    fun the_effect_is_a_radio_group() {
        show(SonoraSettings.Default.copy(graphicsOverride = true, graphicsEffect = GraphicsEffect.Blur))

        compose.onNodeWithText("Blur", ignoreCase = true).performScrollTo()
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose.onNodeWithText("Fluted glass", ignoreCase = true).performClick()

        assertEquals(listOf("effect=FlutedGlass"), calls)
    }

    @Test
    fun brightness_speaks_as_a_signed_percentage_and_changes_the_setting() {
        show(SonoraSettings.Default.copy(miscOverride = true, brightness = -0.4f))

        compose.onNodeWithContentDescription("Brightness").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "−40%"))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }

        assertEquals("brightness=0.5", calls.last())
    }
}
