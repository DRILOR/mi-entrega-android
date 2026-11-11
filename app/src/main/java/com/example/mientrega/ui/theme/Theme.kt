package com.example.mientrega.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Tema oscuro basado en la paleta del logo:
 * morado para acciones principales, verde para estados de éxito y naranja
 * para alertas de vencimiento.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PurpleVibrant,
    onPrimary = Color.White,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = OnPurpleContainer,
    secondary = EmeraldGreen,
    onSecondary = Color(0xFF003915),
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = OnEmeraldContainer,
    tertiary = OrangeAlert,
    onTertiary = Color(0xFF2E1B00),
    tertiaryContainer = OrangeContainer,
    onTertiaryContainer = OnOrangeContainer,
    background = DeepBlueBackground,
    onBackground = OnDarkText,
    surface = BlueGraySurface,
    onSurface = OnDarkText,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = OnDarkMuted,
    surfaceContainerLowest = SurfaceLowest,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer = BlueGraySurface,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHighest,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

/** Tema claro existente, sin cambios de paleta. */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4743C7),
    secondary = Color(0xFF087F8C),
    background = Color(0xFFF8F8FF)
)

@Composable
fun MiEntregaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
