package com.caa.app.presentation.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caa_kmp.composeapp.generated.resources.Res
import caa_kmp.composeapp.generated.resources.category_all
import caa_kmp.composeapp.generated.resources.grid_settings_cd
import caa_kmp.composeapp.generated.resources.grid_speaking
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.caa.app.presentation.components.CategoryFilterChip
import com.caa.app.presentation.components.ParentalGateDialog
import com.caa.app.presentation.components.PictogramCell
import com.caa.app.presentation.components.SentenceBar
import com.caa.app.presentation.components.rememberDebounce
import com.caa.app.presentation.theme.CaaSpacing
import com.caa.app.presentation.theme.fitzgeraldOf
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GridScreen(component: GridComponent) {
    val state by component.state.subscribeAsState()
    val debounce = rememberDebounce(state.debounceMs)
    var showGate by remember { mutableStateOf(false) }

    if (showGate) {
        ParentalGateDialog(
            onPass = { showGate = false; component.onParentalGate() },
            onDismiss = { showGate = false }
        )
    }

    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }

    Scaffold(
        topBar = {
            Column {
                SentenceBar(
                    sentence = state.sentence,
                    categoriesById = categoriesById,
                    onSpeak = { debounce.fire { component.onIntent(GridIntent.SpeakSentence) } },
                    onClear = { debounce.fire { component.onIntent(GridIntent.ClearSentence) } },
                    onRemoveLast = { debounce.fire { component.onIntent(GridIntent.RemoveLastFromSentence) } }
                )
                if (state.categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CaaSpacing.sp12, vertical = CaaSpacing.sp8),
                        horizontalArrangement = Arrangement.spacedBy(CaaSpacing.sp8)
                    ) {
                        item {
                            CategoryFilterChip(
                                label = stringResource(Res.string.category_all),
                                selected = state.selectedCategoryId == null,
                                accent = MaterialTheme.colorScheme.primary,
                                onAccent = MaterialTheme.colorScheme.onPrimary,
                                onClick = { component.onIntent(GridIntent.SelectCategory(null)) }
                            )
                        }
                        items(state.categories, key = { it.id }) { c ->
                            val palette = fitzgeraldOf(c.key)
                            CategoryFilterChip(
                                label = c.name,
                                selected = state.selectedCategoryId == c.id,
                                accent = palette.bg,
                                onAccent = palette.fg,
                                onClick = { component.onIntent(GridIntent.SelectCategory(c.id)) }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showGate = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(Res.string.grid_settings_cd))
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    modifier = Modifier.fillMaxSize().padding(CaaSpacing.sp8),
                    contentPadding = PaddingValues(CaaSpacing.sp4)
                ) {
                    items(state.pictograms, key = { it.id }) { p ->
                        PictogramCell(
                            pictogram = p,
                            category = p.categoryId?.let { categoriesById[it] },
                            onClick = { debounce.fire { component.onIntent(GridIntent.TapPictogram(p)) } }
                        )
                    }
                }
            }

            SpeakingOverlay(visible = state.isSpeaking)
        }
    }
}

@Composable
private fun SpeakingOverlay(visible: Boolean) {
    val accent = Color(0xFF43A047)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = accent,
                shadowElevation = 12.dp,
                modifier = Modifier.shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = accent,
                    spotColor = accent
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(Res.string.grid_speaking),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
