package com.buka.mobile.ui

import android.graphics.Bitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buka.mobile.R
import kotlin.math.abs

val DefaultBukaCoral = Color(0xFFFF7867)
private val DefaultBukaInk = Color(0xFF19202C)

val BukaCoral: Color
    @Composable get() = LocalBukaPalette.current.accent
val BukaCoralSoft: Color
    @Composable get() = LocalBukaPalette.current.accentSoft
val BukaMint: Color
    @Composable get() = LocalBukaPalette.current.accentStrong
val BukaInk: Color
    @Composable get() = LocalBukaPalette.current.ink
val BukaMuted: Color
    @Composable get() = LocalBukaPalette.current.muted
val BukaPaper: Color
    @Composable get() = LocalBukaPalette.current.background
val BukaLine: Color
    @Composable get() = LocalBukaPalette.current.line
val BukaSand: Color
    @Composable get() = LocalBukaPalette.current.surfaceAlt

data class BukaPalette(
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val glassFill: Color,
    val glassFallback: Color,
    val glassBorder: Color,
    val isDark: Boolean,
)

val LocalBukaPalette = staticCompositionLocalOf {
    paletteFor(DefaultBukaCoral, false)
}

private val UiFont = FontFamily(
    Font(R.font.sarasa_ui_sc_regular, FontWeight.Normal),
    Font(R.font.sarasa_ui_sc_semibold, FontWeight.Medium),
    Font(R.font.sarasa_ui_sc_semibold, FontWeight.SemiBold),
    Font(R.font.sarasa_ui_sc_bold, FontWeight.Bold),
)

val DisplayFont = FontFamily(
    Font(R.font.source_han_serif_sc_regular, FontWeight.Normal),
    Font(R.font.source_han_serif_sc_semibold, FontWeight.SemiBold),
)

private fun text(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = UiFont, fontSize = size.sp, lineHeight = lineHeight.sp, fontWeight = weight)

private val BukaTypography = Typography(
    displayLarge = text(38, 46, FontWeight.SemiBold).copy(fontFamily = DisplayFont),
    displayMedium = text(32, 40, FontWeight.SemiBold).copy(fontFamily = DisplayFont),
    displaySmall = text(28, 36, FontWeight.SemiBold).copy(fontFamily = DisplayFont),
    headlineLarge = text(28, 36, FontWeight.SemiBold).copy(fontFamily = DisplayFont),
    headlineMedium = text(24, 32, FontWeight.SemiBold).copy(fontFamily = DisplayFont),
    headlineSmall = text(20, 28, FontWeight.SemiBold),
    titleLarge = text(20, 28, FontWeight.SemiBold),
    titleMedium = text(16, 24, FontWeight.Medium),
    titleSmall = text(14, 20, FontWeight.Medium),
    bodyLarge = text(16, 24),
    bodyMedium = text(14, 20),
    bodySmall = text(12, 18),
    labelLarge = text(14, 20, FontWeight.SemiBold),
    labelMedium = text(12, 16, FontWeight.Medium),
    labelSmall = text(11, 16, FontWeight.Medium),
)

private val BukaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BukaMobileTheme(
    accent: Color = DefaultBukaCoral,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(accent, dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.ink,
            secondary = palette.accentStrong,
            onSecondary = palette.onAccent,
            background = palette.background,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            outlineVariant = palette.line,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            primaryContainer = palette.accentSoft,
            onPrimaryContainer = palette.ink,
            secondary = palette.accentStrong,
            onSecondary = palette.onAccent,
            background = palette.background,
            onBackground = palette.ink,
            surface = palette.surface,
            onSurface = palette.ink,
            surfaceVariant = palette.surfaceAlt,
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            outlineVariant = palette.line,
        )
    }
    CompositionLocalProvider(
        LocalBukaPalette provides palette,
        LocalContentColor provides palette.ink,
        LocalIndication provides NoRippleIndication,
        LocalRippleConfiguration provides null,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BukaTypography,
            shapes = BukaShapes,
            content = content,
        )
    }
}

