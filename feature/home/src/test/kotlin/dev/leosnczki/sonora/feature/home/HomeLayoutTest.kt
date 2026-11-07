package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class HomeLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    private fun leftOf(text: String): Dp =
        compose.onNodeWithText(text, ignoreCase = true).getUnclippedBoundsInRoot().left

    @Test
    @Config(qualifiers = "w390dp-h800dp")
    fun phone_width_stacks_every_card_in_one_column() {
        compose.setContent { HomeScreen(HomeScreenState.Preview, HomeActions.None) }

        assertEquals(leftOf("Now playing"), leftOf("Help"))
        assertEquals(leftOf("Now playing"), leftOf("About"))
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun wide_screen_puts_help_and_about_in_a_second_column() {
        compose.setContent { HomeScreen(HomeScreenState.Preview, HomeActions.None) }

        assertTrue(leftOf("Help") > leftOf("Now playing"))
        assertEquals(leftOf("Help"), leftOf("About"))
    }
}
