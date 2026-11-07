package dev.leosnczki.sonora.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.leosnczki.sonora.core.domain.SonoraSettings
import java.io.File
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun repository(scope: CoroutineScope, file: File) =
        SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })

    @Test
    fun a_fresh_install_starts_from_the_defaults() = runTest {
        val repo = repository(backgroundScope, folder.newFile("fresh.preferences_pb").also { it.delete() })

        assertEquals(SonoraSettings.Default, repo.settings.first())
    }

    @Test
    fun fluid_settings_survive_a_restart() = runTest(UnconfinedTestDispatcher()) {
        val file = File(folder.root, "settings.preferences_pb")
        val firstRun = Job()
        repository(CoroutineScope(coroutineContext + firstRun), file).apply {
            setFluidOverride(true)
            setFluidScale(0.8f)
            setSpeedRange(0.2f, 0.9f)
            markSetupCompleted()
        }
        firstRun.cancelAndJoin() // the app process ends

        val restored = repository(backgroundScope, file).settings.first()

        assertEquals(SonoraSettings.Default.copy(fluidOverride = true, fluidScale = 0.8f, speedMin = 0.2f, speedMax = 0.9f, setupCompletedOnce = true), restored)
    }
}
