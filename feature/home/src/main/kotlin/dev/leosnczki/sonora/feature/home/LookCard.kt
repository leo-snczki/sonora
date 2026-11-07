package dev.leosnczki.sonora.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.leosnczki.sonora.core.designsystem.component.LabeledRangeSlider
import dev.leosnczki.sonora.core.designsystem.component.LabeledSlider
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SettingGroup
import dev.leosnczki.sonora.core.domain.SonoraSettings
import kotlin.math.roundToInt

/** The settings groups. Each change applies live to the wallpaper behind the screen. */
@Composable
internal fun LookCard(settings: SonoraSettings, actions: HomeActions) {
    SectionCard(stringResource(R.string.look_title)) {
        FluidGroup(settings, actions)
    }
}

@Composable
private fun FluidGroup(settings: SonoraSettings, actions: HomeActions) {
    val scaleWords = listOf(
        stringResource(R.string.fluid_scale_calm),
        stringResource(R.string.fluid_scale_gentle),
        stringResource(R.string.fluid_scale_lively),
        stringResource(R.string.fluid_scale_turbulent),
    )
    SettingGroup(
        label = stringResource(R.string.fluid_group),
        supporting = stringResource(R.string.fluid_group_desc),
        checked = settings.fluidOverride,
        onCheckedChange = actions::setFluidOverride,
    ) {
        LabeledSlider(
            label = stringResource(R.string.fluid_scale),
            supporting = stringResource(R.string.fluid_scale_desc),
            value = settings.fluidScale,
            onValueChange = actions::setFluidScale,
            describe = { scaleWords[(it * scaleWords.size).toInt().coerceIn(0, scaleWords.lastIndex)] },
        )
        LabeledRangeSlider(
            label = stringResource(R.string.fluid_speed),
            supporting = stringResource(R.string.fluid_speed_desc),
            value = settings.speedMin..settings.speedMax,
            onValueChange = { actions.setSpeedRange(it.start, it.endInclusive) },
            describe = { "${(it * 100).roundToInt()}%" },
            startLabel = stringResource(R.string.fluid_speed_paused),
            endLabel = stringResource(R.string.fluid_speed_playing),
        )
    }
}
