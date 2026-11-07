// Design tokens. Names are roles: a rebrand changes values only.
package dev.leosnczki.sonora.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object SonoraColors {
    // Worst-case composites (a white wallpaper under the scrims), used for contrast checks only.
    val Bg = Color(0xFF737373)
    val Surface = Color(0xFF393939)

    val SurfaceModal = Color(0xFF18191D)
    val Border = Color(0xFFFFFFFF)
    val BorderInput = Color(0xFFB9BBC4)
    val Track = Color(0xFF8F9099)
    val Text = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFFDADBE0)
    val Accent = Color(0xFFE4E5EA)
    val OnAccent = Color(0xFF1B1C21)
    val Danger = Color(0xFFFFB4AB)
    val Success = Color(0xFFA6D9A8)
    val Warning = Color(0xFFF5D27A)

    // What is actually drawn over the wallpaper.
    val ScrimPage = Color.Black.copy(alpha = 0.55f)
    val ScrimCard = Color.Black.copy(alpha = 0.50f)
    val ScrimDialog = Color.Black.copy(alpha = 0.60f)
    val Divider = Color.White.copy(alpha = 0.40f)
    const val DISABLED_ALPHA = 0.38f
}

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

object SonoraFonts {
    val Sans = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700))

    @OptIn(ExperimentalTextApi::class)
    val Mono = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    )
}

object SonoraType {
    val Display = TextStyle(fontFamily = SonoraFonts.Sans, fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em)
    val Title = TextStyle(fontFamily = SonoraFonts.Sans, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)

    // Uppercase is applied to the string, not by the style, so TalkBack reads normal words.
    val Label = TextStyle(fontFamily = SonoraFonts.Sans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.12.em)
    val Button = TextStyle(fontFamily = SonoraFonts.Sans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.06.em)
    val Body = TextStyle(fontFamily = SonoraFonts.Sans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.01.em)
    val BodyStrong = Body.copy(fontWeight = FontWeight.SemiBold)
    val Caption = TextStyle(fontFamily = SonoraFonts.Sans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.02.em)
    val Mono = TextStyle(fontFamily = SonoraFonts.Mono, fontSize = 12.sp, lineHeight = 16.sp)
}

object SonoraSpace {
    val Xxs = 4.dp
    val Xs = 8.dp
    val Sm = 12.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 32.dp
    val Xxl = 48.dp
}

object SonoraSize {
    val Button = 40.dp
    val TouchMin = 48.dp
    val Icon = 18.dp
    val IconLarge = 32.dp
    val AppIcon = 40.dp
    val ArtThumbMin = 84.dp
    val ArtThumbMax = 120.dp
    val Header = 112.dp
    val StrokeCard = 2.dp
    val StrokeInput = 1.dp
    val Divider = 1.dp
    val FocusRing = 2.dp
    val MaxContentWidth = 1200.dp
    val TwoColumnBreakpoint = 600.dp
}

object SonoraRadius {
    val Sm = 4.dp
    val Md = 12.dp
    val Lg = 28.dp
    val Pill = 999.dp
}

object SonoraMotion {
    const val FAST_MS = 150
    const val BASE_MS = 250
    val Ease = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
