package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LicencesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun lists_the_notices_for_the_fonts_icons_and_libraries() {
        compose.setContent { LicencesScreen(onBack = {}) }

        compose.onNodeWithText("Manrope and JetBrains Mono fonts", ignoreCase = true).assertExists()
        compose.onNodeWithText("The Manrope Project Authors", substring = true).assertExists()
        compose.onNodeWithText("JetBrains Mono Project Authors", substring = true).assertExists()
        compose.onNodeWithText("Lucide icons", ignoreCase = true).assertExists()
        compose.onNodeWithText("ISC License", substring = true).assertExists()
        compose.onNodeWithText("Apache License", substring = true).assertExists()
    }

    @Test
    fun back_button_leaves_the_screen() {
        var back = false
        compose.setContent { LicencesScreen(onBack = { back = true }) }

        compose.onNodeWithText("Back", ignoreCase = true).performClick()

        assertTrue(back)
    }
}
