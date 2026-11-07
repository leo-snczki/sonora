package dev.leosnczki.sonora.feature.backdrops

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraTheme
import dev.leosnczki.sonora.core.designsystem.SonoraType
import dev.leosnczki.sonora.core.designsystem.component.BackdropTile
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.ConfirmDialog
import dev.leosnczki.sonora.core.designsystem.component.EmptyState
import dev.leosnczki.sonora.core.designsystem.component.PageScrim
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SectionCardStyle
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton
import dev.leosnczki.sonora.core.designsystem.component.SonoraTabs
import dev.leosnczki.sonora.core.domain.BackdropId
import dev.leosnczki.sonora.core.domain.Backdrops
import dev.leosnczki.sonora.core.domain.BuiltinBackdrop
import dev.leosnczki.sonora.core.designsystem.R as DsR

/** A Backdrop the user saved, as the library shows it; [image] is null while it is being read. */
data class MineBackdrop(val id: BackdropId, val name: String, val image: ImageBitmap?)

/** Everything the Backdrop Library (S04) shows. */
data class BackdropLibraryState(
    val chosen: BackdropId,
    val mine: List<MineBackdrop> = emptyList(),
    /** Something is playing with Album Art, so it can be saved as a Backdrop. */
    val canSaveAlbumArt: Boolean = false,
)

interface BackdropLibraryActions {
    fun choose(id: BackdropId)
    fun saveAlbumArt()
    fun delete(id: BackdropId)
    fun back()
}

/** S04: pick the Backdrop from the built-in ones or the user's own (Mine). */
@Composable
fun BackdropLibraryScreen(state: BackdropLibraryState, actions: BackdropLibraryActions, modifier: Modifier = Modifier) {
    BackHandler(onBack = actions::back)
    var tab by rememberSaveable { mutableStateOf(0) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
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
                Column(Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(SonoraSpace.Sm)) {
                    SonoraButton(stringResource(R.string.library_back), actions::back, variant = ButtonVariant.Text)
                    SectionCard(stringResource(R.string.library_title), style = SectionCardStyle.Modal) {
                        Text(stringResource(R.string.library_desc), style = SonoraType.Body, color = SonoraColors.TextMuted)
                        SonoraTabs(
                            listOf(stringResource(R.string.library_builtin), stringResource(R.string.library_mine)),
                            selected = tab,
                            onSelect = { tab = it },
                        )
                        when (tab) {
                            0 -> TileGrid(Backdrops.Builtin) { backdrop ->
                                BackdropTile(
                                    image = gradientPainter(backdrop),
                                    name = builtinName(backdrop.id),
                                    selected = backdrop.id == state.chosen,
                                    onClick = { actions.choose(backdrop.id) },
                                )
                            }
                            else -> Mine(state, actions, onDelete = { deleting = it.value })
                        }
                    }
                }
            }
        }
        deleting?.let { id ->
            ConfirmDialog(
                title = stringResource(R.string.library_delete_title),
                body = stringResource(R.string.library_delete_body),
                confirmLabel = stringResource(R.string.library_delete),
                dismissLabel = stringResource(R.string.library_cancel),
                onConfirm = {
                    deleting = null
                    actions.delete(BackdropId(id))
                },
                onDismiss = { deleting = null },
            )
        }
    }
}

@Composable
private fun Mine(state: BackdropLibraryState, actions: BackdropLibraryActions, onDelete: (BackdropId) -> Unit) {
    val save = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SonoraButton(stringResource(R.string.library_save_album_art), actions::saveAlbumArt, enabled = state.canSaveAlbumArt)
            if (!state.canSaveAlbumArt) {
                Text(stringResource(R.string.library_save_unavailable), style = SonoraType.Caption, color = SonoraColors.TextMuted)
            }
        }
    }
    if (state.mine.isEmpty()) {
        EmptyState(
            painterResource(DsR.drawable.ic_music),
            stringResource(R.string.library_empty_title),
            stringResource(R.string.library_empty_body),
            action = save,
        )
        return
    }
    save()
    val deleteLabel = stringResource(R.string.library_delete)
    TileGrid(state.mine) { backdrop ->
        BackdropTile(
            image = backdrop.image?.let(::BitmapPainter) ?: ColorPainter(SonoraColors.ScrimCard),
            name = backdrop.name,
            selected = backdrop.id == state.chosen,
            onClick = { actions.choose(backdrop.id) },
            onDelete = { onDelete(backdrop.id) },
            deleteLabel = deleteLabel,
        )
    }
}

/** Tiles in rows of [COLUMNS]; a plain column of rows, since the page itself scrolls. */
@Composable
private fun <T> TileGrid(items: List<T>, tile: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SonoraSpace.Sm)) {
        items.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Sm)) {
                row.forEach { Box(Modifier.weight(1f)) { tile(it) } }
                repeat(COLUMNS - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private const val COLUMNS = 4

private fun gradientPainter(backdrop: BuiltinBackdrop): Painter = BrushPainter(Brush.linearGradient(backdrop.colors.map(::Color)))

@Composable
private fun builtinName(id: BackdropId): String = stringResource(
    when (id.value.removePrefix("builtin:")) {
        "dusk" -> R.string.backdrop_dusk
        "lagoon" -> R.string.backdrop_lagoon
        "ember" -> R.string.backdrop_ember
        "moss" -> R.string.backdrop_moss
        "orchid" -> R.string.backdrop_orchid
        "glacier" -> R.string.backdrop_glacier
        "citrus" -> R.string.backdrop_citrus
        else -> R.string.backdrop_graphite
    },
)
