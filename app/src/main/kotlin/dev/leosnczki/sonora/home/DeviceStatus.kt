package dev.leosnczki.sonora.home

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.service.media.MediaBrowserService
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.leosnczki.sonora.feature.home.PlayerApp
import javax.inject.Inject

/** What the settings screen needs to know about the device, read fresh each time it resumes. */
class DeviceStatus @Inject constructor(@ApplicationContext private val context: Context) {
    private val pm = context.packageManager

    fun isSonoraTheWallpaper(): Boolean =
        WallpaperManager.getInstance(context).wallpaperInfo?.packageName == context.packageName

    fun appName(packageName: String): String = runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    fun launchIntent(packageName: String): Intent? = pm.getLaunchIntentForPackage(packageName)

    /** Apps that declare themselves music players, or expose a media browser (most streaming apps do). */
    fun installedPlayers(): List<PlayerApp> {
        val activities = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC), 0) +
            @Suppress("DEPRECATION") pm.queryIntentActivities(Intent(android.provider.MediaStore.INTENT_ACTION_MUSIC_PLAYER), 0)
        val services = pm.queryIntentServices(Intent(MediaBrowserService.SERVICE_INTERFACE), 0)
        return (activities + services)
            .mapNotNull { it.ownerPackage }
            .distinct()
            .filter { it != context.packageName && launchIntent(it) != null }
            .map { pkg ->
                val icon = runCatching { pm.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
                PlayerApp(appName(pkg), pkg, icon)
            }
            .sortedBy { it.name.lowercase() }
    }

    private val ResolveInfo.ownerPackage: String?
        get() = activityInfo?.packageName ?: serviceInfo?.packageName
}
