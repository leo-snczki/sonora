package dev.leosnczki.sonora.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.leosnczki.sonora.core.designsystem.R
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraRadius
import dev.leosnczki.sonora.core.designsystem.SonoraSize
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraType

/**
 * One step of the setup card: status icon, label and the action that completes it.
 * TalkBack reads "Step 2 of 3, done".
 */
@Composable
fun SetupStep(
    index: Int,
    total: Int,
    label: String,
    done: Boolean,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    action: @Composable () -> Unit = {},
) {
    val stepOf = stringResource(R.string.ds_step_of, index, total)
    val status = stringResource(if (done) R.string.ds_done else R.string.ds_to_do)
    val optionalText = stringResource(R.string.ds_optional)
    Row(
        modifier.fillMaxWidth().heightIn(min = SonoraSize.TouchMin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
    ) {
        Row(
            Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$stepOf, $label"
                    stateDescription = status
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
        ) {
            Icon(
                painterResource(if (done) R.drawable.ic_check else R.drawable.ic_circle),
                contentDescription = null,
                tint = if (done) SonoraColors.Success else SonoraColors.Track,
                modifier = Modifier.size(SonoraSize.Icon),
            )
            Column {
                Text(label, style = SonoraType.BodyStrong, color = SonoraColors.Text)
                if (optional) Text(optionalText, style = SonoraType.Caption, color = SonoraColors.TextMuted)
            }
        }
        if (!done) action()
    }
}

@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = SonoraSpace.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SonoraSpace.Xs),
    ) {
        Icon(icon, contentDescription = null, tint = SonoraColors.TextMuted, modifier = Modifier.size(SonoraSize.IconLarge))
        Text(title, style = SonoraType.BodyStrong, color = SonoraColors.Text)
        Text(body, style = SonoraType.Body, color = SonoraColors.TextMuted)
        action?.invoke()
    }
}

@Composable
fun AppListRow(
    icon: Painter,
    name: String,
    subtitle: String,
    onClick: () -> Unit,
    clickLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = SonoraSize.TouchMin + SonoraSpace.Md)
            .clickable(onClickLabel = clickLabel, role = Role.Button, onClick = onClick)
            .padding(vertical = SonoraSpace.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Md),
    ) {
        Image(icon, contentDescription = null, modifier = Modifier.size(SonoraSize.AppIcon))
        Column(Modifier.weight(1f)) {
            Text(name, style = SonoraType.BodyStrong, color = SonoraColors.Text)
            Text(subtitle, style = SonoraType.Caption, color = SonoraColors.TextMuted)
        }
    }
}

@Composable
fun SonoraTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    TabRow(
        selectedTabIndex = selected,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = SonoraColors.Text,
        indicator = { positions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(positions[selected]),
                height = SonoraSize.FocusRing,
                color = SonoraColors.Accent,
            )
        },
        divider = { SonoraDivider() },
    ) {
        titles.forEachIndexed { index, title ->
            Tab(
                selected = index == selected,
                onClick = { onSelect(index) },
                text = { Text(title.uppercase(), style = SonoraType.Button) },
                selectedContentColor = SonoraColors.Text,
                unselectedContentColor = SonoraColors.TextMuted,
            )
        }
    }
}

/** A Backdrop in the library: square image, name, selected ring. */
@Composable
fun BackdropTile(
    image: Painter,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SonoraRadius.Md)
    val selectedText = stringResource(R.string.ds_selected)
    Column(
        modifier
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = name
                role = Role.Button
                if (selected) stateDescription = selectedText
            },
        verticalArrangement = Arrangement.spacedBy(SonoraSpace.Xs),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Image(
                image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .then(if (selected) Modifier.border(SonoraSize.FocusRing, SonoraColors.Accent, shape) else Modifier),
            )
            if (selected) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = SonoraColors.Accent,
                    modifier = Modifier.padding(SonoraSpace.Xs).size(SonoraSize.Icon),
                )
            }
        }
        Text(name, style = SonoraType.Caption, color = SonoraColors.Text)
    }
}
