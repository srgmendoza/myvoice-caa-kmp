package com.caa.app.presentation.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caa_kmp.composeapp.generated.resources.Res
import caa_kmp.composeapp.generated.resources.action_back
import caa_kmp.composeapp.generated.resources.action_settings
import caa_kmp.composeapp.generated.resources.category_all
import caa_kmp.composeapp.generated.resources.edit_parent_badge
import caa_kmp.composeapp.generated.resources.grid_settings_cd
import caa_kmp.composeapp.generated.resources.grid_speaking
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.caa.app.domain.model.FitzgeraldKey
import com.caa.app.domain.model.Pictogram
import com.caa.app.presentation.components.CategoryFilterChip
import com.caa.app.presentation.components.ParentalGateDialog
import com.caa.app.presentation.components.PictogramCell
import com.caa.app.presentation.components.RightPanel
import com.caa.app.presentation.components.SentenceBar
import com.caa.app.presentation.components.rememberDebounce
import com.caa.app.presentation.theme.CaaSpacing
import com.caa.app.presentation.theme.FitzgeraldPalette
import com.caa.app.presentation.theme.fitzgeraldOf
import org.jetbrains.compose.resources.stringResource

private val DangerColor = Color(0xFFE53935)
private val DangerLight = Color(0xFFFFF5F5)
private val DangerBorder = Color(0xFFFFCDD2)
private val WarnColor = Color(0xFFF9A825)
private val WarnLight = Color(0xFFFFF8E1)
private val WarnBorder = Color(0xFFFFE082)
private val MutedSurface = Color(0xFFF0F2F5)
private val MutedText = Color(0xFF777777)
private val SubtleBorder = Color(0xFFE8EAED)
private val Accent = Color(0xFF43A047)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GridScreen(component: GridComponent) {
    val state by component.state.subscribeAsState()
    val debounce = rememberDebounce(state.debounceMs)
    var showGate by remember { mutableStateOf(false) }

    // Right panel state
    var panelPict by remember { mutableStateOf<Pictogram?>(null) }
    val panelVisible = panelPict !== null || false

    if (showGate) {
        ParentalGateDialog(
            onPass = { showGate = false; component.onParentalGate() },
            onDismiss = { showGate = false }
        )
    }

    val categoriesById = remember(state.categories) { state.categories.associateBy { it.id } }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (state.isParentMode) {
                    val isRoot = state.folderStack.size <= 1
                    ParentToolbar(
                        folderStack = state.folderStack,
                        folderPath = state.folderPath,
                        onBack = {
                            if (isRoot) component.onIntent(GridIntent.ToggleParentMode)
                            else component.onIntent(GridIntent.NavigateBack)
                        },
                        onHome = { component.onIntent(GridIntent.NavigateHome) },
                        onAdd = { panelPict = null },
                        onSettings = { component.onIntent(GridIntent.OpenSettings) },
                        isRoot = isRoot
                    )
                } else {
                    Column {
                        SentenceBar(
                            sentence = state.sentence,
                            categoriesById = categoriesById,
                            onSpeak = { debounce.fire { component.onIntent(GridIntent.SpeakSentence) } },
                            onClear = { debounce.fire { component.onIntent(GridIntent.ClearSentence) } },
                            onRemoveLast = { debounce.fire { component.onIntent(GridIntent.RemoveLastFromSentence) } }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!state.isParentMode) {
                    FloatingActionButton(onClick = { showGate = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(Res.string.grid_settings_cd))
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Column(Modifier.fillMaxSize()) {
                    // Category filter
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
                            items(state.pictograms, key = { it.id }) { pict ->
                                val palette: FitzgeraldPalette? = pict.fitzKey.let { fitzgeraldOf(FitzgeraldKey.fromStorage(it)) }

                                if (state.isParentMode) {
                                    ParentPictCard(
                                        pict = pict,
                                        palette = palette,
                                        onTap = {
                                            if (pict.isFolder) {
                                                component.onIntent(GridIntent.NavigateToFolder(pict.id))
                                            } else {
                                                component.onIntent(GridIntent.TapPictogram(pict))
                                            }
                                        },
                                        onEdit = { panelPict = pict },
                                        onDelete = { component.onIntent(GridIntent.DeletePictogram(pict.id)) }
                                    )
                                } else {
                                    PictogramCell(
                                        pictogram = pict,
                                        category = pict.categoryId?.let { categoriesById[it] },
                                        onClick = { debounce.fire { component.onIntent(GridIntent.TapPictogram(pict)) } }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!state.isParentMode) {
                    SpeakingOverlay(visible = state.isSpeaking)
                }
            }
        }

        // Right panel overlay
        if (panelVisible) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).clickable { panelPict = null },
                contentAlignment = Alignment.CenterEnd
            ) {
                RightPanel(
                    visible = true,
                    editPict = panelPict,
                    categories = state.categories,
                    onSave = { form ->
                        component.onIntent(GridIntent.SavePictogram(form, editId = panelPict?.id))
                        panelPict = null
                    },
                    onDelete = { id ->
                        component.onIntent(GridIntent.DeletePictogram(id))
                        panelPict = null
                    },
                    onDismiss = { panelPict = null }
                )
            }
        }
    }
}

// ─── Parent toolbar ─────────────────────────────────────────────────────────
@Composable
private fun ParentToolbar(
    folderStack: List<Long?>,
    folderPath: List<Pictogram>,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit,
    isRoot: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isRoot) {
                    SquareIcon(onClick = onHome) { Icon(Icons.Rounded.Home, null, tint = Color(0xFF999999)) }
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.action_back), tint = Color(0xFF999999))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isRoot) "Inicio" else folderPath.lastOrNull()?.label?.uppercase() ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isRoot) {
                        Text(
                            folderPath.joinToString(" › ") { it.label.lowercase() },
                            fontSize = 12.sp,
                            color = MutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                ParentBadge()
                SquareIcon(onClick = onAdd) { Icon(Icons.Rounded.Add, "Añadir", tint = Accent) }
                SquareIcon(onClick = onSettings) { Icon(Icons.Rounded.Settings, null, tint = Color(0xFF999999)) }
            }
            HorizontalDivider(color = SubtleBorder)
        }
    }
}

