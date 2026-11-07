package dev.leosnczki.sonora.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.leosnczki.sonora.core.domain.AlbumArtKeep
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.CoverOrientation
import dev.leosnczki.sonora.core.domain.FluidStyle
import dev.leosnczki.sonora.core.domain.GraphicsEffect
import dev.leosnczki.sonora.core.domain.LookSettings
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

    @Test
    fun the_fluid_style_survives_a_restart() = runTest(UnconfinedTestDispatcher()) {
        val file = File(folder.root, "style.preferences_pb")
        val firstRun = Job()
        repository(CoroutineScope(coroutineContext + firstRun), file).setFluidStyle(FluidStyle.Cover)
        firstRun.cancelAndJoin()

        assertEquals(FluidStyle.Cover, repository(backgroundScope, file).settings.first().fluidStyle)
    }

    @Test
    fun the_cover_orientation_survives_a_restart() = runTest(UnconfinedTestDispatcher()) {
        val file = File(folder.root, "orientation.preferences_pb")
        val firstRun = Job()
        repository(CoroutineScope(coroutineContext + firstRun), file).apply {
            setCoverFlipVertical(true)
            setCoverMirrorHorizontal(true)
        }
        firstRun.cancelAndJoin()

        assertEquals(
            CoverOrientation(flipVertical = true, mirrorHorizontal = true),
            repository(backgroundScope, file).settings.first().coverOrientation,
        )
    }

    @Test
    fun the_graphics_and_misc_groups_survive_a_restart() = runTest(UnconfinedTestDispatcher()) {
        val file = File(folder.root, "look.preferences_pb")
        val firstRun = Job()
        repository(CoroutineScope(coroutineContext + firstRun), file).apply {
            setGraphicsOverride(true)
            setGraphicsEffect(GraphicsEffect.FlutedGlass)
            setMiscOverride(true)
            setBrightness(-0.3f)
        }
        firstRun.cancelAndJoin()

        val restored = repository(backgroundScope, file).settings.first()
        assertEquals(LookSettings(GraphicsEffect.FlutedGlass, -0.3f), restored.effectiveLook)
    }

    @Test
    fun the_beat_pulse_group_survives_a_restart() = runTest(UnconfinedTestDispatcher()) {
        val file = File(folder.root, "beat.preferences_pb")
        val firstRun = Job()
        repository(CoroutineScope(coroutineContext + firstRun), file).apply {
            setBeatOverride(true)
            setBeatStrength(0.7f)
        }
        firstRun.cancelAndJoin()

        assertEquals(0.7f, repository(backgroundScope, file).settings.first().effectiveBeatStrength)
    }

    @Test
    fun the_album_art_group_and_the_chosen_backdrop_survive_a_restart() = runTest(UnconfinedTestDispatcher()) {
        val file = File(folder.root, "album.preferences_pb")
        val firstRun = Job()
        repository(CoroutineScope(coroutineContext + firstRun), file).apply {
            setAlbumArtOverride(true)
            setKeepAfterPause(true)
            setKeepAfterReboot(true)
            setBackdrop(BackdropId("builtin:lagoon"))
        }
        firstRun.cancelAndJoin()

        val restored = repository(backgroundScope, file).settings.first()
        assertEquals(AlbumArtKeep(afterPause = true, afterReboot = true), restored.effectiveAlbumArtKeep)
        assertEquals(BackdropId("builtin:lagoon"), restored.backdrop)
    }

    @Test
    fun brightness_is_clamped_to_minus_one_to_one() = runTest {
        val repo = repository(backgroundScope, File(folder.root, "brightness.preferences_pb"))

        repo.setBrightness(1.7f)
        assertEquals(1f, repo.settings.first().brightness)
    }

    @Test
    fun an_out_of_range_speed_range_is_clamped_never_inverted() = runTest {
        val repo = repository(backgroundScope, File(folder.root, "clamp.preferences_pb"))

        repo.setSpeedRange(1.2f, 1f)
        repo.settings.first().let { assertEquals(1f to 1f, it.speedMin to it.speedMax) }

        repo.setSpeedRange(-0.5f, -0.2f)
        repo.settings.first().let { assertEquals(0f to 0f, it.speedMin to it.speedMax) }
    }
}
