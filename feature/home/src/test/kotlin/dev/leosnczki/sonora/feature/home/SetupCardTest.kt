package dev.leosnczki.sonora.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun offers_to_set_the_wallpaper_and_opens_the_system_picker() {
        var opened = false
        val actions = object : HomeActions {
            override fun setWallpaper() {
                opened = true
            }
        }
        compose.setContent { HomeScreen(HomeScreenState.Preview.copy(wallpaperSet = false), actions) }

        compose.onNodeWithText("Set wallpaper", ignoreCase = true, substring = false).performScrollTo().performClick()

        assertTrue(opened)
    }

    @Test
    fun offers_notification_access_and_opens_the_system_setting() {
        var opened = false
        val actions = object : HomeActions {
            override fun allowNotificationAccess() {
                opened = true
            }
        }
        compose.setContent { HomeScreen(HomeScreenState.Preview.copy(notificationAccess = false), actions) }

        compose.onNodeWithText("Allow access", ignoreCase = true).performScrollTo().performClick()

        assertTrue(opened)
    }
}
