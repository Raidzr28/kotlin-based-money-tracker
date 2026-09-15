package com.moneymanager.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.moneymanager.R

/*
 * The world, in one rule: the screen is a column of water lit from above.
 *
 * Elevation is depth, not shadow. A surface nearer the top of the stack is lighter and more
 * translucent because it is nearer the light; a surface buried under others is denser navy.
 * Material 3's tonal elevation already works this way, so the metaphor rides the platform's
 * own system instead of fighting it.
 *
 * Dynamic Color is deliberately not wired up. The depth ladder below is the product's single
 * legibility mechanism, and a wallpaper-derived scheme would reorder it until the waterline
 * stopped meaning anything. Material You is offered as an explicit opt-in in Appearance,
 * never as the default.
 */

// --- Depth ladder: surface light down to the abyss -------------------------------------------

private val Spray = Color(0xFFDCEAF8)
private val Surf = Color(0xFF6FB4FA)
private val Shelf = Color(0xFF17406C)
private val Trench = Color(0xFF113052)
private val Deep = Color(0xFF0D2540)
private val Night = Color(0xFF071628)
private val Abyss = Color(0xFF04101D)

private val Kelp = Color(0xFF43D39E)
private val KelpInk = Color(0xFF0E7A55)
private val Coral = Color(0xFFFF7A6B)
private val CoralInk = Color(0xFFA33227)
private val Sun = Color(0xFFF6C566)
private val SunInk = Color(0xFF8A6300)

private val DarkScheme = darkColorScheme(
    primary = Surf,
    onPrimary = Color(0xFF04223F),
    primaryContainer = Shelf,
    onPrimaryContainer = Color(0xFFCFE5FF),
    inversePrimary = Color(0xFF15558F),
    secondary = Color(0xFF8FC0E8),
    onSecondary = Color(0xFF0A2A46),
    secondaryContainer = Trench,
    onSecondaryContainer = Color(0xFFCFE5FF),
    tertiary = Sun,
    onTertiary = Color(0xFF3E2C00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE3A6),
    background = Night,
    onBackground = Spray,
    surface = Night,
    onSurface = Spray,
    surfaceVariant = Trench,
    onSurfaceVariant = Color(0xFFA9C4E0),
    surfaceTint = Surf,
    inverseSurface = Spray,
    inverseOnSurface = Night,
    error = Coral,
    onError = Color(0xFF54100A),
    errorContainer = Color(0xFF7A241B),
    onErrorContainer = Color(0xFFFFDAD4),
    outline = Color(0xFF5A7DA3),
    outlineVariant = Color(0xFF1D3C60),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF1A3C60),
    surfaceDim = Abyss,
    surfaceContainerLowest = Abyss,
    surfaceContainerLow = Color(0xFF091E33),
    surfaceContainer = Deep,
    surfaceContainerHigh = Color(0xFF12304F),
    surfaceContainerHighest = Color(0xFF17395C),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF15558F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFE3FB),
    onPrimaryContainer = Color(0xFF062744),
    inversePrimary = Surf,
    secondary = Color(0xFF3D6489),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E7F6),
    onSecondaryContainer = Color(0xFF10304C),
    tertiary = SunInk,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE3A6),
    onTertiaryContainer = Color(0xFF2C1F00),
    background = Color(0xFFEFF5FB),
    onBackground = Color(0xFF0A2137),
    surface = Color(0xFFEFF5FB),
    onSurface = Color(0xFF0A2137),
    surfaceVariant = Color(0xFFDCE8F4),
    onSurfaceVariant = Color(0xFF41617F),
    surfaceTint = Color(0xFF15558F),
    inverseSurface = Color(0xFF0A2137),
    inverseOnSurface = Color(0xFFEFF5FB),
    error = CoralInk,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD4),
    onErrorContainer = Color(0xFF410100),
    outline = Color(0xFF6F8DAD),
    outlineVariant = Color(0xFFC3D6E8),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD3E0ED),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FAFD),
    surfaceContainer = Color(0xFFE8F0F9),
    surfaceContainerHigh = Color(0xFFDFEAF5),
    surfaceContainerHighest = Color(0xFFD5E3F1),
)

/**
 * Tokens Material 3 has no role for, because they belong to this product rather than to the
 * platform: the sign of money, the depth gradient, and the waterline itself.
 *
 * [expense] is deliberately not red. In a tracker you log expenses all day; painting every one
 * of them as an alarm makes the alarm meaningless. Only over-budget and overdue reach for
 * [alert]. The minus sign and the tabular column carry the direction, so colour is never the
 * only thing saying which way the money went.
 */
@Immutable
data class WaterTokens(
    val income: Color,
    val expense: Color,
    val alert: Color,
    val goal: Color,
    val waterline: Color,
    val causticHigh: Color,
    val causticLow: Color,
    val glass: Color,
    val glassEdge: Color,
    /** Surface light down to the abyss. Index 0 is nearest the light. */
    val depth: List<Color>,
)

