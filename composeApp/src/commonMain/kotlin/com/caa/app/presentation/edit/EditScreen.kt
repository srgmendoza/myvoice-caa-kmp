@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.caa.app.presentation.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.FitzgeraldKey
import com.caa.app.domain.model.Pictogram
import com.caa.app.platform.image.rememberCameraCapture
import com.caa.app.platform.image.rememberFilePicker
import com.caa.app.platform.image.rememberImagePicker
import com.caa.app.presentation.components.CropDialog
import com.caa.app.presentation.components.FitzPill
import com.caa.app.presentation.components.FitzTagChip
import com.caa.app.presentation.components.LivePictCard
import com.caa.app.presentation.components.MiniPictCard
import com.caa.app.presentation.components.PictogramIcons
import com.caa.app.presentation.components.PictogramImage
import com.caa.app.presentation.theme.fitzgeraldOf
import kotlinx.coroutines.launch

private val DangerColor = Color(0xFFE53935)
private val DangerLight = Color(0xFFFFF5F5)
private val DangerBorder = Color(0xFFFFCDD2)
private val WarnColor = Color(0xFFF9A825)
private val WarnLight = Color(0xFFFFF8E1)
private val WarnBorder = Color(0xFFFFE082)
private val MutedSurface = Color(0xFFF0F2F5)
private val MutedText = Color(0xFF777777)
private val SubtleBorder = Color(0xFFE8EAED)

private sealed interface FormMode {
    data object None : FormMode
    data object Add : FormMode
    data class Edit(val pict: Pictogram) : FormMode
}

@Composable
fun EditScreen(component: EditComponent) {
    val state by component.state.subscribeAsState()
    var formMode by remember { mutableStateOf<FormMode>(FormMode.None) }
    var deletePict by remember { mutableStateOf<Pictogram?>(null) }
    var filterKey by remember { mutableStateOf<FitzgeraldKey?>(null) }
    var search by remember { mutableStateOf("") }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(msg: String) = scope.launch {
        snackbarHost.currentSnackbarData?.dismiss()
        snackbarHost.showSnackbar(msg)
    }

    val categoriesByKey = remember(state.categories) {
        state.categories.groupBy { it.key }.mapValues { it.value.first() }
    }
    val categoryById = remember(state.categories) {
        state.categories.associateBy { it.id }
    }

    fun keyFor(pict: Pictogram): FitzgeraldKey =
        pict.categoryId?.let { categoryById[it]?.key } ?: FitzgeraldKey.Verbs

    fun categoryIdFor(key: FitzgeraldKey): Long? = categoriesByKey[key]?.id

    val visible = remember(state.pictograms, filterKey, search, categoryById) {
        state.pictograms
            .filter { p -> filterKey == null || keyFor(p) == filterKey }
            .filter { p -> search.isBlank() || p.label.contains(search.trim(), ignoreCase = true) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MutedSurface
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                HeaderBar(
                    total = state.pictograms.size,
                    onBack = component::onBack,
                    onSettings = component::onOpenSettings,
                    onAdd = { formMode = FormMode.Add }
                )
                FilterBar(
                    search = search,
                    onSearch = { search = it },
                    filterKey = filterKey,
                    onFilter = { filterKey = it }
                )

                if (visible.isEmpty()) {
                    EmptyState(
                        empty = state.pictograms.isEmpty(),
                        onAdd = { formMode = FormMode.Add }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    clip = false
                                ),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column {
                                    visible.forEachIndexed { i, p ->
                                        PictRow(
                                            pict = p,
                                            category = categoryById[p.categoryId],
                                            onEdit = { formMode = FormMode.Edit(p) },
                                            onDelete = { deletePict = p },
                                            isLast = i == visible.lastIndex
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Form sheet overlay
            if (formMode != FormMode.None) {
                FormSheet(
                    mode = formMode,
                    categories = state.categories,
                    onClose = { formMode = FormMode.None },
                    onSave = { label, speech, imagePath, key ->
                        when (val m = formMode) {
                            FormMode.Add -> {
                                component.onAdd(label, speech, imagePath, categoryIdFor(key))
                                toast("Pictograma añadido")
                            }
                            is FormMode.Edit -> {
                                component.onUpdate(m.pict.id, label, speech, imagePath, categoryIdFor(key))
                                toast("Pictograma actualizado")
                            }
                            FormMode.None -> {}
                        }
                        formMode = FormMode.None
                    },
                    onDelete = {
                        val m = formMode
                        if (m is FormMode.Edit) {
                            deletePict = m.pict
                            formMode = FormMode.None
                        }
                    }
                )
            }

            // Delete confirm dialog
            deletePict?.let { p ->
                DeleteDialog(
                    pict = p,
                    category = categoryById[p.categoryId],
                    onConfirm = {
                        component.onDelete(p.id)
                        toast("\"${p.label}\" eliminado")
                        deletePict = null
                    },
                    onCancel = { deletePict = null }
                )
            }
        }
    }
}

@Composable
private fun HeaderBar(
    total: Int,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SquareIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = Color(0xFF999999))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Pictogramas",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Modo padres · $total pictogramas",
                    fontSize = 12.sp,
                    color = MutedText
                )
            }
            ParentBadge()
            SquareIconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, "Ajustes", tint = Color(0xFF999999))
            }
            GradientButton(
                text = "Añadir",
                leadingIcon = Icons.Rounded.Add,
                onClick = onAdd
            )
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
            Text("Padres", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = WarnColor)
        }
    }
}

