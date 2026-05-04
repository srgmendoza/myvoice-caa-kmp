package com.caa.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun CaaTheme(useDark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CaaLightColorScheme,
        typography = caaTypography(),
        content = content
    )
}
