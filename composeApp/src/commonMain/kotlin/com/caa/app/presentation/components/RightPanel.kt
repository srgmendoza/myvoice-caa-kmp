package com.caa.app.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.caa.app.data.repository.ArasaacRepository
import com.caa.app.domain.model.ArasaacPictogram
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.FitzgeraldKey
import com.caa.app.domain.model.Pictogram
import com.caa.app.platform.image.rememberCameraCapture
import com.caa.app.platform.image.rememberFilePicker
import com.caa.app.platform.image.rememberImagePicker
import com.caa.app.presentation.grid.PictogramFormData
import com.caa.app.presentation.theme.fitzgeraldOf
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RightPanel(
    visible: Boolean,
    editPict: Pictogram?,
    categories: List<Category>,
    allPictograms: List<Pictogram> = emptyList(),
    currentFolderId: Long? = null,
    currentDepth: Int = 0,
    maxDepth: Int = 3,
    onSave: (PictogramFormData) -> Unit,
    onDelete: (Long) -> Unit,
    onAddReferences: (folderId: Long, pictogramIds: List<Long>) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    val isEdit = editPict != null
    val initialKey = remember(editPict) { FitzgeraldKey.fromStorage(editPict?.fitzKey) ?: FitzgeraldKey.Verbs }
    val arasaacRepo: ArasaacRepository = koinInject()

    var label by remember(editPict) { mutableStateOf(editPict?.label ?: "") }
    var speech by remember(editPict) { mutableStateOf(editPict?.speech ?: "") }
    var fitzKey by remember(editPict) { mutableStateOf(initialKey) }
    var isFolder by remember(editPict) { mutableStateOf(editPict?.isFolder ?: false) }
    var imageSource by remember(editPict) { mutableStateOf(editPict?.imageSource ?: "arasaac") }
    var arasaacId by remember(editPict) { mutableStateOf(editPict?.arasaacId) }
    var customImage by remember(editPict) { mutableStateOf(editPict?.customImage) }
    var imageTab by remember { mutableStateOf("arasaac") }
    var pendingCropPath by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    // Library picker state — reset when panel opens/closes
    var selectedIds by remember(visible) { mutableStateOf<Set<Long>>(emptySet()) }
    var libSearch by remember(visible) { mutableStateOf("") }
    var libFilter by remember(visible) { mutableStateOf<FitzgeraldKey?>(null) }

    val scope = rememberCoroutineScope()
    val canSave by derivedStateOf { label.isNotBlank() && (!isFolder || selectedIds.isNotEmpty()) }
    val canBeFolder = currentDepth < maxDepth
    val palette = fitzgeraldOf(fitzKey)

    // All pictograms for the library picker — show everything available
    val displayedPicts = remember(allPictograms, libSearch, libFilter) {
        allPictograms
            .filter { libFilter == null || FitzgeraldKey.fromStorage(it.fitzKey) == libFilter }
            .filter { libSearch.isBlank() || it.label.contains(libSearch, ignoreCase = true) }
            .sortedBy { it.sortOrder }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it }
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight().width(380.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Header(editPict, onDismiss) { showDelete = true }
                HorizontalDivider(color = Color(0xFFE8EAED))

                // Content area: form always visible
                Box(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        TypeSection(isFolder = isFolder, onIsFolder = { isFolder = it && canBeFolder }, canBeFolder = canBeFolder)
                        FormField("Etiqueta", label, { label = it }, "Ej: Quiero")
                        FormField("Texto a hablar", speech, { speech = it }, "Ej: Quiero")
                        SectionLabel("Categoría")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FitzgeraldKey.entries.forEach { k ->
                                val p = fitzgeraldOf(k)
                                FitzPill(label = k.displayName, accent = p.bg, accentFg = p.fg, selected = fitzKey == k, onClick = { fitzKey = k })
                            }
                        }
                        HorizontalDivider(color = Color(0xFFE8EAED))
                        SectionLabel("Imagen")
                        TabRow(
                            selectedTabIndex = if (imageTab == "arasaac") 0 else 1,
                            containerColor = Color(0xFFF0F2F5), contentColor = MaterialTheme.colorScheme.primary, divider = {}
                        ) {
                            Tab(selected = imageTab == "arasaac", onClick = { imageTab = "arasaac" }) { Text("ARASAAC", fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp)) }
                            Tab(selected = imageTab == "photo", onClick = { imageTab = "photo" }) { Text("Galería", fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp)) }
                        }
                        when (imageTab) {
                            "arasaac" -> ArasaacSearchTab(arasaacRepo = arasaacRepo, selectedId = arasaacId, onSelect = { id ->
                                arasaacId = id; imageSource = "arasaac"
                                scope.launch { arasaacRepo.downloadImage(id)?.let { customImage = it } }
                            })
                            "photo" -> GalleryTab(customImage = customImage, onImagePicked = { path ->
                                imageSource = "photo"; arasaacId = null; pendingCropPath = path
                            })
                        }
                        Text("Sergio Palao · ARASAAC · CC BY-NC-SA", fontSize = 10.sp, color = Color(0xFFAAAAAA))

                         // Library picker — when isFolder
                         if (isFolder) {
                            HorizontalDivider(color = Color(0xFFE8EAED))
                            SectionLabel("Hijos de la carpeta")
                            Text("Selecciona pictogramas para añadir como hijos:", fontSize = 11.sp, color = Color(0xFF777777))
                            if (allPictograms.isEmpty()) {
                                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp))
                                }
                            } else {
                                LibraryContent(
                                    displayedPicts = displayedPicts, selectedIds = selectedIds,
                                    onToggle = { id -> selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id },
                                    libSearch = libSearch, onLibSearch = { libSearch = it },
                                    libFilter = libFilter, onLibFilter = { libFilter = it }
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE8EAED))
                        SectionLabel("Vista previa")
                        val previewCategory = remember(fitzKey, categories) { categories.firstOrNull { it.key == fitzKey } }
                        val previewPath = when {
                            imageTab == "photo" && customImage != null -> customImage!!
                            arasaacId != null -> "https://static.arasaac.org/pictograms/$arasaacId/${arasaacId}_300.png"
                            else -> "ic_default"
                        }
                        LivePictCard(label = label, imagePath = previewPath, category = previewCategory, isFolder = isFolder, size = 100.dp)
                    }
                }

                // Footer — single save button
                HorizontalDivider(color = Color(0xFFE8EAED))
                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = { onSave(PictogramFormData(label.trim(), speech.trim(), fitzKey, isFolder, imageSource, arasaacId, customImage, childIds = selectedIds.toList())) },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                    ) { Text(if (isEdit) "Guardar cambios" else "Añadir", fontWeight = FontWeight.Black, fontSize = 16.sp) }
                }
            }
        }
    }

    if (showDelete && isEdit) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Eliminar pictograma", fontWeight = FontWeight.Black, fontSize = 18.sp) },
            text = {
                Column {
                    Text("¿Eliminar \"${editPict!!.label}\" permanentemente?", fontSize = 14.sp)
                    if (editPict.isFolder) {
                        Spacer(Modifier.height(8.dp))
                        Text("Los pictogramas dentro de esta carpeta también se eliminarán.", fontSize = 12.sp, color = Color(0xFFE53935))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onDelete(editPict.id); showDelete = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } }
        )
    }

    pendingCropPath?.let { src ->
        CropDialog(
            sourcePath = src,
            onCancel = { pendingCropPath = null; customImage = null; imageSource = "arasaac" },
            onConfirm = { cropped -> customImage = cropped; pendingCropPath = null }
        )
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────
@Composable
private fun Header(editPict: Pictogram?, onDismiss: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Cerrar", tint = Color(0xFF666666)) }
        Column(Modifier.weight(1f)) {
            Text(if (editPict != null) "Editar pictograma" else "Añadir pictograma", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(editPict?.label ?: "Nuevo pictograma", fontSize = 12.sp, color = Color(0xFF777777))
        }
        if (editPict != null) {
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53935)),
                border = BorderStroke(1.5.dp, Color(0xFFFFCDD2))
            ) { Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp)) }
        }
    }
}

