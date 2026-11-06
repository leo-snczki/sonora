package dev.leosnczki.sonora.core.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.leosnczki.sonora.core.domain.ArtId
import dev.leosnczki.sonora.core.domain.MediaSessionSnapshot
import dev.leosnczki.sonora.core.domain.NowPlaying
import dev.leosnczki.sonora.core.domain.NowPlayingTracker
import dev.leosnczki.sonora.core.domain.Track
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A monotonic clock in milliseconds, shared by everything that feeds the core. */
fun monotonicMillis(): Long = System.nanoTime() / 1_000_000

/**
 * Process-wide source of Now Playing (ADR-0002): reads the system's active Media Sessions,
 * plus any session carried by a notification, and lets [NowPlayingTracker] choose one.
 * Every wallpaper engine and the settings screen observe the same [nowPlaying].
 * All work happens on the main thread.
 */
@Singleton
class NowPlayingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artStore: AlbumArtStore,
) {
    private val main = Handler(Looper.getMainLooper())
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, NowPlayingListenerService::class.java)
    private val tracker = NowPlayingTracker()

    private val controllers = mutableMapOf<MediaSession.Token, MediaController>()
    private val fallbackTokens = mutableSetOf<MediaSession.Token>()
    private val callbacks = mutableMapOf<MediaSession.Token, MediaController.Callback>()
    private var listening = false

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    /** True once the user granted notification access. */
    fun hasAccess(): Boolean = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Sessions the system reports as active, and sessions only seen through a notification. */
    private var systemSessions: List<MediaController> = emptyList()

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        systemSessions = list.orEmpty()
        sync()
    }

    /** Called when the listener connects: from here on, sessions can be read. */
    fun start() {
        main.post {
            if (listening) return@post
            try {
                sessionManager.addOnActiveSessionsChangedListener(sessionsChanged, listenerComponent, main)
                listening = true
                systemSessions = sessionManager.getActiveSessions(listenerComponent)
            } catch (e: SecurityException) {
                Log.w(TAG, "no notification access yet", e)
            }
            sync()
        }
    }

    fun stop() {
        main.post {
            if (listening) sessionManager.removeOnActiveSessionsChangedListener(sessionsChanged)
            listening = false
            systemSessions = emptyList()
            fallbackTokens.clear()
            sync()
        }
    }

    /** A notification carried a Media Session the session list may not include (ADR-0002 fallback). */
    fun addSessionFromNotification(token: MediaSession.Token) {
        main.post { if (fallbackTokens.add(token)) sync() }
    }

    fun removeSessionFromNotification(token: MediaSession.Token) {
        main.post { if (fallbackTokens.remove(token)) sync() }
    }

    /** Keeps one registered controller per wanted session, then re-reads them all. */
    private fun sync() {
        val wanted = systemSessions.associateBy { it.sessionToken }.toMutableMap()
        fallbackTokens.forEach { token -> if (token !in wanted) wanted[token] = controllers[token] ?: MediaController(context, token) }

        (controllers.keys - wanted.keys).forEach { token ->
            callbacks.remove(token)?.let { controllers[token]?.unregisterCallback(it) }
            controllers.remove(token)
        }
        wanted.forEach { (token, controller) ->
            if (token in controllers) return@forEach
            controllers[token] = controller
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
                override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
                override fun onSessionDestroyed() {
                    fallbackTokens.remove(token)
                    systemSessions = systemSessions.filter { it.sessionToken != token }
                    sync()
                }
            }
            callbacks[token] = callback
            controller.registerCallback(callback, main)
        }
        refresh()
    }

    private fun refresh() {
        val snapshots = controllers.values.map { it.snapshot() }
        val now = monotonicMillis()
        tracker.update(snapshots, now)
        publish(now)
        // Re-check once the pause grace may have run out, so "stopped" is published on time.
        main.removeCallbacks(regrace)
        if (snapshots.any { !it.playing }) main.postDelayed(regrace, NowPlayingTracker.PAUSE_GRACE_MILLIS + 50)
    }

    private val regrace = Runnable { publish(monotonicMillis()) }

    private fun publish(now: Long) {
        _nowPlaying.value = tracker.nowPlaying(now)
    }

    private fun MediaController.snapshot(): MediaSessionSnapshot {
        val metadata = metadata
        val art = metadata?.albumArt()?.let { artStore.idOf(it) }
        return MediaSessionSnapshot(
            id = sessionToken.hashCode().toString(),
            player = packageName,
            playing = playbackState?.state in PLAYING_STATES,
            track = metadata?.let {
                Track(
                    title = it.getString(MediaMetadata.METADATA_KEY_TITLE),
                    artist = it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: it.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
                    album = it.getString(MediaMetadata.METADATA_KEY_ALBUM),
                )
            },
            art = art,
        )
    }

    private fun MediaMetadata.albumArt(): Bitmap? =
        getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

    /** The Album Art for [id], downscaled for the renderer, or null if it is gone. Call off the main thread. */
    fun art(id: ArtId): Bitmap? = artStore.scaled(id)

    private companion object {
        const val TAG = "SonoraNowPlaying"

        // Buffering and skipping are moments inside playback, not pauses.
        val PLAYING_STATES = setOf(
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
        )
    }
}
