package com.caa.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caa.app.domain.model.Category
import com.caa.app.presentation.theme.fitzgeraldOf

@Composable
fun LivePictCard(
    label: String,
    imagePath: String,
    category: Category?,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    val palette = category?.let { fitzgeraldOf(it.key) }
    val accent = palette?.bg ?: MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(3.dp, accent),
        shadowElevation = 6.dp
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(10.dp).background(accent))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isPhotoPath(imagePath)) {
                    PhotoTile(
                        path = imagePath,
                        label = label,
                        accent = accent,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PictogramImage(
                        path = imagePath,
                        contentDescription = label,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp),
                        tint = accent
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val display = label.ifBlank { "ETIQUETA" }
                Text(
                    text = display.uppercase(),
                    color = if (label.isBlank()) Color(0xFFBBBBBB) else onSurface,
                    fontSize = if (label.isBlank()) 12.sp else 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 0.4.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