@Composable
private fun SquareIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
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
private fun GradientButton(
    text: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    fullWidth: Boolean = false,
    onClick: () -> Unit
) {
    val brush = if (enabled) {
        if (isDanger) Brush.linearGradient(listOf(DangerColor, Color(0xFFB71C1C)))
        else Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF2E7D32)))
    } else Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0)))
    val fg = if (enabled) Color.White else Color(0xFFAAAAAA)
    val mod = (if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .clip(RoundedCornerShape(14.dp))
        .background(brush)
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 12.dp)
    Row(
        modifier = mod,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = fg, modifier = Modifier.size(20.dp))
        }
        Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun FilterBar(
    search: String,
    onSearch: (String) -> Unit,
    filterKey: FitzgeraldKey?,
    onFilter: (FitzgeraldKey?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                placeholder = { Text("Buscar…", color = Color(0xFFAAAAAA)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFFAAAAAA)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = SubtleBorder,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MutedSurface,
                    focusedContainerColor = MutedSurface
                )
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FitzPill(
                        label = "Todos",
                        accent = Color(0xFF555555),
                        accentFg = Color.White,
                        selected = filterKey == null,
                        onClick = { onFilter(null) }
                    )
                }
                items(FitzgeraldKey.entries.toList(), key = { it.storage }) { k ->
                    val palette = fitzgeraldOf(k)
                    FitzPill(
                        label = k.displayName,
                        accent = palette.bg,
                        accentFg = palette.fg,
                        selected = filterKey == k,
                        onClick = { onFilter(k) },
                        showDot = true
                    )
                }
            }
        }
    }
}

