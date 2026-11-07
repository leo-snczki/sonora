package dev.leosnczki.sonora.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraTheme
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.PageScrim
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton

/** Notices for the fonts and icons Sonora bundles; their licences require shipping them. */
@Composable
fun LicencesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val texts = remember {
        listOf("ofl_manrope.txt", "ofl_jetbrainsmono.txt", "lucide.txt").associateWith { name ->
            runCatching { context.assets.open("licenses/$name").bufferedReader().use { it.readText() } }.getOrDefault("")
        }
    }
    SonoraTheme {
        PageScrim(modifier) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(SonoraSpace.Md),
                verticalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
            ) {
                SonoraButton(stringResource(R.string.licences_back), onBack, variant = ButtonVariant.Text)
                Text(
                    stringResource(R.string.licences_title),
                    style = SonoraType.Title,
                    color = SonoraColors.Text,
                    modifier = Modifier.semantics { heading() },
                )
                Notice(stringResource(R.string.licences_fonts), texts.getValue("ofl_manrope.txt") + "\n\n" + texts.getValue("ofl_jetbrainsmono.txt"))
                Notice(stringResource(R.string.licences_icons), texts.getValue("lucide.txt"))
                Notice(stringResource(R.string.licences_libraries), stringResource(R.string.licences_apache))
            }
        }
    }
}

@Composable
private fun Notice(title: String, text: String) {
    SectionCard(title) {
        Text(text, style = SonoraType.Caption, color = SonoraColors.TextMuted)
    }
}
