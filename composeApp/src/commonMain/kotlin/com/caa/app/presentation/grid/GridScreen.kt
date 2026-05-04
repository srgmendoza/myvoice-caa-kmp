package com.caa.app.presentation.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.caa.app.presentation.components.CategoryFilterChip
import com.caa.app.presentation.components.ParentalGateDialog
import com.caa.app.presentation.components.PictogramCell
import com.caa.app.presentation.components.SentenceBar
import com.caa.app.presentation.components.rememberDebounce
import com.caa.app.presentation.theme.CaaSpacing
import com.caa.app.presentation.theme.fitzgeraldOf

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
                                label = "Todos",
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
                Icon(Icons.Rounded.Settings, contentDescription = "Configuración")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxSize().padding(padding).padding(CaaSpacing.sp8),
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
    }
}