@Composable
private fun PictRow(
    pict: Pictogram,
    category: Category?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MiniPictCard(pictogram = pict, category = category, size = 52.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = pict.label.uppercase(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val key = category?.key ?: FitzgeraldKey.Verbs
                val palette = fitzgeraldOf(key)
                FitzTagChip(label = key.displayName, accent = palette.bg)
                Text(
                    text = "\"${pict.speech}\"",
                    color = MutedText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        OutlinedSquareButton(
            icon = Icons.Rounded.Edit,
            tint = Color(0xFF666666),
            border = SubtleBorder,
            background = Color(0xFFF8F9FA),
            onClick = onEdit
        )
        OutlinedSquareButton(
            icon = Icons.Rounded.Delete,
            tint = DangerColor,
            border = DangerBorder,
            background = DangerLight,
            onClick = onDelete
        )
    }
    if (!isLast) {
        HorizontalDivider(color = SubtleBorder, thickness = 1.dp)
    }
}

@Composable
private fun OutlinedSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    border: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .border(1.5.dp, border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ColumnScope.EmptyState(empty: Boolean, onAdd: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MutedSurface)
                .border(2.dp, SubtleBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, null, tint = Color(0xFFCCCCCC), modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (empty) "Sin pictogramas" else "Nada coincide",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFAAAAAA)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (empty) "Añade el primero para empezar" else "Prueba con otra búsqueda o filtro",
            fontSize = 14.sp,
            color = Color(0xFFBBBBBB)
        )
        if (empty) {
            Spacer(Modifier.height(16.dp))
            GradientButton(text = "Añadir pictograma", leadingIcon = Icons.Rounded.Add, onClick = onAdd)
        }
    }
}

