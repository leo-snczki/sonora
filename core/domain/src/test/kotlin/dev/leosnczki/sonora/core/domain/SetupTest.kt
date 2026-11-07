package dev.leosnczki.sonora.core.domain

import dev.leosnczki.sonora.core.domain.SetupStepKind.AudioAccess
import dev.leosnczki.sonora.core.domain.SetupStepKind.NotificationAccess
import dev.leosnczki.sonora.core.domain.SetupStepKind.Wallpaper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class SetupTest {
    private val nothing = SetupStatus(wallpaperSet = false, notificationAccess = false, audioAccess = false)

    @Test
    fun first_run_lists_every_step_with_audio_as_optional() {
        val steps = setupSteps(nothing, completedOnce = false)

        assertEquals(listOf(Wallpaper, NotificationAccess, AudioAccess), steps?.map { it.kind })
        assertEquals(listOf(false, false, true), steps?.map { it.optional })
    }

    @Test
    fun finished_steps_stay_listed_as_done_until_setup_is_complete() {
        val steps = setupSteps(nothing.copy(wallpaperSet = true), completedOnce = false)

        assertEquals(listOf(true, false, false), steps?.map { it.done })
    }

    @Test
    fun the_card_hides_once_the_required_steps_are_done_even_without_audio() {
        assertNull(setupSteps(nothing.copy(wallpaperSet = true, notificationAccess = true), completedOnce = false))
    }

    @Test
    fun after_setup_was_completed_once_only_the_missing_required_step_comes_back() {
        val revoked = SetupStatus(wallpaperSet = true, notificationAccess = false, audioAccess = false)

        assertEquals(listOf(NotificationAccess), setupSteps(revoked, completedOnce = true)?.map { it.kind })
    }

    @Test
    fun the_battery_hint_is_for_the_known_aggressive_manufacturers_only() {
        listOf("Xiaomi", "samsung", "HUAWEI", "OPPO", "OnePlus", "realme", "vivo").forEach {
            assertTrue(shouldShowBatteryHint(manufacturer = it, exemptFromBatteryOptimisation = false), it)
        }
        assertFalse(shouldShowBatteryHint(manufacturer = "Google", exemptFromBatteryOptimisation = false))
        assertFalse(shouldShowBatteryHint(manufacturer = "LENOVO", exemptFromBatteryOptimisation = false))
    }

    @Test
    fun the_battery_hint_goes_away_once_sonora_is_exempt() {
        assertFalse(shouldShowBatteryHint(manufacturer = "Xiaomi", exemptFromBatteryOptimisation = true))
    }
}
