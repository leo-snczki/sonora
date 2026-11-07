package dev.leosnczki.sonora.core.domain

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

/** ADR-0002: a notification counts if it carries a Media Session, whatever its flags. */
class MediaNotificationTest {
    @Test
    fun a_clearable_notification_that_is_not_ongoing_is_accepted_when_it_carries_a_media_session() {
        // YouTube Music ReVanced and the Android 16 behaviour post media notifications like this.
        assertTrue(MediaNotification(hasMediaSession = true, ongoing = false, clearable = true).isPlayerNotification)
    }

    @Test
    fun an_ongoing_media_notification_is_accepted() {
        assertTrue(MediaNotification(hasMediaSession = true, ongoing = true, clearable = false).isPlayerNotification)
    }

    @Test
    fun a_notification_without_a_media_session_is_ignored_even_if_ongoing() {
        assertFalse(MediaNotification(hasMediaSession = false, ongoing = true, clearable = false).isPlayerNotification)
    }
}
