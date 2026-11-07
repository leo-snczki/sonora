package dev.leosnczki.sonora.core.domain

import kotlin.test.assertEquals
import org.junit.Test

class SonoraSettingsTest {
    private val custom = SonoraSettings.Default.copy(fluidScale = 0.9f, speedMin = 0.3f, speedMax = 0.95f)

    @Test
    fun with_the_fluid_group_off_the_defaults_apply_whatever_was_set() {
        assertEquals(FluidSettings.Default, custom.copy(fluidOverride = false).effectiveFluid)
    }

    @Test
    fun with_the_fluid_group_on_the_users_values_apply() {
        assertEquals(FluidSettings(style = FluidStyle.Flow, scale = 0.9f, speedMin = 0.3f, speedMax = 0.95f), custom.copy(fluidOverride = true).effectiveFluid)
    }

    @Test
    fun a_fresh_install_shows_the_flow_style() {
        assertEquals(FluidStyle.Flow, SonoraSettings.Default.effectiveFluid.style)
    }

    @Test
    fun the_fluid_style_applies_whether_the_fluid_group_is_on_or_off() {
        assertEquals(FluidStyle.Cover, custom.copy(fluidStyle = FluidStyle.Cover, fluidOverride = false).effectiveFluid.style)
        assertEquals(FluidStyle.Cover, custom.copy(fluidStyle = FluidStyle.Cover, fluidOverride = true).effectiveFluid.style)
    }

    @Test
    fun the_director_draws_the_chosen_fluid_style() {
        val director = SceneDirector().apply { setVisible(true) }
        director.setFluid(custom.copy(fluidStyle = FluidStyle.Cover).effectiveFluid)

        assertEquals(FluidStyle.Cover, director.frame(atMillis = 0).style)
    }

    @Test
    fun the_director_uses_new_fluid_settings_straight_away() {
        val director = SceneDirector().apply { setVisible(true) }
        director.onNowPlaying(NowPlaying("p", null, ArtId("a"), playing = true, stopped = false), atMillis = 0)
        director.setFluid(custom.copy(fluidOverride = true).effectiveFluid)

        val frame = director.frame(atMillis = 10_000)
        assertEquals(0.9f, frame.scale, 0.001f)
        assertEquals(0.95f, frame.speed, 0.001f)
    }
}