fun paletteFor(sourceAccent: Color, dark: Boolean): BukaPalette {
    val raw = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.rgb(
            (sourceAccent.red * 255).toInt().coerceIn(0, 255),
            (sourceAccent.green * 255).toInt().coerceIn(0, 255),
            (sourceAccent.blue * 255).toInt().coerceIn(0, 255),
        ),
        raw,
    )
    val hue = if (raw[1] < .08f) 8f else raw[0]
    val accent = if (dark) {
        hsvColor(hue, raw[1].coerceIn(.42f, .72f), .86f)
    } else {
        hsvColor(hue, raw[1].coerceIn(.42f, .70f), .72f)
    }
    val onAccent = if (accent.luminance() > .52f) Color(0xFF17191D) else Color.White
    return if (dark) {
        BukaPalette(
            accent = accent,
            accentStrong = hsvColor(hue, .55f, .72f),
            accentSoft = mix(Color(0xFF282D35), accent, .28f),
            onAccent = onAccent,
            background = mix(Color(0xFF111318), accent, .12f),
            surface = mix(Color(0xFF171A20), accent, .14f),
            surfaceAlt = mix(Color(0xFF22262E), accent, .18f),
            ink = mix(Color(0xFFF5F6F8), accent, .035f),
            muted = mix(Color(0xFFAEB5C0), accent, .18f),
            line = mix(Color(0xFF3B414B), accent, .18f),
            glassFill = mix(Color(0xFF23272E), accent, .18f).copy(alpha = .62f),
            glassFallback = mix(Color(0xFF20242A), accent, .16f),
            glassBorder = Color.White.copy(alpha = .16f),
            isDark = true,
        )
    } else {
        BukaPalette(
            accent = accent,
            accentStrong = hsvColor(hue, .50f, .60f),
            accentSoft = mix(Color.White, accent, .24f),
            onAccent = onAccent,
            background = mix(Color(0xFFFAFAF8), accent, .075f),
            surface = mix(Color.White, accent, .045f),
            surfaceAlt = mix(Color(0xFFF1F3F5), accent, .16f),
            ink = mix(Color(0xFF19202C), accent, .065f),
            muted = mix(Color(0xFF687181), accent, .16f),
            line = mix(Color(0xFFDCE0E6), accent, .13f),
            glassFill = mix(Color.White, accent, .12f).copy(alpha = .34f),
            glassFallback = mix(Color.White, accent, .08f),
            glassBorder = Color.White.copy(alpha = .76f),
            isDark = false,
        )
    }
}

fun extractArtworkAccent(bitmap: Bitmap?): Color {
    if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return DefaultBukaCoral
    return runCatching {
        val size = 24
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        try {
            val hsv = FloatArray(3)
            val weight = FloatArray(24)
            val sumR = FloatArray(24)
            val sumG = FloatArray(24)
            val sumB = FloatArray(24)
            for (y in 0 until size) for (x in 0 until size) {
                val color = scaled.getPixel(x, y)
                android.graphics.Color.colorToHSV(color, hsv)
                if (hsv[1] < .15f || hsv[2] < .15f || hsv[2] > .96f) continue
                val bucket = (hsv[0] / 15f).toInt().coerceIn(0, 23)
                val w = hsv[1] * hsv[2]
                weight[bucket] += w
                sumR[bucket] += android.graphics.Color.red(color) * w
                sumG[bucket] += android.graphics.Color.green(color) * w
                sumB[bucket] += android.graphics.Color.blue(color) * w
            }
            var best = -1
            for (i in 0 until 24) {
                if (weight[i] > 0f && (best < 0 || weight[i] > weight[best])) best = i
            }
            if (best < 0) DefaultBukaCoral else Color(
                android.graphics.Color.rgb(
                    (sumR[best] / weight[best]).toInt().coerceIn(0, 255),
                    (sumG[best] / weight[best]).toInt().coerceIn(0, 255),
                    (sumB[best] / weight[best]).toInt().coerceIn(0, 255),
                ),
            )
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }.getOrDefault(DefaultBukaCoral)
}

fun fallbackTrackAccent(title: String): Color {
    val hue = (abs(title.lowercase().hashCode()) % 360).toFloat()
    return hsvColor(hue, .52f, .76f)
}

private fun hsvColor(hue: Float, saturation: Float, value: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))

private fun mix(base: Color, tint: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = base.red * (1f - t) + tint.red * t,
        green = base.green * (1f - t) + tint.green * t,
        blue = base.blue * (1f - t) + tint.blue * t,
        alpha = base.alpha,
    )
}
