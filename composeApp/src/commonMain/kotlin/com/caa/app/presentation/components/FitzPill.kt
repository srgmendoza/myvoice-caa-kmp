package com.caa.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FitzPill(
    label: String,
    accent: Color,
    accentFg: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDot: Boolean = false
) {
    val bg = if (selected) accent else Color(0xFFEEEEEE)
    val fg = if (selected) accentFg else Color(0xFF777777)
    val borderColor = if (selected) accent else Color.Transparent

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (showDot) {
                Box(
                    Modifier.size(8.dp).clip(CircleShape).background(if (selected) accentFg else accent)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun FitzTagChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accent.copy(alpha = 0.13f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.55f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}
