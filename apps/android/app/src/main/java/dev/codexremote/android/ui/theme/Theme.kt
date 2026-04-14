package dev.codexremote.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CodexBlueDark,
    onPrimary = Color(0xFF10213D),
    primaryContainer = CodexCurrentCardDark,
    onPrimaryContainer = Color(0xFFD9E7FF),
    secondary = Color(0xFF9BB0CC),
    onSecondary = Color(0xFF101929),
    secondaryContainer = CodexSurfaceSoftDark,
    onSecondaryContainer = Color(0xFFE2EBF8),
    tertiary = Color(0xFF84C3FF),
    onTertiary = Color(0xFF0E1D33),
    tertiaryContainer = CodexSurfaceSubtleDark,
    onTertiaryContainer = Color(0xFFE5F3FF),
    surface = CodexSurfaceDark,
    onSurface = Color(0xFFE6EDF8),
    surfaceVariant = CodexSurfaceRaisedDark,
    onSurfaceVariant = Color(0xFFB3C1D5),
    background = CodexBackgroundDark,
    onBackground = Color(0xFFE6EDF8),
    outline = CodexOutlineDark,
    outlineVariant = Color(0xFF273448),
    inverseSurface = Color(0xFFE6EDF8),
    inverseOnSurface = Color(0xFF121A2A),
    inversePrimary = CodexBlueLight,
    surfaceTint = CodexBlueDark,
    error = CodexOffline,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF4A1515),
    onErrorContainer = Color(0xFFFFDADA),
)

private val LightColorScheme = lightColorScheme(
    primary = CodexBlueLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CodexCurrentCardLight,
    onPrimaryContainer = Color(0xFF0E2446),
    secondary = Color(0xFF5D738F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = CodexSurfaceSoftLight,
    onSecondaryContainer = Color(0xFF20314D),
    tertiary = Color(0xFF5C95D9),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3EEFF),
    onTertiaryContainer = Color(0xFF12315A),
    surface = CodexSurface,
    onSurface = Color(0xFF152033),
    surfaceVariant = CodexSurfaceRaised,
    onSurfaceVariant = Color(0xFF46566F),
    background = CodexBackgroundLight,
    onBackground = Color(0xFF0E1726),
    outline = CodexOutlineLight,
    outlineVariant = Color(0xFFCBD7E6),
    inverseSurface = Color(0xFF182133),
    inverseOnSurface = Color(0xFFF4F8FF),
    inversePrimary = CodexBlueDark,
    surfaceTint = CodexBlueLight,
    error = CodexOffline,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDCDC),
    onErrorContainer = Color(0xFF7A1717),
)

@Composable
fun CodexRemoteTheme(
    themePreference: ThemePreference = ThemePreference.AUTO,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (themePreference.isDarkNow()) DarkColorScheme else LightColorScheme,
        typography = CodexTypography,
        content = content,
    )
}
