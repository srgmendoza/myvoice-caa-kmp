package com.caa.app.presentation.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.caa.app.domain.model.FitzgeraldKey

object CaaColors {
    val Primary = Color(0xFF43A047)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFC8E6C9)
    val OnPrimaryContainer = Color(0xFF1B5E20)

    val Secondary = Color(0xFFFF8C00)
    val OnSecondary = Color(0xFFFFFFFF)

    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF111111)
    val SurfaceVariant = Color(0xFFF0F2F5)
    val OnSurfaceVariant = Color(0xFF666666)

    val Error = Color(0xFFE53935)
    val OnError = Color(0xFFFFFFFF)

    val Outline = Color(0xFFE8EAED)
}

val CaaLightColorScheme = lightColorScheme(
    primary = CaaColors.Primary,
    onPrimary = CaaColors.OnPrimary,
    primaryContainer = CaaColors.PrimaryContainer,
    onPrimaryContainer = CaaColors.OnPrimaryContainer,
    secondary = CaaColors.Secondary,
    onSecondary = CaaColors.OnSecondary,
    background = CaaColors.SurfaceVariant,
    onBackground = CaaColors.OnSurface,
    surface = CaaColors.Surface,
    onSurface = CaaColors.OnSurface,
    surfaceVariant = CaaColors.SurfaceVariant,
    onSurfaceVariant = CaaColors.OnSurfaceVariant,
    error = CaaColors.Error,
    onError = CaaColors.OnError,
    outline = CaaColors.Outline
)

// Fitzgerald Key — fixed AAC palette. Bar 10dp + tile border + sentence token border use these.
data class FitzgeraldPalette(val bg: Color, val fg: Color)

val FitzgeraldColors: Map<FitzgeraldKey, FitzgeraldPalette> = mapOf(
    FitzgeraldKey.People to FitzgeraldPalette(Color(0xFFF5C842), Color(0xFF1A1100)),
    FitzgeraldKey.Verbs to FitzgeraldPalette(Color(0xFF4CAF50), Color(0xFFFFFFFF)),
    FitzgeraldKey.Nouns to FitzgeraldPalette(Color(0xFFFF8C00), Color(0xFFFFFFFF)),
    FitzgeraldKey.Desc to FitzgeraldPalette(Color(0xFF4A90D9), Color(0xFFFFFFFF)),
    FitzgeraldKey.Social to FitzgeraldPalette(Color(0xFF9B59B6), Color(0xFFFFFFFF)),
    FitzgeraldKey.Food to FitzgeraldPalette(Color(0xFFE74C3C), Color(0xFFFFFFFF))
)

fun fitzgeraldOf(key: FitzgeraldKey): FitzgeraldPalette =
    FitzgeraldColors.getValue(key)
