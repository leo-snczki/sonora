package dev.leosnczki.sonora.core.media

import android.graphics.Bitmap
import androidx.core.graphics.scale
import dev.leosnczki.sonora.core.domain.ArtId
import java.security.MessageDigest
import java.util.WeakHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Names Album Art by its content, so a Player re-posting the same cover gives the same [ArtId],
 * and hands the renderer a small copy. Keeps only the last few covers.
 */
@Singleton
class AlbumArtStore @Inject constructor() {
    private val ids = WeakHashMap<Bitmap, ArtId>()
    private val originals = object : LinkedHashMap<ArtId, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ArtId, Bitmap>) = size > KEEP
    }
    private val scaledCopies = object : LinkedHashMap<ArtId, Bitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ArtId, Bitmap>) = size > KEEP
    }

    /** Cheap after the first call for a given bitmap: the hash uses a 16×16 copy. */
    @Synchronized
    fun idOf(bitmap: Bitmap): ArtId {
        ids[bitmap]?.let { originals[it] = bitmap; return it }
        val tiny = bitmap.scale(16, 16, filter = true)
        val pixels = IntArray(16 * 16).also { tiny.getPixels(it, 0, 16, 0, 0, 16, 16) }
        if (tiny !== bitmap) tiny.recycle()
        val digest = MessageDigest.getInstance("SHA-1")
        pixels.forEach { digest.update(byteArrayOf((it shr 16).toByte(), (it shr 8).toByte(), it.toByte())) }
        val id = ArtId(digest.digest().joinToString("") { "%02x".format(it) })
        ids[bitmap] = id
        originals[id] = bitmap
        return id
    }

    /**
     * At most [MAX_SIZE] px on the long side: the blur hides any more detail. The scaling runs
     * outside the lock, so [idOf] on the main thread never waits for it.
     */
    fun scaled(id: ArtId): Bitmap? {
        val original = synchronized(this) {
            scaledCopies[id]?.let { return it }
            originals[id]?.takeUnless { it.isRecycled }
        } ?: return null
        val factor = MAX_SIZE.toFloat() / max(original.width, original.height)
        val copy = if (factor >= 1f) {
            original.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            original.scale((original.width * factor).toInt().coerceAtLeast(1), (original.height * factor).toInt().coerceAtLeast(1), filter = true)
        }
        // GL can only upload software bitmaps; Players may hand over HARDWARE ones.
        val uploadable = if (copy.config == Bitmap.Config.HARDWARE) copy.copy(Bitmap.Config.ARGB_8888, false) else copy
        synchronized(this) { scaledCopies[id] = uploadable }
        return uploadable
    }

    private companion object {
        const val MAX_SIZE = 512
        const val KEEP = 4
    }
}
