package com.caa.app.presentation.grid

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.caa.app.domain.model.FitzgeraldKey
import com.caa.app.domain.model.Pictogram
import com.caa.app.domain.repository.PictogramRepository
import com.caa.app.domain.repository.SettingsRepository
import com.caa.app.platform.tts.SpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

interface GridComponent {
    val state: Value<GridState>
    fun onIntent(intent: GridIntent)
    fun onParentalGate()
}

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGridComponent(
    componentContext: ComponentContext,
    private val repository: PictogramRepository,
    private val settings: SettingsRepository,
    private val speech: SpeechEngine,
    private val onOpenSettings: () -> Unit,
    private val onOpenParentMode: (() -> Unit)? = null,
    private val onExitParentMode: (() -> Unit)? = null,
    private val isParentMode: Boolean = false
) : GridComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main)
    private val _state = MutableValue(GridState(isParentMode = isParentMode))
    override val state: Value<GridState> = _state

    private val selectedCategory = MutableStateFlow<Long?>(null)
    private val folderVersion = MutableStateFlow(0)
    private var sentenceSpeakActive = false

    init {
        scope.launch { repository.seedDefaults() }

        scope.launch {
            combine(selectedCategory, folderVersion) { catId, _ ->
                catId to _state.value.currentFolderId
            }.flatMapLatest { (catId, folderId) ->
                when {
                    catId != null && folderId != null -> repository.observeChildrenByCategory(folderId, catId)
                    catId != null -> repository.observeByCategory(catId)
                    else -> repository.observeChildren(folderId)
                }
            }.collectLatest { pictos ->
                _state.update { it.copy(pictograms = pictos) }
            }
        }

        scope.launch {
            repository.observeCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }

        scope.launch {
            repository.observeAll().collect { all ->
                _state.update { it.copy(allPictograms = all) }
            }
        }

        scope.launch {
            settings.observe().collect { s ->
                _state.update { it.copy(debounceMs = s.debounceMs, isLoading = false) }
            }
        }

        scope.launch {
            speech.isSpeaking.collect { speaking ->
                if (!speaking) sentenceSpeakActive = false
                _state.update { it.copy(isSpeaking = speaking && sentenceSpeakActive) }
            }
        }
    }

    override fun onIntent(intent: GridIntent) {
        when (intent) {
            is GridIntent.TapPictogram -> {
                speech.speak(intent.pictogram.speech)
                if (intent.pictogram.isFolder) {
                    // Folders navigate only: audible feedback but no sentence token.
                    navigateToFolder(intent.pictogram)
                } else {
                    _state.update { it.copy(sentence = it.sentence + intent.pictogram) }
                }
            }
            GridIntent.SpeakSentence -> {
                val text = _state.value.sentence.joinToString(" ") { it.speech }
                if (text.isNotBlank()) {
                    sentenceSpeakActive = true
                    speech.speak(text)
                }
            }
            GridIntent.ClearSentence -> _state.update { it.copy(sentence = emptyList()) }
            GridIntent.RemoveLastFromSentence -> _state.update {
                it.copy(sentence = it.sentence.dropLast(1))
            }
            is GridIntent.SelectCategory -> {
                selectedCategory.value = intent.id
                _state.update { it.copy(selectedCategoryId = intent.id) }
            }
            is GridIntent.NavigateToFolder -> {
                val folder = _state.value.pictograms.firstOrNull { it.id == intent.folderId }
                if (folder != null) navigateToFolder(folder)
            }
            GridIntent.NavigateBack -> {
                if (_state.value.folderStack.size > 1) {
                    _state.update {
                        val newStack = it.folderStack.dropLast(1)
                        val newPath = it.folderPath.dropLast(1)
                        it.copy(folderStack = newStack, folderPath = newPath)
                    }
                    folderVersion.value++
                }
            }
            GridIntent.NavigateHome -> {
                _state.update { it.copy(folderStack = listOf(null), folderPath = emptyList()) }
                folderVersion.value++
            }
            GridIntent.OpenSettings -> {
                onOpenSettings()
            }
            GridIntent.ToggleParentMode -> {
                onExitParentMode?.invoke()
            }
            is GridIntent.EditPictogram -> {
                // Handled by the screen
            }
            GridIntent.AddPictogram -> {
                // Handled by the screen
            }
            is GridIntent.AddExistingReferences -> {
                scope.launch {
                    repository.addReferencesToFolder(intent.folderId, intent.pictogramIds)
                    folderVersion.value++
                }
            }
            is GridIntent.SavePictogram -> {
                scope.launch {
                    val folderId = _state.value.currentFolderId
                    val form = intent.form
                    if (intent.editId != null) {
                        // Fetch the persisted entity (not the filtered visible list) and
                        // copy only form-edited fields, preserving categoryId, colorHex,
                        // iconKey, sortOrder and parentId.
                        val existing = repository.observeAll().first()
                            .firstOrNull { it.id == intent.editId }
                        if (existing != null) {
                            repository.update(
                                existing.copy(
                                    label = form.label,
                                    speech = form.speech,
                                    isFolder = form.isFolder,
                                    imageSource = form.imageSource,
                                    arasaacId = form.arasaacId,
                                    customImage = form.customImage,
                                    fitzKey = form.fitzKey.storage
                                )
                            )
                            if (form.childIds.isNotEmpty()) {
                                repository.addReferencesToFolder(intent.editId, form.childIds)
                            }
                        }
                    } else {
                        val pict = Pictogram(
                            id = 0,
                            label = form.label,
                            speech = form.speech,
                            parentId = folderId,
                            isFolder = form.isFolder,
                            imageSource = form.imageSource,
                            arasaacId = form.arasaacId,
                            customImage = form.customImage,
                            fitzKey = form.fitzKey.storage,
                            sortOrder = _state.value.pictograms.size
                        )
                        val newId = repository.add(pict)
                        if (form.isFolder && form.childIds.isNotEmpty()) {
                            repository.addReferencesToFolder(newId, form.childIds)
                        }
                    }
                    folderVersion.value++
                }
            }
            is GridIntent.DeletePictogram -> {
                scope.launch { repository.delete(intent.id) }
            }
        }
    }

    private fun navigateToFolder(folder: Pictogram) {
        _state.update {
            it.copy(
                folderStack = it.folderStack + folder.id,
                folderPath = it.folderPath + folder,
                selectedCategoryId = null
            )
        }
        selectedCategory.value = null
        folderVersion.value++
    }

    override fun onParentalGate() { onOpenParentMode?.invoke() }
}
