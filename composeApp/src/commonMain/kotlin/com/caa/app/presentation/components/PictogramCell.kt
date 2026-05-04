package com.caa.app.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.Pictogram
import com.caa.app.presentation.theme.CaaElevation
import com.caa.app.presentation.theme.CaaRadius
import com.caa.app.presentation.theme.CaaSpacing
import com.caa.app.presentation.theme.FitzgeraldPalette
import com.caa.app.presentation.theme.fitzgeraldOf

@Composable
fun PictogramCell(
    pictogram: Pictogram,
    category: Category?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette: FitzgeraldPalette? = category?.let { fitzgeraldOf(it.key) }
    val accent = palette?.bg ?: MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tilePress"
    )

    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .aspectRatio(1f)
            .padding(CaaSpacing.sp6)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(CaaRadius.lg),
        colors = CardDefaults.cardColors(containerColor = surface, contentColor = onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = CaaElevation.medium),
        border = BorderStroke(2.dp, accent)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(CaaSpacing.sp10),
                contentAlignment = Alignment.Center
            ) {
                PictogramImage(
                    path = pictogram.imagePath,
                    contentDescription = pictogram.label,
                    modifier = Modifier.fillMaxSize(0.78f),
                    tint = if (pictogram.imagePath.startsWith("ic_")) onSurface else Color.Unspecified
                )
            }
            Text(
                text = pictogram.label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CaaSpacing.sp8, vertical = CaaSpacing.sp4)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CaaSpacing.sp10)
                    .clip(RoundedCornerShape(bottomStart = CaaRadius.lg, bottomEnd = CaaRadius.lg))
                    .background(accent)
            )
        }
    }
}

