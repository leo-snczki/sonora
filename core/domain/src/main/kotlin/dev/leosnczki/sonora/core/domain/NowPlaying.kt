package dev.leosnczki.sonora.core.domain

/** Identifies one Album Art image by its content, so the same cover posted twice is the same art. */
@JvmInline
value class ArtId(val value: String)

data class Track(val title: String?, val artist: String?, val album: String?)

/** What a Player's Media Session says at one moment. */
data class MediaSessionSnapshot(
    val id: String,
    val player: String,
    val playing: Boolean,
    val track: Track?,
    val art: ArtId?,
)

/**
 * The Media Session Sonora has chosen to show. It is [stopped] once it has been paused for
 * longer than the grace period, so a short pause between tracks never counts as stopping.
 */
data class NowPlaying(
    val player: String,
    val track: Track?,
    val art: ArtId?,
    val playing: Boolean,
    val stopped: Boolean,
)

/**
 * Chooses Now Playing from the Media Sessions over time: the session that started playing most
 * recently wins; when none is playing, the last one that played, as paused.
 */
class NowPlayingTracker(private val pauseGraceMillis: Long = PAUSE_GRACE_MILLIS) {
    private var sessions: List<MediaSessionSnapshot> = emptyList()
    private val startedPlayingAt = mutableMapOf<String, Long>()
    private val lastPlayingAt = mutableMapOf<String, Long>()

    fun update(sessions: List<MediaSessionSnapshot>, atMillis: Long) {
        val wasPlaying = this.sessions.filter { it.playing }.map { it.id }.toSet()
        sessions.forEach { session ->
            if (session.playing && session.id !in wasPlaying) startedPlayingAt[session.id] = atMillis
            // A session that was playing played until now, even if it has just paused.
            if (session.playing || session.id in wasPlaying) lastPlayingAt[session.id] = atMillis
        }
        this.sessions = sessions
    }

    fun nowPlaying(atMillis: Long): NowPlaying? {
        val chosen = sessions.filter { it.playing }.maxByOrNull { startedPlayingAt[it.id] ?: Long.MIN_VALUE }
            ?: sessions.filter { it.id in lastPlayingAt }.maxByOrNull { lastPlayingAt.getValue(it.id) }
            ?: return null
        val pausedFor = atMillis - (lastPlayingAt[chosen.id] ?: atMillis)
        val stopped = !chosen.playing && pausedFor >= pauseGraceMillis
        return NowPlaying(chosen.player, chosen.track, chosen.art, chosen.playing, stopped)
    }

    companion object {
        const val PAUSE_GRACE_MILLIS = 10_000L
    }
}

/**
 * A posted notification, as far as Now Playing cares (ADR-0002): it belongs to a Player when it
 * carries a Media Session. Its ongoing / clearable flags never matter.
 */
data class MediaNotification(val hasMediaSession: Boolean, val ongoing: Boolean, val clearable: Boolean) {
    val isPlayerNotification: Boolean get() = hasMediaSession
}
