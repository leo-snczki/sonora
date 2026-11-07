package dev.leosnczki.sonora.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.leosnczki.sonora.core.designsystem.R
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraTheme
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.AppListRow
import dev.leosnczki.sonora.core.designsystem.component.BackdropTile
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.CheckboxRow
import dev.leosnczki.sonora.core.designsystem.component.EmptyState
import dev.leosnczki.sonora.core.designsystem.component.LabeledRangeSlider
import dev.leosnczki.sonora.core.designsystem.component.LabeledSlider
import dev.leosnczki.sonora.core.designsystem.component.PageScrim
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SectionCardStyle
import dev.leosnczki.sonora.core.designsystem.component.SegmentedChoice
import dev.leosnczki.sonora.core.designsystem.component.SettingGroup
import dev.leosnczki.sonora.core.designsystem.component.SetupStep
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton
import dev.leosnczki.sonora.core.designsystem.component.SonoraHeader
import dev.leosnczki.sonora.core.designsystem.component.SonoraTabs
import dev.leosnczki.sonora.core.designsystem.component.SwitchRow

/** Which fake wallpaper the catalog sits on. White is the worst case for contrast. */
enum class CatalogWallpaper { White, Black, Colourful }

/**
 * Debug-only catalog: every component in every state, over a fake wallpaper.
 * Sample text here is placeholder copy for the catalog, not product copy.
 */
@Composable
fun DesignCatalog(wallpaper: CatalogWallpaper, modifier: Modifier = Modifier) {
    val backdrop = when (wallpaper) {
        CatalogWallpaper.White -> Modifier.background(Color.White)
        CatalogWallpaper.Black -> Modifier.background(Color.Black)
        CatalogWallpaper.Colourful -> Modifier.background(Brush.linearGradient(listOf(Color(0xFFE84A5F), Color(0xFF2A363B), Color(0xFF99B898))))
    }
    SonoraTheme {
        Box(modifier.then(backdrop)) {
            PageScrim {
                Column(Modifier.padding(SonoraSpace.Md), verticalArrangement = Arrangement.spacedBy(SonoraSpace.Md)) {
                    SonoraHeader("Catalog", "Design system · ${wallpaper.name} wallpaper")
                    Row(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Md)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SonoraSpace.Md)) {
                            ButtonsCard()
                            SetupCard()
                            ListsCard()
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SonoraSpace.Md)) {
                            ControlsCard()
                            ModalCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonsCard() = SectionCard("Buttons") {
    val icon = painterResource(R.drawable.ic_music)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Xs), verticalArrangement = Arrangement.spacedBy(SonoraSpace.Xs)) {
        SonoraButton("Primary", {})
        SonoraButton("With icon", {}, icon = icon)
        SonoraButton("Loading", {}, loading = true)
        SonoraButton("Disabled", {}, enabled = false)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Xs)) {
        SonoraButton("Secondary", {}, variant = ButtonVariant.Secondary)
        SonoraButton("Disabled", {}, variant = ButtonVariant.Secondary, enabled = false)
        SonoraButton("Text", {}, variant = ButtonVariant.Text)
    }
}

@Composable
private fun SetupCard() = SectionCard("Setup steps") {
    SetupStep(1, 3, "Done step", done = true)
    SetupStep(2, 3, "Step to do", done = false) { SonoraButton("Act", {}) }
    SetupStep(3, 3, "Optional step", done = false, optional = true) {
        SonoraButton("Act", {}, variant = ButtonVariant.Secondary)
    }
}

@Composable
private fun ControlsCard() = SectionCard("Controls") {
    var group by remember { mutableStateOf(true) }
    var closed by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf(true) }
    var single by remember { mutableFloatStateOf(0.6f) }
    var range by remember { mutableStateOf(0.2f..0.7f) }
    var segment by remember { mutableIntStateOf(0) }
    Text("Body text sits in cards. Captions explain options.", style = SonoraType.Body, color = SonoraColors.TextMuted)
    SettingGroup("Group, open", "Supporting text always visible", group, { group = it }) {
        LabeledSlider("Single slider", single, { single = it }, { "${(it * 100).toInt()}%" })
        LabeledRangeSlider("Range slider", range, { range = it }, { "${(it * 100).toInt()}%" }, "Minimum", "Maximum")
        SegmentedChoice(listOf(0, 1), segment, { segment = it }, { if (it == 0) "First" else "Second" })
    }
    SettingGroup("Group, closed", "Defaults apply while off", closed, { closed = it }) {}
    SettingGroup("Group, locked", "", false, {}, lockedReason = "Needs a permission first") {}
    SwitchRow("Switch row", "Supporting text", check, { check = it })
    CheckboxRow("Checkbox row", "Supporting text", check, { check = it })
    CheckboxRow("Checkbox disabled", null, false, {}, enabled = false)
}

@Composable
private fun ListsCard() = SectionCard("Lists") {
    var tab by remember { mutableIntStateOf(0) }
    SonoraTabs(listOf("First", "Second"), tab, { tab = it })
    AppListRow(ColorPainter(SonoraColors.Accent), "App name", "package.name", {}, "Open")
    Row(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Sm)) {
        BackdropTile(ColorPainter(Color(0xFF6C5B7B)), "Selected", selected = true, onClick = {}, modifier = Modifier.width(96.dp))
        BackdropTile(ColorPainter(Color(0xFF355C7D)), "Not selected", selected = false, onClick = {}, modifier = Modifier.width(96.dp))
    }
}

@Composable
private fun ModalCard() = SectionCard("Modal card", style = SectionCardStyle.Modal) {
    EmptyState(painterResource(R.drawable.ic_info), "Empty state", "Nothing here yet.", Modifier.fillMaxWidth()) {
        SonoraButton("Action", {}, variant = ButtonVariant.Secondary)
    }
}
