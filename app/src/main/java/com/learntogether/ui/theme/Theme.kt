package com.learntogether.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

//  Color Palette
// Warm, inviting teal + amber accent palette

val Teal10 = Color(0xFF0D2B2A)
val Teal20 = Color(0xFF1A4745)
val Teal30 = Color(0xFF246460)
val Teal40 = Color(0xFF2E807B)
val Teal50 = Color(0xFF389D96)
val Teal60 = Color(0xFF4DB8B0)
val Teal70 = Color(0xFF73CEC7)
val Teal80 = Color(0xFF9FE0DB)
val Teal90 = Color(0xFFCBF0ED)
val Teal95 = Color(0xFFE5F8F6)
val Teal99 = Color(0xFFF5FCFB)

val Amber10 = Color(0xFF2D1F00)
val Amber20 = Color(0xFF4A3400)
val Amber30 = Color(0xFF694B00)
val Amber40 = Color(0xFF8A6300)
val Amber50 = Color(0xFFAB7C00)
val Amber60 = Color(0xFFCC9500)
val Amber70 = Color(0xFFE8AF1E)
val Amber80 = Color(0xFFF5C94E)
val Amber90 = Color(0xFFFFE08A)
val Amber95 = Color(0xFFFFF0C5)

val Coral40 = Color(0xFFBF4A3A)
val Coral80 = Color(0xFFFFB4A8)
val Coral90 = Color(0xFFFFDAD4)

val Neutral10 = Color(0xFF1A1C1E)
val Neutral20 = Color(0xFF2F3133)
val Neutral30 = Color(0xFF454749)
val Neutral40 = Color(0xFF5D5F61)
val Neutral50 = Color(0xFF76787A)
val Neutral60 = Color(0xFF909294)
val Neutral70 = Color(0xFFABADAF)
val Neutral80 = Color(0xFFC6C8CA)
val Neutral90 = Color(0xFFE2E4E6)
val Neutral95 = Color(0xFFF1F3F5)
val Neutral99 = Color(0xFFFCFCFE)

val NeutralVariant30 = Color(0xFF3F4947)
val NeutralVariant50 = Color(0xFF6B7573)
val NeutralVariant60 = Color(0xFF858F8D)
val NeutralVariant80 = Color(0xFFBFC9C7)
val NeutralVariant90 = Color(0xFFDBE5E3)

//  Color Schemes
private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber10,
    tertiary = Coral40,
    onTertiary = Color.White,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Color(0xFF410001),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Teal80
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Amber80,
    onSecondary = Amber20,
    secondaryContainer = Amber30,
    onSecondaryContainer = Amber90,
    tertiary = Coral80,
    onTertiary = Color(0xFF5F1411),
    tertiaryContainer = Color(0xFF7E2D26),
    onTertiaryContainer = Coral90,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Teal40
)

//  Accent: Green (replaces older “sunset”-style warm accent)
private val GreenPrimaryLight = Color(0xFF1B5E20)
private val GreenPrimaryDark = Color(0xFF81C784)

private val LightGreenColorScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF002106),
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber10,
    tertiary = Coral40,
    onTertiary = Color.White,
    tertiaryContainer = Coral90,
    onTertiaryContainer = Color(0xFF410001),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Color(0xFF97D5A3)
)

private val DarkGreenColorScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = Color(0xFF003910),
    primaryContainer = Color(0xFF00531A),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Amber80,
    onSecondary = Amber20,
    secondaryContainer = Amber30,
    onSecondaryContainer = Amber90,
    tertiary = Coral80,
    onTertiary = Color(0xFF5F1411),
    tertiaryContainer = Color(0xFF7E2D26),
    onTertiaryContainer = Coral90,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = GreenPrimaryLight
)

//  Accent: Violet
private val Violet40 = Color(0xFF6750A4)
private val Violet80 = Color(0xFFD0BCFF)

private val LightVioletColorScheme = lightColorScheme(
    primary = Violet40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber10,
    tertiary = Teal40,
    onTertiary = Color.White,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal10,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Violet80
)

private val DarkVioletColorScheme = darkColorScheme(
    primary = Violet80,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Amber80,
    onSecondary = Amber20,
    secondaryContainer = Amber30,
    onSecondaryContainer = Amber90,
    tertiary = Teal80,
    onTertiary = Teal20,
    tertiaryContainer = Teal30,
    onTertiaryContainer = Teal90,
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Violet40
)

enum class AppAccentPalette(val persistenceKey: String, val displayName: String) {
    TEAL("teal", "Teal"),
    GREEN("green", "Green"),
    VIOLET("violet", "Violet");

    companion object {
        fun fromKey(key: String): AppAccentPalette =
            entries.find { it.persistenceKey == key } ?: TEAL
    }
}

//  Font Families
val DefaultFontFamily = FontFamily.Default

val SerifFontFamily = FontFamily.Serif

val MonoFontFamily = FontFamily.Monospace

val SansSerifFontFamily = FontFamily.SansSerif

/**
 * Available font style options for user settings.
 */
enum class AppFontStyle(val displayName: String, val fontFamily: FontFamily) {
    DEFAULT("Default", DefaultFontFamily),
    SERIF("Serif", SerifFontFamily),
    SANS_SERIF("Sans Serif", SansSerifFontFamily),
    MONOSPACE("Monospace", MonoFontFamily);

    companion object {
        fun fromName(name: String): AppFontStyle =
            entries.find { it.displayName == name } ?: DEFAULT
    }
}

/**
 * Creates typography based on selected font family.
 */
fun createTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp
    ),
    displaySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Main app theme composable with dynamic dark/light mode
 * and configurable font style.
 */
@Composable
fun LearnTogetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontStyle: AppFontStyle = AppFontStyle.DEFAULT,
    accentPalette: AppAccentPalette = AppAccentPalette.TEAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when (accentPalette) {
        AppAccentPalette.TEAL -> if (darkTheme) DarkColorScheme else LightColorScheme
        AppAccentPalette.GREEN -> if (darkTheme) DarkGreenColorScheme else LightGreenColorScheme
        AppAccentPalette.VIOLET -> if (darkTheme) DarkVioletColorScheme else LightVioletColorScheme
    }
    val typography = createTypography(fontStyle.fontFamily)

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        LaunchedEffect(darkTheme, accentPalette) {
            activity?.window?.let { window ->
                window.statusBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
