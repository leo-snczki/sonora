package dev.leosnczki.sonora.core.media

import android.app.Notification
import android.media.session.MediaSession
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.os.BundleCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.leosnczki.sonora.core.domain.MediaNotification
import javax.inject.Inject

/**
 * Exists for notification access: while it is connected, Sonora may read Media Sessions.
 * Notifications are only used to find sessions; their flags are ignored (ADR-0002).
 */
@AndroidEntryPoint
class NowPlayingListenerService : NotificationListenerService() {
    @Inject lateinit var repository: NowPlayingRepository

    override fun onListenerConnected() {
        repository.start()
        activeNotifications?.forEach(::onNotificationPosted)
    }

    override fun onListenerDisconnected() = repository.stop()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val token = sbn.mediaSessionToken()
        val notification = MediaNotification(hasMediaSession = token != null, ongoing = sbn.isOngoing, clearable = sbn.isClearable)
        if (notification.isPlayerNotification && token != null) repository.addSessionFromNotification(token)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        sbn.mediaSessionToken()?.let(repository::removeSessionFromNotification)
    }

    private fun StatusBarNotification.mediaSessionToken(): MediaSession.Token? =
        notification?.extras?.let { BundleCompat.getParcelable(it, Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java) }
}