// ─── Type toggle section ─────────────────────────────────────────────────────
@Composable
private fun TypeSection(isFolder: Boolean, onIsFolder: (Boolean) -> Unit, canBeFolder: Boolean) {
    SectionLabel("Tipo")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !isFolder, onClick = { onIsFolder(false) }, label = { Text("Hoja", fontWeight = FontWeight.Bold) })
        FilterChip(selected = isFolder, onClick = { if (canBeFolder) onIsFolder(true) }, enabled = canBeFolder, label = { Text("Carpeta", fontWeight = FontWeight.Bold) })
    }
    if (!canBeFolder) {
        Surface(color = Color(0xFFFFF8E1), shape = RoundedCornerShape(8.dp)) {
            Text("Límite de anidamiento alcanzado. Este picto solo puede ser hoja.", modifier = Modifier.padding(8.dp), fontSize = 11.sp, color = Color(0xFFB87900), fontWeight = FontWeight.Bold)
        }
    }
    if (isFolder) {
        Spacer(Modifier.height(2.dp))
        Text("Las carpetas se crean vacías. Después de guardar, añade pictogramas abriendo la carpeta.", fontSize = 11.sp, color = Color(0xFF777777), lineHeight = 14.sp)
    }
}

// ─── Library picker tab ──────────────────────────────────────────────────────
@Composable
private fun LibraryContent(
    displayedPicts: List<Pictogram>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    libSearch: String, onLibSearch: (String) -> Unit,
    libFilter: FitzgeraldKey?, onLibFilter: (FitzgeraldKey?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = libSearch, onValueChange = onLibSearch,
            placeholder = { Text("Buscar...", color = Color(0xFFAAAAAA)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFFAAAAAA)) },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE8EAED), focusedBorderColor = Color(0xFF43A047), unfocusedContainerColor = Color(0xFFF8F9FA), focusedContainerColor = Color(0xFFF8F9FA))
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(selected = libFilter == null, onClick = { onLibFilter(null) }, label = { Text("Todos", fontWeight = FontWeight.Bold, fontSize = 11.sp) })
            }
            items(FitzgeraldKey.entries.toList(), key = { it.storage }) { k ->
                val p = fitzgeraldOf(k)
                FilterChip(selected = libFilter == k, onClick = { onLibFilter(k) }, label = { Text(k.displayName, fontWeight = FontWeight.Bold, fontSize = 11.sp) })
            }
        }
        if (displayedPicts.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { Text("No hay pictos disponibles", fontSize = 12.sp, color = Color(0xFF999999)) }
        } else {
            // Fixed-height grid — never nest LazyVerticalGrid inside verticalScroll
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayedPicts, key = { it.id }) { pict ->
                    val selected = pict.id in selectedIds
                    Card(
                        modifier = Modifier.aspectRatio(1f).clickable { onToggle(pict.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface),
                        border = if (selected) BorderStroke(2.dp, Color(0xFF43A047)) else BorderStroke(1.dp, Color(0xFFE8EAED))
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            com.caa.app.presentation.components.PictogramImage(
                                path = pict.imagePath, contentDescription = pict.label,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                tint = if (pict.imagePath.startsWith("ic_")) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                            )
                            if (selected) {
                                Box(Modifier.align(Alignment.TopStart).padding(2.dp).size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF43A047)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Library picker tab ──────────────────────────────────────────────────────
@Composable
private fun LibraryTabContent(
    displayedPicts: List<Pictogram>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    libSearch: String, onLibSearch: (String) -> Unit,
    libFilter: FitzgeraldKey?, onLibFilter: (FitzgeraldKey?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Search
        OutlinedTextField(
            value = libSearch, onValueChange = onLibSearch,
            placeholder = { Text("Buscar...", color = Color(0xFFAAAAAA)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFFAAAAAA)) },
            singleLine = true, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE8EAED), focusedBorderColor = Color(0xFF43A047),
                unfocusedContainerColor = Color(0xFFF8F9FA), focusedContainerColor = Color(0xFFF8F9FA)
            )
        )

        // Fitzgerald filter
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = libFilter == null,
                    onClick = { onLibFilter(null) },
                    label = { Text("Todos", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
            items(FitzgeraldKey.entries.toList(), key = { it.storage }) { k ->
                val p = fitzgeraldOf(k)
                FilterChip(
                    selected = libFilter == k,
                    onClick = { onLibFilter(k) },
                    label = { Text(k.displayName, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }

        // Grid
        if (displayedPicts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay pictos disponibles", fontSize = 12.sp, color = Color(0xFF999999))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayedPicts, key = { it.id }) { pict ->
                    val selected = pict.id in selectedIds
                    Card(
                        modifier = Modifier.aspectRatio(1f).clickable { onToggle(pict.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface),
                        border = if (selected) BorderStroke(2.dp, Color(0xFF43A047)) else BorderStroke(1.dp, Color(0xFFE8EAED))
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            com.caa.app.presentation.components.PictogramImage(
                                path = pict.imagePath,
                                contentDescription = pict.label,
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                tint = if (pict.imagePath.startsWith("ic_")) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                            )
                            if (selected) {
                                Box(Modifier.align(Alignment.TopStart).padding(2.dp).size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF43A047)),
                                    contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── ARASAAC Search Tab ─────────────────────────────────────────────────────
@Composable
private fun ArasaacSearchTab(arasaacRepo: ArasaacRepository, selectedId: Int?, onSelect: (Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ArasaacPictogram>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query, onValueChange = {
                query = it
                if (it.length >= 2) { searching = true; scope.launch { results = arasaacRepo.search(it); searching = false } }
                else results = emptyList()
            },
            placeholder = { Text("Buscar...", color = Color(0xFFAAAAAA)) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color(0xFFAAAAAA)) },
            singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE8EAED), focusedBorderColor = Color(0xFF43A047), unfocusedContainerColor = Color(0xFFF8F9FA), focusedContainerColor = Color(0xFFF8F9FA))
        )
        if (searching) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
        } else if (results.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(results, key = { it.id }) { pict ->
                    val isSelected = pict.id == selectedId
                    Box(
                        modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                            .then(if (isSelected) Modifier.border(BorderStroke(2.dp, Color(0xFF43A047)), RoundedCornerShape(10.dp)) else Modifier)
                            .clickable { onSelect(pict.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(model = pict.imageUrl300, contentDescription = pict.primaryKeyword, modifier = Modifier.fillMaxSize(0.7f), contentScale = ContentScale.Fit)
                            if (isSelected) Icon(Icons.Rounded.Check, null, tint = Color(0xFF43A047), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Text("${results.size} resultados", fontSize = 11.sp, color = Color(0xFFBBBBBB))
        } else if (query.length >= 2) Text("Sin resultados", fontSize = 12.sp, color = Color(0xFF999999))
    }
}

// ─── Gallery Tab ────────────────────────────────────────────────────────────
@Composable
private fun GalleryTab(customImage: String?, onImagePicked: (String) -> Unit) {
    val launchCamera = rememberCameraCapture { path -> onImagePicked(path) }
    val launchGallery = rememberImagePicker { path -> onImagePicked(path) }
    val launchFiles = rememberFilePicker { path -> onImagePicked(path) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (customImage != null) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                com.caa.app.presentation.components.PictogramImage(path = customImage, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (launchCamera != null) {
                OutlinedButton(onClick = { launchCamera() }, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.PhotoCamera, null, modifier = Modifier.size(18.dp)); Text("Cámara", fontSize = 11.sp) }
                }
            }
            OutlinedButton(onClick = { launchGallery() }, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(18.dp)); Text("Galería", fontSize = 11.sp) }
            }
            OutlinedButton(onClick = { launchFiles() }, modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp)); Text("Archivos", fontSize = 11.sp) }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF777777), letterSpacing = 0.6.sp)
}

@Composable
private fun FormField(label: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    SectionLabel(label)
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text(placeholder, color = Color(0xFFAAAAAA)) },
        singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFFE8EAED), focusedBorderColor = Color(0xFF43A047), unfocusedContainerColor = Color(0xFFF8F9FA), focusedContainerColor = Color(0xFFF8F9FA))
    )
}
