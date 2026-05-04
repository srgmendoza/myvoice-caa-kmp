package com.caa.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.Pictogram
import com.caa.app.presentation.theme.fitzgeraldOf

@Composable
fun MiniPictCard(
    pictogram: Pictogram,
    category: Category?,
    size: Dp = 52.dp,
    modifier: Modifier = Modifier
) {
    val palette = category?.let { fitzgeraldOf(it.key) }
    val accent = palette?.bg ?: MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(3.dp, accent)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(7.dp).background(accent))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                PictogramImage(
                    path = pictogram.imagePath,
                    contentDescription = pictogram.label,
                    modifier = Modifier.fillMaxSize(),
                    tint = if (pictogram.imagePath.startsWith("ic_")) accent else Color.Unspecified
                )
            }
        }
    }
}
