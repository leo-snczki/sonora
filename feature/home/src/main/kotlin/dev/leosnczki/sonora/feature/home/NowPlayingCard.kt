package dev.leosnczki.sonora.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraRadius
import dev.leosnczki.sonora.core.designsystem.SonoraSize
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton
import dev.leosnczki.sonora.core.designsystem.R as DsR

/** The three things the Now Playing card can say. */
sealed interface NowPlayingCardState {
    data object NoAccess : NowPlayingCardState

    data class Idle(val showDetectionHint: Boolean) : NowPlayingCardState

    data class Playing(
        val title: String?,
        val artist: String?,
        val album: String?,
        val playerName: String,
        val art: ImageBitmap?,
        val paused: Boolean,
        val showDetectionHint: Boolean = false,
    ) : NowPlayingCardState
}

@Composable
internal fun NowPlayingCard(state: NowPlayingCardState, actions: HomeActions) {
    SectionCard(stringResource(R.string.now_playing_title)) {
        when (state) {
            NowPlayingCardState.NoAccess -> {
                Body(stringResource(R.string.now_playing_no_access))
                SonoraButton(stringResource(R.string.setup_notifications_action), actions::allowNotificationAccess)
            }
            is NowPlayingCardState.Idle -> {
                Body(stringResource(R.string.now_playing_nothing))
                SonoraButton(stringResource(R.string.now_playing_open_music_app), actions::openPlayerChooser, variant = ButtonVariant.Secondary)
                if (state.showDetectionHint) DetectionHint(actions)
            }
            is NowPlayingCardState.Playing -> {
                Playing(state, actions)
                if (state.showDetectionHint) DetectionHint(actions)
            }
        }
    }
}

@Composable
private fun Body(text: String) = Text(text, style = SonoraType.Body, color = SonoraColors.TextMuted)

@Composable
private fun Playing(state: NowPlayingCardState.Playing, actions: HomeActions) {
    val unknown = stringResource(R.string.now_playing_unknown)
    val sentence = stringResource(R.string.now_playing_sentence, state.title ?: unknown, state.artist ?: unknown)
    val status = stringResource(if (state.paused) R.string.now_playing_paused_in else R.string.now_playing_playing_in, state.playerName)
    val spoken = listOfNotNull(sentence, state.album, status).joinToString(", ")
    Row(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Sm), verticalAlignment = Alignment.CenterVertically) {
        Art(state.art)
        Column(
            Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = spoken },
            verticalArrangement = Arrangement.spacedBy(SonoraSpace.Xxs),
        ) {
            Line(state.title ?: unknown, SonoraType.BodyStrong, SonoraColors.Text)
            state.artist?.let { Line(it, SonoraType.Body, SonoraColors.TextMuted) }
            state.album?.let { Line(it, SonoraType.Body, SonoraColors.TextMuted) }
            Line(status, SonoraType.Caption, SonoraColors.TextMuted)
        }
    }
    SonoraButton(
        stringResource(R.string.now_playing_open_player, state.playerName),
        actions::openPlayer,
        variant = ButtonVariant.Secondary,
    )
}

@Composable
private fun Line(text: String, style: androidx.compose.ui.text.TextStyle, color: androidx.compose.ui.graphics.Color) =
    Text(text, style = style, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis)

@Composable
private fun Art(art: ImageBitmap?) {
    val shape = RoundedCornerShape(SonoraRadius.Sm)
    if (art != null) {
        Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(SonoraSize.ArtThumbMin).clip(shape))
    } else {
        Box(Modifier.size(SonoraSize.ArtThumbMin).background(SonoraColors.ScrimCard, shape), contentAlignment = Alignment.Center) {
            Icon(painterResource(DsR.drawable.ic_music), contentDescription = null, tint = SonoraColors.TextMuted)
        }
    }
}

@Composable
private fun DetectionHint(actions: HomeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(SonoraSpace.Xxs)) {
        Text(stringResource(R.string.detection_hint_title), style = SonoraType.BodyStrong, color = SonoraColors.Warning)
        Body(stringResource(R.string.detection_hint_body))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Xs)) {
            SonoraButton(
                stringResource(R.string.detection_hint_restart),
                actions::restartListener,
                variant = ButtonVariant.Secondary,
                icon = painterResource(DsR.drawable.ic_rotate_ccw),
            )
        }
    }
}
