package dev.leosnczki.sonora.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.CoverOrientation
import dev.leosnczki.sonora.core.domain.FluidStyle
import dev.leosnczki.sonora.core.domain.GraphicsEffect
import dev.leosnczki.sonora.core.domain.SonoraSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The user's settings, persisted. The wallpaper and the settings screen both observe [settings]. */
@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {

    val settings: Flow<SonoraSettings> = store.data.map { it.toSettings() }.distinctUntilChanged()

    suspend fun setFluidStyle(style: FluidStyle) = write { it[FLUID_STYLE] = style.name }

    suspend fun setCoverFlipVertical(enabled: Boolean) = write { it[COVER_FLIP_VERTICAL] = enabled }

    suspend fun setCoverMirrorHorizontal(enabled: Boolean) = write { it[COVER_MIRROR_HORIZONTAL] = enabled }

    suspend fun setFluidOverride(enabled: Boolean) = write { it[FLUID_OVERRIDE] = enabled }

    suspend fun setFluidScale(scale: Float) = write { it[FLUID_SCALE] = scale.coerceIn(0f, 1f) }

    suspend fun setSpeedRange(min: Float, max: Float) = write {
        val low = min.coerceIn(0f, 1f)
        it[SPEED_MIN] = low
        it[SPEED_MAX] = max.coerceIn(low, 1f)
    }

    suspend fun setGraphicsOverride(enabled: Boolean) = write { it[GRAPHICS_OVERRIDE] = enabled }

    suspend fun setGraphicsEffect(effect: GraphicsEffect) = write { it[GRAPHICS_EFFECT] = effect.name }

    suspend fun setMiscOverride(enabled: Boolean) = write { it[MISC_OVERRIDE] = enabled }

    suspend fun setBrightness(brightness: Float) = write { it[BRIGHTNESS] = brightness.coerceIn(-1f, 1f) }

    suspend fun setBeatOverride(enabled: Boolean) = write { it[BEAT_OVERRIDE] = enabled }

    suspend fun setBeatStrength(strength: Float) = write { it[BEAT_STRENGTH] = strength.coerceIn(0f, 1f) }

    suspend fun setAlbumArtOverride(enabled: Boolean) = write { it[ALBUM_ART_OVERRIDE] = enabled }

    suspend fun setKeepAfterPause(enabled: Boolean) = write { it[KEEP_AFTER_PAUSE] = enabled }

    suspend fun setKeepAfterReboot(enabled: Boolean) = write { it[KEEP_AFTER_REBOOT] = enabled }

    suspend fun setBackdrop(backdrop: BackdropId) = write { it[BACKDROP] = backdrop.value }

    suspend fun markSetupCompleted() = write { it[SETUP_COMPLETED_ONCE] = true }

    private suspend fun write(change: (MutablePreferences) -> Unit) {
        store.edit(change)
    }

    private fun Preferences.toSettings(): SonoraSettings {
        val defaults = SonoraSettings.Default
        return SonoraSettings(
            // Stored by name; an unknown name (a style since removed) falls back to the default.
            fluidStyle = FluidStyle.entries.firstOrNull { it.name == this[FLUID_STYLE] } ?: defaults.fluidStyle,
            coverOrientation = CoverOrientation(
                flipVertical = this[COVER_FLIP_VERTICAL] ?: defaults.coverOrientation.flipVertical,
                mirrorHorizontal = this[COVER_MIRROR_HORIZONTAL] ?: defaults.coverOrientation.mirrorHorizontal,
            ),
            fluidOverride = this[FLUID_OVERRIDE] ?: defaults.fluidOverride,
            fluidScale = this[FLUID_SCALE] ?: defaults.fluidScale,
            speedMin = this[SPEED_MIN] ?: defaults.speedMin,
            speedMax = this[SPEED_MAX] ?: defaults.speedMax,
            graphicsOverride = this[GRAPHICS_OVERRIDE] ?: defaults.graphicsOverride,
            graphicsEffect = GraphicsEffect.entries.firstOrNull { it.name == this[GRAPHICS_EFFECT] } ?: defaults.graphicsEffect,
            miscOverride = this[MISC_OVERRIDE] ?: defaults.miscOverride,
            brightness = this[BRIGHTNESS] ?: defaults.brightness,
            beatOverride = this[BEAT_OVERRIDE] ?: defaults.beatOverride,
            beatStrength = this[BEAT_STRENGTH] ?: defaults.beatStrength,
            albumArtOverride = this[ALBUM_ART_OVERRIDE] ?: defaults.albumArtOverride,
            keepAfterPause = this[KEEP_AFTER_PAUSE] ?: defaults.keepAfterPause,
            keepAfterReboot = this[KEEP_AFTER_REBOOT] ?: defaults.keepAfterReboot,
            backdrop = this[BACKDROP]?.let(::BackdropId) ?: defaults.backdrop,
            setupCompletedOnce = this[SETUP_COMPLETED_ONCE] ?: defaults.setupCompletedOnce,
        )
    }

    private companion object {
        val FLUID_STYLE = stringPreferencesKey("fluid_style")
        val COVER_FLIP_VERTICAL = booleanPreferencesKey("cover_flip_vertical")
        val COVER_MIRROR_HORIZONTAL = booleanPreferencesKey("cover_mirror_horizontal")
        val FLUID_OVERRIDE = booleanPreferencesKey("fluid_override")
        val FLUID_SCALE = floatPreferencesKey("fluid_scale")
        val SPEED_MIN = floatPreferencesKey("speed_min")
        val SPEED_MAX = floatPreferencesKey("speed_max")
        val GRAPHICS_OVERRIDE = booleanPreferencesKey("graphics_override")
        val GRAPHICS_EFFECT = stringPreferencesKey("graphics_effect")
        val MISC_OVERRIDE = booleanPreferencesKey("misc_override")
        val BRIGHTNESS = floatPreferencesKey("brightness")
        val BEAT_OVERRIDE = booleanPreferencesKey("beat_override")
        val BEAT_STRENGTH = floatPreferencesKey("beat_strength")
        val ALBUM_ART_OVERRIDE = booleanPreferencesKey("album_art_override")
        val KEEP_AFTER_PAUSE = booleanPreferencesKey("keep_after_pause")
        val KEEP_AFTER_REBOOT = booleanPreferencesKey("keep_after_reboot")
        val BACKDROP = stringPreferencesKey("backdrop")
        val SETUP_COMPLETED_ONCE = booleanPreferencesKey("setup_completed_once")
    }
}
