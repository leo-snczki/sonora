package dev.leosnczki.sonora.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import dev.leosnczki.sonora.core.designsystem.R
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraRadius
import dev.leosnczki.sonora.core.designsystem.SonoraSize
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraType

enum class ButtonVariant { Primary, Secondary, Text }

/**
 * The one button. Loading keeps the label (for screen readers) and swaps the icon for a spinner.
 */
@Composable
fun SonoraButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    icon: Painter? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(SonoraRadius.Pill)
    val ring = if (focused) {
        Modifier.border(SonoraSize.FocusRing, SonoraColors.Accent, shape)
    } else {
        Modifier
    }
    val loadingDescription = stringResource(R.string.ds_loading)
    val sized = modifier
        .defaultMinSize(minHeight = SonoraSize.TouchMin)
        .then(ring)
        .semantics { if (loading) stateDescription = loadingDescription }
    val padding = PaddingValues(
        horizontal = if (icon != null || loading) SonoraSpace.Md else SonoraSpace.Lg,
        vertical = SonoraSpace.Xs,
    )
    val content: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Xs), verticalAlignment = Alignment.CenterVertically) {
            when {
                loading -> CircularProgressIndicator(Modifier.size(SonoraSize.Icon), strokeWidth = SonoraSize.FocusRing)
                icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(SonoraSize.Icon))
            }
            Text(label.uppercase(), style = SonoraType.Button)
        }
    }
    val clickable = enabled && !loading
    when (variant) {
        ButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = sized,
            enabled = clickable,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = SonoraColors.Accent,
                contentColor = SonoraColors.OnAccent,
                disabledContainerColor = SonoraColors.Accent.copy(alpha = SonoraColors.DISABLED_ALPHA),
                disabledContentColor = SonoraColors.OnAccent.copy(alpha = SonoraColors.DISABLED_ALPHA),
            ),
            contentPadding = padding,
            interactionSource = interaction,
        ) { content() }
        ButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = sized,
            enabled = clickable,
            shape = shape,
            border = BorderStroke(
                SonoraSize.StrokeInput,
                if (clickable) SonoraColors.BorderInput else SonoraColors.BorderInput.copy(alpha = SonoraColors.DISABLED_ALPHA),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = SonoraColors.Text,
                disabledContentColor = SonoraColors.Text.copy(alpha = SonoraColors.DISABLED_ALPHA),
            ),
            contentPadding = padding,
            interactionSource = interaction,
        ) { content() }
        ButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = sized,
            enabled = clickable,
            shape = shape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = SonoraColors.Accent,
                disabledContentColor = SonoraColors.Accent.copy(alpha = SonoraColors.DISABLED_ALPHA),
            ),
            contentPadding = padding,
            interactionSource = interaction,
        ) { content() }
    }
}
