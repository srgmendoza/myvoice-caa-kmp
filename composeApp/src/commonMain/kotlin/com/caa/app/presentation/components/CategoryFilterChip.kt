package com.caa.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.caa.app.presentation.theme.CaaRadius
import com.caa.app.presentation.theme.CaaSpacing

@Composable
fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipPress"
    )
    val bg by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.surface,
        label = "chipBg"
    )
    val fg by animateColorAsState(
        if (selected) onAccent else MaterialTheme.colorScheme.onSurface,
        label = "chipFg"
    )

    Surface(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(CaaRadius.full))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(CaaRadius.full),
        border = BorderStroke(2.dp, if (selected) accent else MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = CaaSpacing.sp16, vertical = CaaSpacing.sp8),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1
            )
        }
    }
}
