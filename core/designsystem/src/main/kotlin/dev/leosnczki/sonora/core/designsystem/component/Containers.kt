package dev.leosnczki.sonora.core.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.leosnczki.sonora.core.designsystem.SonoraColors
import dev.leosnczki.sonora.core.designsystem.SonoraMotion
import dev.leosnczki.sonora.core.designsystem.SonoraRadius
import dev.leosnczki.sonora.core.designsystem.SonoraSize
import dev.leosnczki.sonora.core.designsystem.SonoraSpace
import dev.leosnczki.sonora.core.designsystem.SonoraType

/** The page backdrop: whatever is behind (the live wallpaper) dimmed by the page scrim. */
@Composable
fun PageScrim(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(SonoraColors.ScrimPage), content = content)
}

@Composable
fun SonoraDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(SonoraSize.Divider).background(SonoraColors.Divider))
}

@Composable
fun SonoraHeader(title: String, tagline: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text(title, style = SonoraType.Display, color = SonoraColors.Text, modifier = Modifier.semantics { heading() })
        Text(
            tagline.uppercase(),
            style = SonoraType.Label,
            color = SonoraColors.Text,
            modifier = Modifier.padding(top = SonoraSpace.Xxs, bottom = SonoraSpace.Sm),
        )
        SonoraDivider()
    }
}

enum class SectionCardStyle { Outlined, Modal }

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    style: SectionCardStyle = SectionCardStyle.Outlined,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(if (style == SectionCardStyle.Modal) SonoraRadius.Lg else SonoraRadius.Md)
    val container = when (style) {
        SectionCardStyle.Outlined -> Modifier
            .background(SonoraColors.ScrimCard, shape)
            .border(SonoraSize.StrokeCard, SonoraColors.Border, shape)
        SectionCardStyle.Modal -> Modifier.background(SonoraColors.SurfaceModal, shape)
    }
    Column(
        modifier
            .fillMaxWidth()
            .then(container)
            .padding(SonoraSpace.Md)
            .animateContentSize(tween(SonoraMotion.BASE_MS, easing = SonoraMotion.Ease)),
        verticalArrangement = Arrangement.spacedBy(SonoraSpace.Sm),
    ) {
        Text(
            title.uppercase(),
            style = SonoraType.Label,
            color = SonoraColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        SonoraDivider()
        content()
    }
}
