package dev.leosnczki.sonora.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraTheme
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.AppListRow
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.EmptyState
import dev.leosnczki.sonora.core.designsystem.component.PageScrim
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SectionCardStyle
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton
import dev.leosnczki.sonora.core.designsystem.R as DsR

/** An installed app that plays music. */
data class PlayerApp(val name: String, val packageName: String, val icon: ImageBitmap?)

/** S06: pick a Player to open. */
@Composable
fun PlayerChooserScreen(players: List<PlayerApp>, onOpen: (PlayerApp) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    SonoraTheme {
        PageScrim(modifier) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(SonoraSpace.Md),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(SonoraSpace.Sm)) {
                    SonoraButton(stringResource(R.string.licences_back), onBack, variant = ButtonVariant.Text)
                    SectionCard(stringResource(R.string.chooser_title), style = SectionCardStyle.Modal) {
                        Text(stringResource(R.string.chooser_hint), style = SonoraType.Body, color = SonoraColors.TextMuted)
                        if (players.isEmpty()) {
                            EmptyState(
                                painterResource(DsR.drawable.ic_music),
                                stringResource(R.string.chooser_empty_title),
                                stringResource(R.string.chooser_empty_body),
                            )
                        }
                        val openLabel = stringResource(R.string.chooser_open)
                        players.forEach { player ->
                            AppListRow(
                                icon = player.icon?.let(::BitmapPainter) ?: ColorPainter(SonoraColors.ScrimCard),
                                name = player.name,
                                subtitle = player.packageName,
                                onClick = { onOpen(player) },
                                clickLabel = openLabel,
                            )
                        }
                    }
                }
            }
        }
    }
}