@Composable
private fun FormSheet(
    mode: FormMode,
    categories: List<Category>,
    onClose: () -> Unit,
    onSave: (label: String, speech: String, imagePath: String, key: FitzgeraldKey) -> Unit,
    onDelete: () -> Unit
) {
    val isEdit = mode is FormMode.Edit
    val initial = (mode as? FormMode.Edit)?.pict
    val initialKey = remember(mode, categories) {
        initial?.categoryId?.let { id -> categories.firstOrNull { it.id == id }?.key } ?: FitzgeraldKey.Verbs
    }

    var label by remember(initial) { mutableStateOf(initial?.label ?: "") }
    var speech by remember(initial) { mutableStateOf(initial?.speech ?: "") }
    var imagePath by remember(initial) { mutableStateOf(initial?.imagePath ?: "ic_default") }
    var fitzKey by remember(initial) { mutableStateOf(initialKey) }
    var pendingCropPath by remember(initial) { mutableStateOf<String?>(null) }

    val canSave = label.isNotBlank() && speech.isNotBlank()
    val palette = fitzgeraldOf(fitzKey)
    val fakeCategory = remember(fitzKey, categories) {
        categories.firstOrNull { it.key == fitzKey } ?: Category(0, "", "#000000", 0, fitzKey)
    }

    // Backdrop dismiss
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(onClick = onClose)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.fillMaxSize()) {
            // Sheet header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SquareIconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, "Cerrar", tint = Color(0xFF666666))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isEdit) "Editar pictograma" else "Nuevo pictograma",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (isEdit) "Editando: ${initial?.label.orEmpty()}" else "Añadir al tablero",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                }
                if (isEdit) {
                    OutlinedSquareButton(
                        icon = Icons.Rounded.Delete,
                        tint = DangerColor,
                        border = DangerBorder,
                        background = DangerLight,
                        onClick = onDelete
                    )
                }
            }
            HorizontalDivider(color = SubtleBorder)

            // Form body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Preview + fields
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LivePictCard(
                            label = label,
                            imagePath = imagePath,
                            category = fakeCategory,
                            size = 120.dp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Vista previa",
                            fontSize = 11.sp,
                            color = MutedText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FormField(
                            label = "Etiqueta",
                            value = label,
                            onChange = { label = it },
                            placeholder = "ej. Mamá",
                            helper = "Texto visible en la tarjeta"
                        )
                        FormField(
                            label = "Texto a hablar",
                            value = speech,
                            onChange = { speech = it },
                            placeholder = "ej. Mamá",
                            helper = "Lo que el dispositivo dice"
                        )
                    }
                }

                // Fitz selector
                FormSectionLabel("Categoría Fitzgerald Key")
                FlowRowSpaced {
                    FitzgeraldKey.entries.forEach { k ->
                        val p = fitzgeraldOf(k)
                        FitzPill(
                            label = k.displayName,
                            accent = p.bg,
                            accentFg = p.fg,
                            selected = fitzKey == k,
                            onClick = { fitzKey = k }
                        )
                    }
                }

                // Icon selector
                FormSectionLabel("Icono del pictograma")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PictogramIcons.keys(), key = { it }) { key ->
                        val active = imagePath == key
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) palette.bg else MutedSurface)
                                .border(
                                    width = 2.5.dp,
                                    color = if (active) palette.bg else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { imagePath = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PictogramIcons.get(key),
                                contentDescription = key,
                                modifier = Modifier.size(36.dp),
                                tint = if (active) palette.fg else Color(0xFFAAAAAA)
                            )
                        }
                    }
                }

                // Photo area
                FormSectionLabel("O usar foto real")
                val launchGallery = rememberImagePicker { newPath -> pendingCropPath = newPath }
                val launchFiles = rememberFilePicker { newPath -> pendingCropPath = newPath }
                val launchCamera = rememberCameraCapture { newPath -> pendingCropPath = newPath }
                val isPhoto = !imagePath.startsWith("ic_")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isPhoto) Color(0xFFE8F5E9) else Color(0xFFFAFAFA))
                        .border(
                            width = 2.dp,
                            color = if (isPhoto) MaterialTheme.colorScheme.primary else SubtleBorder,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isPhoto) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFC8E6C9)),
                                contentAlignment = Alignment.Center
                            ) {
                                PictogramImage(
                                    path = imagePath,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column {
                                Text(
                                    "Foto seleccionada",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Toca para cambiar",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    } else {
                        Icon(
                            Icons.Rounded.PhotoCamera,
                            null,
                            tint = Color(0xFFCCCCCC),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (launchCamera != null) "Cámara · Galería · Archivos" else "Galería · Archivos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBBBBBB)
                        )
                        Text(
                            "JPG, PNG, HEIC",
                            fontSize = 11.sp,
                            color = Color(0xFFCCCCCC)
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (launchCamera != null) {
                            OutlinedButton(onClick = { launchCamera() }) {
                                Icon(Icons.Rounded.PhotoCamera, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cámara", fontSize = 13.sp)
                            }
                        }
                        OutlinedButton(onClick = { launchGallery() }) {
                            Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Galería", fontSize = 13.sp)
                        }
                        OutlinedButton(onClick = { launchFiles() }) {
                            Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Archivos", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Footer CTA
            HorizontalDivider(color = SubtleBorder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp)
            ) {
                GradientButton(
                    text = if (isEdit) "Guardar cambios" else "Añadir pictograma",
                    leadingIcon = if (isEdit) Icons.Rounded.Check else Icons.Rounded.Add,
                    enabled = canSave,
                    fullWidth = true,
                    onClick = { onSave(label.trim(), speech.trim(), imagePath, fitzKey) }
                )
            }
        }
    }

    pendingCropPath?.let { src ->
        CropDialog(
            sourcePath = src,
            onCancel = { pendingCropPath = null },
            onConfirm = { cropped ->
                imagePath = cropped
                pendingCropPath = null
            }
        )
    }
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MutedText,
        letterSpacing = 0.6.sp
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    helper: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FormSectionLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = Color(0xFFAAAAAA)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SubtleBorder,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
        Text(helper, fontSize = 11.sp, color = MutedText)
    }
}

@Composable
private fun FlowRowSpaced(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun DeleteDialog(
    pict: Pictogram,
    category: Category?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DangerLight)
                        .border(1.5.dp, DangerBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = DangerColor, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "¿Eliminar pictograma?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Esta acción no se puede deshacer",
                        fontSize = 12.sp,
                        color = MutedText
                    )
                }
            }
        },
        text = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFAFAFA),
                border = BorderStroke(1.dp, SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniPictCard(pictogram = pict, category = category, size = 48.dp)
                    Column {
                        Text(
                            pict.label.uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "\"${pict.speech}\"",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = "Eliminar",
                isDanger = true,
                onClick = onConfirm
            )
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MutedSurface)
            ) {
                Text("Cancelar", color = MutedText, fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}
