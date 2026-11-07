package dev.leosnczki.sonora.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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

    suspend fun setFluidOverride(enabled: Boolean) = write { it[FLUID_OVERRIDE] = enabled }

    suspend fun setFluidScale(scale: Float) = write { it[FLUID_SCALE] = scale.coerceIn(0f, 1f) }

    suspend fun setSpeedRange(min: Float, max: Float) = write {
        it[SPEED_MIN] = min.coerceIn(0f, 1f)
        it[SPEED_MAX] = max.coerceIn(min, 1f)
    }

    suspend fun markSetupCompleted() = write { it[SETUP_COMPLETED_ONCE] = true }

    private suspend fun write(change: (MutablePreferences) -> Unit) {
        store.edit(change)
    }

    private fun Preferences.toSettings(): SonoraSettings {
        val defaults = SonoraSettings.Default
        return SonoraSettings(
            fluidOverride = this[FLUID_OVERRIDE] ?: defaults.fluidOverride,
            fluidScale = this[FLUID_SCALE] ?: defaults.fluidScale,
            speedMin = this[SPEED_MIN] ?: defaults.speedMin,
            speedMax = this[SPEED_MAX] ?: defaults.speedMax,
            setupCompletedOnce = this[SETUP_COMPLETED_ONCE] ?: defaults.setupCompletedOnce,
        )
    }

    private companion object {
        val FLUID_OVERRIDE = booleanPreferencesKey("fluid_override")
        val FLUID_SCALE = floatPreferencesKey("fluid_scale")
        val SPEED_MIN = floatPreferencesKey("speed_min")
        val SPEED_MAX = floatPreferencesKey("speed_max")
        val SETUP_COMPLETED_ONCE = booleanPreferencesKey("setup_completed_once")
    }
}
