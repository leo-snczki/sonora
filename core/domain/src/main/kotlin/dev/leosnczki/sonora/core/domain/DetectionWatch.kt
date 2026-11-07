package dev.leosnczki.sonora.core.domain

/**
 * Notices when Sonora has notification access but never sees a Media Session, which usually
 * means the listener was killed or the Player is unsupported, so the screen can offer help.
 */
class DetectionWatch(private val timeoutMillis: Long = TIMEOUT_MILLIS) {
    private var accessSince: Long? = null
    private var sessionSeen = false

    fun update(hasAccess: Boolean, sessionSeen: Boolean, atMillis: Long) {
        accessSince = if (hasAccess) accessSince ?: atMillis else null
        this.sessionSeen = this.sessionSeen || sessionSeen
    }

    fun showHint(atMillis: Long): Boolean {
        val since = accessSince ?: return false
        return !sessionSeen && atMillis - since >= timeoutMillis
    }

    companion object {
        const val TIMEOUT_MILLIS = 120_000L
    }
}
