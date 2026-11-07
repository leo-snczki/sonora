package dev.leosnczki.sonora.core.domain

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class DetectionWatchTest {
    private val watch = DetectionWatch()

    @Test
    fun hints_after_two_minutes_with_access_and_no_media_session() {
        watch.update(hasAccess = true, sessionSeen = false, atMillis = 0)

        assertFalse(watch.showHint(atMillis = 119_999))
        assertTrue(watch.showHint(atMillis = 120_000))
    }

    @Test
    fun never_hints_without_access() {
        watch.update(hasAccess = false, sessionSeen = false, atMillis = 0)

        assertFalse(watch.showHint(atMillis = 600_000))
    }

    @Test
    fun a_media_session_clears_the_hint_for_good() {
        watch.update(hasAccess = true, sessionSeen = false, atMillis = 0)
        watch.update(hasAccess = true, sessionSeen = true, atMillis = 150_000)
        watch.update(hasAccess = true, sessionSeen = false, atMillis = 160_000)

        assertFalse(watch.showHint(atMillis = 600_000))
    }

    @Test
    fun the_wait_starts_when_access_is_granted() {
        watch.update(hasAccess = false, sessionSeen = false, atMillis = 0)
        watch.update(hasAccess = true, sessionSeen = false, atMillis = 500_000)

        assertFalse(watch.showHint(atMillis = 600_000))
        assertTrue(watch.showHint(atMillis = 620_000))
    }
}
