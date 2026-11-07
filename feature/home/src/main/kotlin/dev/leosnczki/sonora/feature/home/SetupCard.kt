package dev.leosnczki.sonora.feature.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SetupStep
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton
import dev.leosnczki.sonora.core.domain.SetupStepKind
import dev.leosnczki.sonora.core.domain.SetupStepState

@Composable
internal fun SetupCard(steps: List<SetupStepState>, actions: HomeActions) {
    SectionCard(stringResource(R.string.setup_title)) {
        Text(stringResource(R.string.setup_intro), style = SonoraType.Body, color = SonoraColors.TextMuted)
        steps.forEachIndexed { index, step ->
            when (step.kind) {
                SetupStepKind.Wallpaper -> SetupStep(index + 1, steps.size, stringResource(R.string.setup_wallpaper), step.done) {
                    SonoraButton(stringResource(R.string.setup_wallpaper_action), actions::setWallpaper)
                }
                SetupStepKind.NotificationAccess -> SetupStep(index + 1, steps.size, stringResource(R.string.setup_notifications), step.done) {
                    SonoraButton(stringResource(R.string.setup_notifications_action), actions::allowNotificationAccess)
                }
                SetupStepKind.AudioAccess -> {
                    SetupStep(index + 1, steps.size, stringResource(R.string.setup_audio), step.done, optional = step.optional) {
                        SonoraButton(stringResource(R.string.setup_audio_action), actions::allowAudioAccess, variant = ButtonVariant.Secondary)
                    }
                    if (!step.done) {
                        Text(stringResource(R.string.setup_audio_why), style = SonoraType.Caption, color = SonoraColors.TextMuted)
                    }
                }
            }
        }
    }
}
