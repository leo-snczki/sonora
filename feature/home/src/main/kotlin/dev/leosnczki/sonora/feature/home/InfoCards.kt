package dev.leosnczki.sonora.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton
import dev.leosnczki.sonora.core.designsystem.R as DsR

@Composable
private fun Question(question: String, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(SonoraSpace.Xxs)) {
        Text(question, style = SonoraType.BodyStrong, color = SonoraColors.Text)
        Text(answer, style = SonoraType.Body, color = SonoraColors.TextMuted)
    }
}

@Composable
internal fun HelpCard(showBatteryHint: Boolean, actions: HomeActions) {
    SectionCard(stringResource(R.string.help_title)) {
        if (showBatteryHint) {
            Question(stringResource(R.string.battery_hint_title), stringResource(R.string.battery_hint_body))
            SonoraButton(
                stringResource(R.string.battery_hint_action),
                actions::openBatterySettings,
                variant = ButtonVariant.Secondary,
                icon = painterResource(DsR.drawable.ic_battery_warning),
            )
        }
        Question(stringResource(R.string.help_detection_q), stringResource(R.string.help_detection_a))
        Question(stringResource(R.string.help_audio_q), stringResource(R.string.help_audio_a))
    }
}

@Composable
internal fun AboutCard(versionName: String, actions: HomeActions) {
    SectionCard(stringResource(R.string.about_title)) {
        Text(stringResource(R.string.about_version, versionName), style = SonoraType.Caption, color = SonoraColors.TextMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Xs)) {
            SonoraButton(
                stringResource(R.string.about_project_page),
                actions::openProjectPage,
                variant = ButtonVariant.Secondary,
                icon = painterResource(DsR.drawable.ic_external_link),
            )
            SonoraButton(stringResource(R.string.about_licences), actions::openLicences, variant = ButtonVariant.Secondary)
        }
    }
}