@Composable
private fun ParentBadge() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = WarnLight,
        border = BorderStroke(1.5.dp, WarnBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Rounded.Lock, null, tint = WarnColor, modifier = Modifier.size(14.dp))
            Text(stringResource(Res.string.edit_parent_badge), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = WarnColor)
        }
    }
}

// ─── Parent mode card ───────────────────────────────────────────────────────
@Composable
private fun ParentPictCard(
    pict: Pictogram,
    palette: FitzgeraldPalette?,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = palette?.bg ?: Accent
    val onSurface = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(CaaSpacing.sp6),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, accent)
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // Fitzgerald bar on TOP
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(accent)
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        com.caa.app.presentation.components.PictogramImage(
                            path = pict.imagePath,
                            contentDescription = pict.label,
                            modifier = Modifier.fillMaxSize(),
                            tint = if (pict.imagePath.startsWith("ic_")) onSurface else Color.Unspecified
                        )
                    }
                }
                Text(
                    text = pict.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(2.dp))
            }

            if (pict.isFolder) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowRight, null, tint = (palette?.fg ?: Color.White), modifier = Modifier.size(14.dp))
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.align(Alignment.BottomStart).size(32.dp)) {
                Icon(Icons.Rounded.Edit, "Editar", tint = Color(0xFF666666), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.BottomEnd).size(32.dp)) {
                Icon(Icons.Rounded.Delete, "Eliminar", tint = DangerColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ─── Shared ──────────────────────────────────────────────────────────────────
@Composable
private fun SquareIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MutedSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
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
            modifier = Modifier.fillMaxSize().background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(shape = RoundedCornerShape(28.dp), color = accent, shadowElevation = 12.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Text(stringResource(Res.string.grid_speaking), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
