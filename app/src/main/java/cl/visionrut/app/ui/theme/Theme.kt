package cl.visionrut.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cl.visionrut.app.R

// Paleta y colores de Material Design.
val Orange = Color(0xFFFF7900)
val Blue = Color(0xFF3E50B4)
val Alert = Color(0xFFFFB600)
val Background = Color(0xFFF5F5F7)
val Ink = Color(0xFF23262D)
val Muted = Color(0xFF5D667A)
val Line = Color(0xFFE9EBF1)
val Pending = Color(0xFFAAB3C5)
val Red = Color(0xFFE94655)

internal val VisionRUTColors =
    lightColorScheme(
        primary = Blue,
        primaryContainer = Color(0xFFEDF1FF),
        onPrimaryContainer = Blue,
        secondary = Orange,
        secondaryContainer = Color(0xFFEDF1FF),
        onSecondaryContainer = Blue,
        tertiary = Orange,
        tertiaryContainer = Color(0xFFFFEDDC),
        onTertiaryContainer = Ink,
        background = Background,
        surface = Color.White,
        onSurface = Ink,
        onBackground = Ink,
        error = Red,
    )

// Fuente y tamaños compartidos por todas las pantallas.
private val AppFontFamily =
    FontFamily(
        Font(R.font.roboto_regular),
        Font(R.font.roboto_medium, FontWeight.Medium),
        Font(R.font.roboto_bold, FontWeight.Bold),
    )

private fun style(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = AppFontFamily,
        fontSize = size.sp,
        lineHeight = height.sp,
        fontWeight = weight,
        letterSpacing = 0.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

val AppTypography =
    Typography(
        displayLarge = style(36, 44),
        displayMedium = style(32, 40),
        displaySmall = style(28, 36),
        headlineLarge = style(28, 36, FontWeight.Bold),
        headlineMedium = style(24, 30, FontWeight.Bold),
        headlineSmall = style(22, 28, FontWeight.Bold),
        titleLarge = style(22, 28, FontWeight.Bold),
        titleMedium = style(18, 24, FontWeight.Medium),
        titleSmall = style(16, 22, FontWeight.Medium),
        bodyLarge = style(16, 22),
        bodyMedium = style(15, 21),
        bodySmall = style(13, 18),
        labelLarge = style(14, 20, FontWeight.Medium),
        labelMedium = style(14, 20, FontWeight.Medium),
        labelSmall = style(13, 18),
    )

// Aplicación del tema visual.
@Composable
fun VisionRUTTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VisionRUTColors, typography = AppTypography, content = content)
}
