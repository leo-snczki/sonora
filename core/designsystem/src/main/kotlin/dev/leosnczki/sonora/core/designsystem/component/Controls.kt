package dev.leosnczki.sonora.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraMotion
import dev.leosnczki.sonora.core.designsystem.SonoraRadius
import dev.leosnczki.sonora.core.designsystem.SonoraSize
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraType

private val switchColors
    @Composable get() = SwitchDefaults.colors(
        checkedTrackColor = SonoraColors.Accent,
        checkedThumbColor = SonoraColors.OnAccent,
        checkedBorderColor = SonoraColors.Accent,
        uncheckedTrackColor = SonoraColors.ScrimCard,
        uncheckedThumbColor = SonoraColors.Track,
        uncheckedBorderColor = SonoraColors.Track,
    )

private val checkboxColors
    @Composable get() = CheckboxDefaults.colors(
        checkedColor = SonoraColors.Accent,
        checkmarkColor = SonoraColors.OnAccent,
        uncheckedColor = SonoraColors.Track,
    )

private val sliderColors
    @Composable get() = SliderDefaults.colors(
        thumbColor = SonoraColors.Accent,
        activeTrackColor = SonoraColors.Accent,
        inactiveTrackColor = SonoraColors.Track,
        activeTickColor = SonoraColors.OnAccent,
        inactiveTickColor = SonoraColors.Accent,
    )

/** Label + always-visible explanation, the way every option describes itself. */
@Composable
private fun OptionText(label: String, supporting: String?, enabled: Boolean, modifier: Modifier = Modifier) {
    val alpha = if (enabled) 1f else SonoraColors.DISABLED_ALPHA
    Column(modifier) {
        Text(label, style = SonoraType.BodyStrong, color = SonoraColors.Text.copy(alpha = alpha))
        if (supporting != null) {
            Text(supporting, style = SonoraType.Caption, color = SonoraColors.TextMuted.copy(alpha = alpha))
        }
    }
}

/**
 * A settings group with the original's "use my settings" behaviour: off means the group's
 * defaults apply and its controls are hidden; on reveals them, indented.
 * [lockedReason] disables the toggle and says why (e.g. a missing permission).
 */
@Composable
fun SettingGroup(
    label: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    lockedReason: String? = null,
    lockedAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val enabled = lockedReason == null
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = SonoraSize.TouchMin)
                .toggleable(value = checked && enabled, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
        ) {
            OptionText(label, lockedReason ?: supporting, enabled, Modifier.weight(1f))
            Switch(checked = checked && enabled, onCheckedChange = null, enabled = enabled, colors = switchColors)
        }
        if (!enabled && lockedAction != null) lockedAction()
        AnimatedVisibility(
            visible = checked && enabled,
            enter = expandVertically(tween(SonoraMotion.BASE_MS, easing = SonoraMotion.Ease)),
            exit = shrinkVertically(tween(SonoraMotion.BASE_MS, easing = SonoraMotion.Ease)),
        ) {
            Column(
                Modifier.padding(start = SonoraSpace.Lg, top = SonoraSpace.Xs),
                verticalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
                content = content,
            )
        }
    }
}

@Composable
fun SwitchRow(
    label: String,
    supporting: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = SonoraSize.TouchMin)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
    ) {
        OptionText(label, supporting, enabled, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled, colors = switchColors)
    }
}

@Composable
fun CheckboxRow(
    label: String,
    supporting: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = SonoraSize.TouchMin)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled, colors = checkboxColors)
        OptionText(label, supporting, enabled, Modifier.weight(1f))
    }
}

@Composable
private fun SliderHeader(label: String, valueText: String, supporting: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        OptionText(label, supporting, enabled = true, modifier = Modifier.weight(1f))
        Text(valueText, style = SonoraType.Caption, color = SonoraColors.TextMuted, textAlign = TextAlign.End)
    }
}

/**
 * [describe] turns a value into words ("Calm", "+20%"): shown next to the label and read by TalkBack.
 */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    describe: (Float) -> String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    // While dragging, the thumb follows the finger, not the (slower) persisted value.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging ?: value
    val description = describe(shown)
    Column(modifier.fillMaxWidth()) {
        SliderHeader(label, description, supporting)
        Slider(
            value = shown,
            onValueChange = {
                dragging = it
                onValueChange(it)
            },
            onValueChangeFinished = { dragging = null },
            valueRange = valueRange,
            steps = steps,
            colors = sliderColors,
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = description
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabeledRangeSlider(
    label: String,
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    describe: (Float) -> String,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    var dragging by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
    val shown = dragging ?: value
    val text = "${describe(shown.start)} – ${describe(shown.endInclusive)}"
    val startInteraction = remember { MutableInteractionSource() }
    val endInteraction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth()) {
        SliderHeader(label, text, supporting)
        RangeSlider(
            value = shown,
            onValueChange = {
                dragging = it
                onValueChange(it)
            },
            onValueChangeFinished = { dragging = null },
            colors = sliderColors,
            startInteractionSource = startInteraction,
            endInteractionSource = endInteraction,
            startThumb = {
                SliderDefaults.Thumb(
                    interactionSource = startInteraction,
                    colors = sliderColors,
                    modifier = Modifier.semantics {
                        contentDescription = startLabel
                        stateDescription = describe(shown.start)
                    },
                )
            },
            endThumb = {
                SliderDefaults.Thumb(
                    interactionSource = endInteraction,
                    colors = sliderColors,
                    modifier = Modifier.semantics {
                        contentDescription = endLabel
                        stateDescription = describe(shown.endInclusive)
                    },
                )
            },
        )
    }
}

/** Two or three equal segments, one selected. Replaces a radio pair. */
@Composable
fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SonoraRadius.Pill)
    Row(
        modifier
            .fillMaxWidth()
            .border(SonoraSize.StrokeInput, SonoraColors.BorderInput, shape)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                label(option).uppercase(),
                style = SonoraType.Button,
                color = if (isSelected) SonoraColors.OnAccent else SonoraColors.Text,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = SonoraSize.TouchMin)
                    .background(if (isSelected) SonoraColors.Accent else Color.Transparent, shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(vertical = SonoraSpace.Sm),
            )
        }
    }
}