private val DarkWater = WaterTokens(
    income = Kelp,
    expense = Color(0xFFC6DCF2),
    alert = Coral,
    goal = Sun,
    waterline = Color(0xFF8FD8FF),
    causticHigh = Color(0x382E86D8),
    causticLow = Color(0x14061A2E),
    glass = Color(0x2E4E9BE8),
    glassEdge = Color(0x3DBFE0FF),
    depth = listOf(Shelf, Trench, Deep, Night, Abyss),
)

private val LightWater = WaterTokens(
    income = KelpInk,
    expense = Color(0xFF1F4463),
    alert = CoralInk,
    goal = SunInk,
    waterline = Color(0xFF15558F),
    causticHigh = Color(0x33A8D2F5),
    causticLow = Color(0x0F15558F),
    glass = Color(0x9EFFFFFF),
    glassEdge = Color(0x80FFFFFF),
    depth = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFE8F0F9),
        Color(0xFFD5E3F1),
        Color(0xFFBFD4E9),
        Color(0xFF9FBBD8),
    ),
)

val LocalWater = staticCompositionLocalOf { DarkWater }

// --- Type -------------------------------------------------------------------------------------

@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: Int, width: Float) = Font(
    R.font.archivo_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width),
    ),
)

/** Archivo run wide. The voice of every figure that stands for real money. */
private val ArchivoWide = FontFamily(
    archivo(400, 112f),
    archivo(500, 112f),
    archivo(600, 115f),
    archivo(700, 118f),
)

/** Archivo at normal width, for titles and headings. */
private val ArchivoText = FontFamily(
    archivo(400, 100f),
    archivo(500, 100f),
    archivo(600, 100f),
    archivo(700, 100f),
)

/** Lining tabular figures. Money in a column aligns on the decimal, always. */
private const val TABULAR = "tnum"

private val Mat = Typography()

private val MoneyTypography = Typography(
    displayLarge = Mat.displayLarge.copy(
        fontFamily = ArchivoWide, fontWeight = FontWeight.W700,
        fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-0.035).em,
        fontFeatureSettings = TABULAR,
    ),
    displayMedium = Mat.displayMedium.copy(
        fontFamily = ArchivoWide, fontWeight = FontWeight.W600,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.03).em,
        fontFeatureSettings = TABULAR,
    ),
    displaySmall = Mat.displaySmall.copy(
        fontFamily = ArchivoWide, fontWeight = FontWeight.W600,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.025).em,
        fontFeatureSettings = TABULAR,
    ),
    headlineLarge = Mat.headlineLarge.copy(
        fontFamily = ArchivoText, fontWeight = FontWeight.W600,
        fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em,
    ),
    headlineMedium = Mat.headlineMedium.copy(
        fontFamily = ArchivoText, fontWeight = FontWeight.W600,
        fontSize = 23.sp, lineHeight = 29.sp, letterSpacing = (-0.018).em,
    ),
    headlineSmall = Mat.headlineSmall.copy(
        fontFamily = ArchivoText, fontWeight = FontWeight.W600,
        fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = (-0.012).em,
    ),
    titleLarge = Mat.titleLarge.copy(
        fontFamily = ArchivoText, fontWeight = FontWeight.W600,
        fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = (-0.012).em,
    ),
    titleMedium = Mat.titleMedium.copy(
        fontFamily = ArchivoText, fontWeight = FontWeight.W600,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = 0.sp,
    ),
    titleSmall = Mat.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = Mat.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = Mat.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = Mat.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = Mat.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = Mat.labelMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.03.em),
    labelSmall = Mat.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.07.em),
)

/** Styles for figures that stand for real money. Tabular, wide, and never italic. */
object MoneyType {
    val hero = TextStyle(
        fontFamily = ArchivoWide, fontWeight = FontWeight.W700,
        fontSize = 54.sp, lineHeight = 58.sp, letterSpacing = (-0.038).em,
        fontFeatureSettings = TABULAR,
    )
    val large = TextStyle(
        fontFamily = ArchivoWide, fontWeight = FontWeight.W600,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.03).em,
        fontFeatureSettings = TABULAR,
    )
    val medium = TextStyle(
        fontFamily = ArchivoWide, fontWeight = FontWeight.W600,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.02).em,
        fontFeatureSettings = TABULAR,
    )
    val row = TextStyle(
        fontFamily = ArchivoWide, fontWeight = FontWeight.W500,
        fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.012).em,
        fontFeatureSettings = TABULAR,
    )
    val small = TextStyle(
        fontFamily = ArchivoWide, fontWeight = FontWeight.W500,
        fontSize = 13.sp, lineHeight = 17.sp, fontFeatureSettings = TABULAR,
    )
}

// --- Shape ------------------------------------------------------------------------------------

private val MoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun MoneyManagerTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWater provides if (dark) DarkWater else LightWater) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = MoneyTypography,
            shapes = MoneyShapes,
            content = content,
        )
    }
}

/** Companion to [MaterialTheme] for the tokens Material has no role for. */
object MoneyTheme {
    val water: WaterTokens
        @Composable @ReadOnlyComposable get() = LocalWater.current
}
