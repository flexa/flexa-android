package com.flexa.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

class FlexaTheme(
    val useDynamicColorScheme: Boolean = true,
    val lightColorsScheme: SpendColorScheme? = null,
    val darkColorsScheme: SpendColorScheme? = null,
)

class SpendColorScheme(
    private val primary: Color? = null,
    private val onPrimary: Color? = null,
    private val primaryContainer: Color? = null,
    private val onPrimaryContainer: Color? = null,
    private val inversePrimary: Color? = null,
    private val secondary: Color? = null,
    private val onSecondary: Color? = null,
    private val secondaryContainer: Color? = null,
    private val onSecondaryContainer: Color? = null,
    private val tertiary: Color? = null,
    private val onTertiary: Color? = null,
    private val tertiaryContainer: Color? = null,
    private val onTertiaryContainer: Color? = null,
    private val error: Color? = null,
    private val onError: Color? = null,
    private val errorContainer: Color? = null,
    private val onErrorContainer: Color? = null,
    private val background: Color? = null,
    private val onBackground: Color? = null,
    private val surface: Color? = null,
    private val onSurface: Color? = null,
    private val inverseSurface: Color? = null,
    private val inverseOnSurface: Color? = null,
    private val surfaceVariant: Color? = null,
    private val onSurfaceVariant: Color? = null,
    private val outline: Color? = null,
    private val surfaceTint: Color? = null,
    private val outlineVariant: Color? = null,
    private val scrim: Color? = null,
) {
    internal fun toComposeColorScheme(colorsScheme: ColorScheme): ColorScheme =
        ColorScheme(
            primary = primary ?: colorsScheme.primary,
            onPrimary = onPrimary ?: colorsScheme.onPrimary,
            primaryContainer = primaryContainer ?: colorsScheme.primaryContainer,
            onPrimaryContainer = onPrimaryContainer ?: colorsScheme.onPrimaryContainer,
            inversePrimary = inversePrimary ?: colorsScheme.inversePrimary,
            secondary = secondary ?: colorsScheme.secondary,
            onSecondary = onSecondary ?: colorsScheme.onSecondary,
            secondaryContainer = secondaryContainer ?: colorsScheme.secondaryContainer,
            onSecondaryContainer = onSecondaryContainer ?: colorsScheme.onSecondaryContainer,
            tertiary = tertiary ?: colorsScheme.tertiary,
            onTertiary = onTertiary ?: colorsScheme.onTertiary,
            tertiaryContainer = tertiaryContainer ?: colorsScheme.tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer ?: colorsScheme.onTertiaryContainer,
            error = error ?: colorsScheme.error,
            onError = onError ?: colorsScheme.onError,
            errorContainer = errorContainer ?: colorsScheme.errorContainer,
            onErrorContainer = onErrorContainer ?: colorsScheme.onErrorContainer,
            background = background ?: colorsScheme.background,
            onBackground = onBackground ?: colorsScheme.onBackground,
            surface = surface ?: colorsScheme.surface,
            onSurface = onSurface ?: colorsScheme.onSurface,
            inverseSurface = inverseSurface ?: colorsScheme.inverseSurface,
            inverseOnSurface = inverseOnSurface ?: colorsScheme.inverseOnSurface,
            surfaceVariant = surfaceVariant ?: colorsScheme.surfaceVariant,
            onSurfaceVariant = onSurfaceVariant ?: colorsScheme.onSurfaceVariant,
            outline = outline ?: colorsScheme.outline,
            surfaceTint = surfaceTint ?: colorsScheme.surfaceTint,
            outlineVariant = outlineVariant ?: colorsScheme.outlineVariant,
            scrim = scrim ?: colorsScheme.scrim
        )
}
