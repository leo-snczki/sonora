package dev.leosnczki.sonora.feature.backdrops

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraSize
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraTheme
import dev.leosnczki.sonora.core.designsystem.component.ButtonVariant
import dev.leosnczki.sonora.core.designsystem.component.PageScrim
import dev.leosnczki.sonora.core.designsystem.component.SectionCard
import dev.leosnczki.sonora.core.designsystem.component.SectionCardStyle
import dev.leosnczki.sonora.core.designsystem.component.SonoraButton
import dev.leosnczki.sonora.core.domain.SquareCrop
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.leosnczki.sonora.core.designsystem.R as DsR

/**
 * S05: a square crop of an uploaded image, as it will be kept. Drag to move, pinch to zoom, and
 * the same through labelled buttons: rotate left and right, flip, zoom in and out.
 */
@Composable
fun CropScreen(
    image: Bitmap,
    initial: SquareCrop,
    onUse: (SquareCrop) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onCancel)
    var crop by remember(image) { mutableStateOf(initial) }
    // Turning a large image takes a moment: off the main thread, showing the last turn meanwhile.
    val shown by produceState<ImageBitmap?>(null, image, crop.rotation, crop.flipped) {
        val turn = crop
        value = withContext(Dispatchers.Default) { turned(image, turn).asImageBitmap() }
    }
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
                SectionCard(stringResource(R.string.crop_title), Modifier.widthIn(max = 560.dp), style = SectionCardStyle.Modal) {
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .border(SonoraSize.FocusRing, SonoraColors.Accent)
                            .pointerInput(image) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    // Dragging moves the image under the frame: the window moves the other way.
                                    // Pinching keeps the point between the fingers where it is.
                                    val perPixel = crop.sourceRect().size / size.width
                                    val towards = 1f - 1f / zoom
                                    val dx = (centroid.x - size.width / 2f) * towards - pan.x
                                    val dy = (centroid.y - size.height / 2f) * towards - pan.y
                                    crop = crop.zoomIn(zoom).pan(dx * perPixel, dy * perPixel)
                                }
                            },
                    ) { shown?.takeIf { it.width == crop.width && it.height == crop.height }?.let { drawCrop(it, crop) } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Tool(DsR.drawable.ic_rotate_ccw, R.string.crop_rotate_left) { crop = crop.rotateLeft() }
                        Tool(DsR.drawable.ic_rotate_cw, R.string.crop_rotate_right) { crop = crop.rotateRight() }
                        Tool(DsR.drawable.ic_flip_horizontal, R.string.crop_flip) { crop = crop.flip() }
                        Tool(DsR.drawable.ic_zoom_out, R.string.crop_zoom_out) { crop = crop.zoomOut(ZOOM_STEP) }
                        Tool(DsR.drawable.ic_zoom_in, R.string.crop_zoom_in) { crop = crop.zoomIn(ZOOM_STEP) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SonoraSpace.Xs, Alignment.End)) {
                        SonoraButton(stringResource(R.string.crop_cancel), onCancel, variant = ButtonVariant.Text)
                        SonoraButton(stringResource(R.string.crop_use), { onUse(crop) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Tool(icon: Int, label: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.padding(SonoraSpace.Xxs)) {
        Icon(painterResource(icon), contentDescription = stringResource(label), tint = SonoraColors.Text)
    }
}

private fun DrawScope.drawCrop(image: ImageBitmap, crop: SquareCrop) {
    val rect = crop.sourceRect()
    drawImage(
        image,
        srcOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
        srcSize = IntSize(rect.size.roundToInt(), rect.size.roundToInt()),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
    )
}

private const val ZOOM_STEP = 1.25f

