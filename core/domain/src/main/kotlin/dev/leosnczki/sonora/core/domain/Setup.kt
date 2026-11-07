package dev.leosnczki.sonora.core.domain

enum class SetupStepKind { Wallpaper, NotificationAccess, AudioAccess }

data class SetupStatus(val wallpaperSet: Boolean, val notificationAccess: Boolean, val audioAccess: Boolean) {
    val requiredDone: Boolean get() = wallpaperSet && notificationAccess
}

data class SetupStepState(val kind: SetupStepKind, val done: Boolean, val optional: Boolean)

/**
 * The steps the setup card lists, or null when the card should be hidden. The first time, every
 * step shows with its progress; once setup was completed, only a required step that went missing
 * (a revoked permission, another wallpaper) comes back.
 */
fun setupSteps(status: SetupStatus, completedOnce: Boolean): List<SetupStepState>? {
    if (status.requiredDone) return null
    val all = listOf(
        SetupStepState(SetupStepKind.Wallpaper, status.wallpaperSet, optional = false),
        SetupStepState(SetupStepKind.NotificationAccess, status.notificationAccess, optional = false),
        SetupStepState(SetupStepKind.AudioAccess, status.audioAccess, optional = true),
    )
    return if (completedOnce) all.filter { !it.done && !it.optional } else all
}

/** Manufacturers known to kill background services such as Sonora's listener. */
private val aggressiveManufacturers = setOf("xiaomi", "samsung", "huawei", "oppo", "oneplus", "realme", "vivo")

fun shouldShowBatteryHint(manufacturer: String, exemptFromBatteryOptimisation: Boolean): Boolean =
    !exemptFromBatteryOptimisation && manufacturer.lowercase() in aggressiveManufacturers
