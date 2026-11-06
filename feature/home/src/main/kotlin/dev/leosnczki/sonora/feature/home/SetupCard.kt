package dev.leosnczki.sonora.feature.home

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SetupStep
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton

@Composable
internal fun SetupCard(state: HomeScreenState, actions: HomeActions) {
    SectionCard(stringResource(R.string.setup_title)) {
        Text(stringResource(R.string.setup_intro), style = SonoraType.Body, color = SonoraColors.TextMuted)
        SetupStep(1, 2, stringResource(R.string.setup_wallpaper), done = state.wallpaperSet) {
            SonoraButton(stringResource(R.string.setup_wallpaper_action), actions::setWallpaper)
        }
        SetupStep(2, 2, stringResource(R.string.setup_notifications), done = state.notificationAccess) {
            SonoraButton(stringResource(R.string.setup_notifications_action), actions::allowNotificationAccess)
        }
    }
}
