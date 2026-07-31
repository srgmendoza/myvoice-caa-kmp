package com.caa.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caa_kmp.composeapp.generated.resources.Res
import caa_kmp.composeapp.generated.resources.sentence_clear_cd
import caa_kmp.composeapp.generated.resources.sentence_placeholder
import caa_kmp.composeapp.generated.resources.sentence_remove_last_cd
import caa_kmp.composeapp.generated.resources.sentence_speak_cd
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.Pictogram
import com.caa.app.presentation.theme.CaaElevation
import com.caa.app.presentation.theme.CaaRadius
import com.caa.app.presentation.theme.CaaSpacing
import com.caa.app.presentation.theme.fitzgeraldOf
import org.jetbrains.compose.resources.stringResource

@Composable
fun SentenceBar(
    sentence: List<Pictogram>,
    categoriesById: Map<Long, Category>,
    onSpeak: () -> Unit,
    onClear: () -> Unit,
    onRemoveLast: () -> Unit,
    onTokenClick: (Pictogram) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = CaaElevation.low,
        shadowElevation = CaaElevation.low
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CaaSpacing.sp12, vertical = CaaSpacing.sp10)
                .heightIn(min = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(CaaSpacing.sp8)
        ) {
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = 84.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(CaaRadius.lg)
            ) {
                if (sentence.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.sentence_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CaaSpacing.sp8),
                        horizontalArrangement = Arrangement.spacedBy(CaaSpacing.sp8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(sentence, key = { idx, p -> "${idx}_${p.id}" }) { _, p ->
                            SentenceToken(
                                pictogram = p,
                                category = p.categoryId?.let { categoriesById[it] },
                                onClick = { onTokenClick(p) }
                            )
                        }
                    }
                }
            }

            SentenceIconButton(
                icon = { Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = stringResource(Res.string.sentence_remove_last_cd)) },
                onClick = onRemoveLast,
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurface,
                size = 56.dp
            )
            SentenceIconButton(
                icon = { Icon(Icons.Rounded.Delete, contentDescription = stringResource(Res.string.sentence_clear_cd)) },
                onClick = onClear,
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurface,
                size = 56.dp
            )
            SentenceIconButton(
                icon = { Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = stringResource(Res.string.sentence_speak_cd)) },
                onClick = onSpeak,
                container = MaterialTheme.colorScheme.primary,
                content = MaterialTheme.colorScheme.onPrimary,
                size = 72.dp
            )
        }
    }
}

@Composable
private fun SentenceToken(
    pictogram: Pictogram,
    category: Category?,
    onClick: () -> Unit
) {
    val palette = category?.let { fitzgeraldOf(it.key) }
    val accent = palette?.bg ?: MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Instant pressed-state feedback, no animation (kid-first constraint).
    val scale = if (pressed) 0.94f else 1f

    Surface(
        modifier = Modifier
            .height(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(CaaRadius.md))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(CaaRadius.md),
        border = BorderStroke(2.dp, accent)
    ) {
        Column(Modifier.fillMaxHeight().widthIn(min = 64.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = CaaSpacing.sp8, vertical = CaaSpacing.sp4),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PictogramImage(
                        path = pictogram.imagePath,
                        contentDescription = pictogram.label,
                        modifier = Modifier.size(36.dp),
                        tint = if (pictogram.imagePath.startsWith("ic_")) onSurface else Color.Unspecified
                    )
                    Spacer(Modifier.width(CaaSpacing.sp6))
                    Text(
                        text = pictogram.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CaaSpacing.sp6)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun SentenceIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    size: androidx.compose.ui.unit.Dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Instant pressed-state feedback, no animation (kid-first constraint).
    val scale = if (pressed) 0.92f else 1f
    FilledIconButton(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(CaaRadius.full),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        icon()
    }
}
