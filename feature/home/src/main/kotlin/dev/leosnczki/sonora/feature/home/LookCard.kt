package dev.leosnczki.sonora.feature.home

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.LabeledRangeSlider
import dev.leosnczki.sonora.core.designsystem.component.LabeledSlider
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SegmentedChoice
import dev.leosnczki.sonora.core.designsystem.component.SettingGroup
import dev.leosnczki.sonora.core.designsystem.component.SwitchRow
import dev.leosnczki.sonora.core.domain.FluidStyle
import dev.leosnczki.sonora.core.domain.SonoraSettings
import kotlin.math.roundToInt

/** The settings groups. Each change applies live to the wallpaper behind the screen. */
@Composable
internal fun LookCard(settings: SonoraSettings, actions: HomeActions) {
    SectionCard(stringResource(R.string.look_title)) {
        FluidStyleChoice(settings.fluidStyle, actions)
        if (settings.fluidStyle == FluidStyle.Cover) CoverOrientationSwitches(settings, actions)
        Spacer(Modifier.height(SonoraSpace.Md))
        FluidGroup(settings, actions)
    }
}

/** Outside the Motion group's override: the style is a choice of look, not fine-tuning. */
@Composable
private fun FluidStyleChoice(style: FluidStyle, actions: HomeActions) {
    Text(stringResource(R.string.fluid_style), style = SonoraType.BodyStrong, color = SonoraColors.Text)
    Text(stringResource(R.string.fluid_style_desc), style = SonoraType.Caption, color = SonoraColors.TextMuted)
    Spacer(Modifier.height(SonoraSpace.Sm))
    val labels = mapOf(
        FluidStyle.Flow to stringResource(R.string.fluid_style_flow),
        FluidStyle.Cover to stringResource(R.string.fluid_style_cover),
    )
    SegmentedChoice(
        options = FluidStyle.entries,
        selected = style,
        onSelect = actions::setFluidStyle,
        label = { labels.getValue(it) },
    )
}

/** Only Cover keeps the image recognisable, so only Cover can turn it. */
@Composable
private fun CoverOrientationSwitches(settings: SonoraSettings, actions: HomeActions) {
    Spacer(Modifier.height(SonoraSpace.Sm))
    SwitchRow(
        label = stringResource(R.string.cover_flip_vertical),
        supporting = stringResource(R.string.cover_flip_vertical_desc),
        checked = settings.coverOrientation.flipVertical,
        onCheckedChange = actions::setCoverFlipVertical,
    )
    SwitchRow(
        label = stringResource(R.string.cover_mirror_horizontal),
        supporting = stringResource(R.string.cover_mirror_horizontal_desc),
        checked = settings.coverOrientation.mirrorHorizontal,
        onCheckedChange = actions::setCoverMirrorHorizontal,
    )
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
