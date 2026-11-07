package dev.leosnczki.sonora.core.domain

/** Names one Backdrop, built in or saved by the user. */
@JvmInline
value class BackdropId(val value: String)

/** Where a Backdrop the user saved came from. */
enum class BackdropSource { AlbumArt, Upload }

/** A Backdrop the user saved: its image lives in private storage at [imagePath] (relative). */
data class SavedBackdrop(
    val id: BackdropId,
    val name: String,
    val source: BackdropSource,
    val imagePath: String,
    val createdAt: Long,
)

/** A Backdrop that ships with Sonora: a diagonal gradient of two to four colours (ARGB). */
data class BuiltinBackdrop(val id: BackdropId, val colors: List<Int>)

/** Sonora's own built-in Backdrops; the first is the default. */
object Backdrops {
    val Builtin = listOf(
        BuiltinBackdrop(BackdropId("builtin:dusk"), listOf(0xFF2B1B4A.toInt(), 0xFF7A3E6B.toInt(), 0xFFE0866A.toInt())),
        BuiltinBackdrop(BackdropId("builtin:lagoon"), listOf(0xFF0B3D4F.toInt(), 0xFF1D8A8A.toInt(), 0xFF9AD9C4.toInt())),
        BuiltinBackdrop(BackdropId("builtin:ember"), listOf(0xFF3A0A0A.toInt(), 0xFFB23A1E.toInt(), 0xFFF2A541.toInt())),
        BuiltinBackdrop(BackdropId("builtin:moss"), listOf(0xFF1E2B1A.toInt(), 0xFF5E7D3A.toInt())),
        BuiltinBackdrop(BackdropId("builtin:orchid"), listOf(0xFF3B1F5C.toInt(), 0xFFB55BA8.toInt(), 0xFFF3B8D4.toInt(), 0xFFFFE6D6.toInt())),
        BuiltinBackdrop(BackdropId("builtin:glacier"), listOf(0xFF1B2E4B.toInt(), 0xFF6F9BC9.toInt(), 0xFFE4EEF7.toInt())),
        BuiltinBackdrop(BackdropId("builtin:citrus"), listOf(0xFF2F5D2A.toInt(), 0xFFC6D343.toInt(), 0xFFFFD25A.toInt())),
        BuiltinBackdrop(BackdropId("builtin:graphite"), listOf(0xFF151515.toInt(), 0xFF4A4A4F.toInt())),
    )

    val Default = Builtin.first().id

    fun builtin(id: BackdropId): BuiltinBackdrop? = Builtin.firstOrNull { it.id == id }

    /** The built-in Backdrop to draw for [id]: an unknown one falls back to the first. */
    fun resolve(id: BackdropId): BuiltinBackdrop = builtin(id) ?: Builtin.first()

    /** Built-in ids read builtin:<key>; saved ones are UUIDs, so the two never collide. */
    fun isBuiltin(id: BackdropId) = id.value.startsWith("builtin:")
}

/** What the wallpaper draws: some Album Art, or a Backdrop. */
sealed interface Picture {
    data class Art(val id: ArtId) : Picture

    data class Backdrop(val id: BackdropId) : Picture
}

/** The Album Art group: whether the last Album Art stays once the music stops, and across a reboot. */
data class AlbumArtKeep(val afterPause: Boolean, val afterReboot: Boolean) {
    companion object {
        /** As in the original: neither. */
        val Default = AlbumArtKeep(afterPause = false, afterReboot = false)
    }
}

/**
 * What to draw for [nowPlaying]: its Album Art while it plays or is within the pause grace, and
 * after it stops too if [keep] says so, even once the Player's session is gone ([lastArt], this
 * run's last Album Art); before any music, the [savedArt] from the last run if kept after reboot;
 * otherwise the [backdrop].
 */
fun pictureFor(nowPlaying: NowPlaying?, keep: AlbumArtKeep, lastArt: ArtId?, savedArt: ArtId?, backdrop: BackdropId): Picture {
    val art = when {
        nowPlaying == null -> lastArt.takeIf { keep.afterPause } ?: savedArt.takeIf { keep.afterReboot }
        nowPlaying.stopped && !keep.afterPause -> null
        else -> nowPlaying.art
    }
    return if (art != null) Picture.Art(art) else Picture.Backdrop(backdrop)
}
