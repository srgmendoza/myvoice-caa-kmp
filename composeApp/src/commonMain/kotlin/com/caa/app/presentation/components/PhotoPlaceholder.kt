package com.caa.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

fun isPhotoPath(path: String): Boolean = !path.startsWith("ic_")

// Renders a real photo cropped square to fill the tile uniformly.
// Falls back to the striped placeholder if the image fails to load.
@Composable
fun PhotoTile(
    path: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    var failed by remember(path) { mutableStateOf(false) }
    Box(modifier = modifier) {
        if (failed) {
            PhotoStripedFill(accent = accent, modifier = Modifier.fillMaxSize())
        } else {
            AsyncImage(
                model = path,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { failed = true }
            )
        }
    }
}

// Striped placeholder shown only when no photo loads (fallback / empty state).
@Composable
fun PhotoStripedFill(
    accent: Color,
    modifier: Modifier = Modifier
) {
    val stripe = Brush.linearGradient(
        colorStops = arrayOf(
            0f to accent.copy(alpha = 0.55f),
            0.5f to accent.copy(alpha = 0.55f),
            0.5f to accent.copy(alpha = 0.20f),
            1f to accent.copy(alpha = 0.20f)
        ),
        start = Offset.Zero,
        end = Offset(14f, 14f),
        tileMode = TileMode.Repeated
    )
    Box(modifier = modifier.background(stripe)) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.55f).align(Alignment.Center),
            tint = accent.copy(alpha = 0.85f)
        )
    }
}
