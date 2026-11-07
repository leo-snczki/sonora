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
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.LabeledRangeSlider
import dev.leosnczki.sonora.core.designsystem.component.LabeledSlider
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SegmentedChoice
import dev.leosnczki.sonora.core.designsystem.component.SettingGroup
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton
import dev.leosnczki.sonora.core.designsystem.component.SwitchRow
import dev.leosnczki.sonora.core.domain.FluidStyle
import dev.leosnczki.sonora.core.domain.GraphicsEffect
import dev.leosnczki.sonora.core.domain.SonoraSettings
import kotlin.math.roundToInt

/** The settings groups. Each change applies live to the wallpaper behind the screen. */
@Composable
internal fun LookCard(state: HomeScreenState, actions: HomeActions) {
    val settings = state.settings
    SectionCard(stringResource(R.string.look_title)) {
        FluidStyleChoice(settings.fluidStyle, actions)
        if (settings.fluidStyle == FluidStyle.Cover) CoverOrientationSwitches(settings, actions)
        Spacer(Modifier.height(SonoraSpace.Md))
        FluidGroup(settings, actions)
        Spacer(Modifier.height(SonoraSpace.Md))
        GraphicsGroup(settings, actions)
        Spacer(Modifier.height(SonoraSpace.Md))
        BeatPulseGroup(state, actions)
        Spacer(Modifier.height(SonoraSpace.Md))
        AlbumArtGroup(settings, actions)
        // Outside the group's override: choosing the Backdrop is a choice of look, not fine-tuning.
        SonoraButton(stringResource(R.string.backdrop_library), actions::openBackdropLibrary, variant = ButtonVariant.Secondary)
        Spacer(Modifier.height(SonoraSpace.Md))
        MiscGroup(settings, actions)
    }
}

@Composable
private fun GraphicsGroup(settings: SonoraSettings, actions: HomeActions) {
    val labels = mapOf(
        GraphicsEffect.Blur to stringResource(R.string.graphics_blur),
        GraphicsEffect.FlutedGlass to stringResource(R.string.graphics_fluted_glass),
    )
    SettingGroup(
        label = stringResource(R.string.graphics_group),
        supporting = stringResource(R.string.graphics_group_desc),
        checked = settings.graphicsOverride,
        onCheckedChange = actions::setGraphicsOverride,
    ) {
        SegmentedChoice(
            options = GraphicsEffect.entries,
            selected = settings.graphicsEffect,
            onSelect = actions::setGraphicsEffect,
            label = { labels.getValue(it) },
        )
    }
}

/** Locked without audio access, with the reason and the way to grant it: no microphone fallback. */
@Composable
private fun BeatPulseGroup(state: HomeScreenState, actions: HomeActions) {
    val settings = state.settings
    SettingGroup(
        label = stringResource(R.string.beat_group),
        supporting = stringResource(R.string.beat_group_desc),
        checked = settings.beatOverride,
        onCheckedChange = actions::setBeatOverride,
        lockedReason = if (state.audioAccess) null else stringResource(R.string.beat_locked),
        lockedAction = {
            SonoraButton(stringResource(R.string.beat_allow), actions::allowAudioAccess, variant = ButtonVariant.Secondary)
        },
    ) {
        LabeledSlider(
            label = stringResource(R.string.beat_strength),
            supporting = stringResource(R.string.beat_strength_desc),
            value = settings.beatStrength,
            onValueChange = actions::setBeatStrength,
            describe = { "${(it * 100).roundToInt()}%" },
        )
        if (state.playerSilent) {
            Spacer(Modifier.height(SonoraSpace.Xs))
            Text(stringResource(R.string.beat_silent_title), style = SonoraType.BodyStrong, color = SonoraColors.Warning)
            Text(stringResource(R.string.beat_silent_body), style = SonoraType.Caption, color = SonoraColors.TextMuted)
        }
    }
}

@Composable
private fun AlbumArtGroup(settings: SonoraSettings, actions: HomeActions) {
    SettingGroup(
        label = stringResource(R.string.album_art_group),
        supporting = stringResource(R.string.album_art_group_desc),
        checked = settings.albumArtOverride,
        onCheckedChange = actions::setAlbumArtOverride,
    ) {
        SwitchRow(
            label = stringResource(R.string.keep_after_pause),
            supporting = stringResource(R.string.keep_after_pause_desc),
            checked = settings.keepAfterPause,
            onCheckedChange = actions::setKeepAfterPause,
        )
        SwitchRow(
            label = stringResource(R.string.keep_after_reboot),
            supporting = stringResource(R.string.keep_after_reboot_desc),
            checked = settings.keepAfterReboot,
            onCheckedChange = actions::setKeepAfterReboot,
        )
    }
}

@Composable
private fun MiscGroup(settings: SonoraSettings, actions: HomeActions) {
    SettingGroup(
        label = stringResource(R.string.misc_group),
        supporting = stringResource(R.string.misc_group_desc),
        checked = settings.miscOverride,
        onCheckedChange = actions::setMiscOverride,
    ) {
        LabeledSlider(
            label = stringResource(R.string.brightness),
            supporting = stringResource(R.string.brightness_desc),
            value = settings.brightness,
            onValueChange = actions::setBrightness,
            describe = ::signedPercent,
            valueRange = -1f..1f,
            centreTick = true,
        )
    }
}

/** "+20%", "−40%" (a true minus sign), "0%". */
private fun signedPercent(value: Float): String {
    val percent = (value * 100).roundToInt()
    return when {
        percent > 0 -> "+$percent%"
        percent < 0 -> "\u2212${-percent}%"
        else -> "0%"
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
