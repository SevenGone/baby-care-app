package com.sevengone.babycare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BabyCareColorScheme = lightColorScheme(
    primary = PeachDeep,
    secondary = Sage,
    background = WarmBackground,
    surface = Cream,
    onPrimary = Cream,
    onSecondary = Ink,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = SoftText,
    error = PeachDeep
)

@Composable
fun BabyCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BabyCareColorScheme,
        typography = BabyCareTypography,
        content = content
    )
}
