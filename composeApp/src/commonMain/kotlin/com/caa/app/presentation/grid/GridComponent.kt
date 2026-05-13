package com.caa.app.presentation.grid

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.caa.app.domain.model.Pictogram
import com.caa.app.domain.repository.PictogramRepository
import com.caa.app.domain.repository.SettingsRepository
import com.caa.app.platform.tts.SpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface GridComponent {
    val state: Value<GridState>
    fun onIntent(intent: GridIntent)
    fun onParentalGate()
}

class DefaultGridComponent(
    componentContext: ComponentContext,
    private val repository: PictogramRepository,
    private val settings: SettingsRepository,
    private val speech: SpeechEngine,
    private val onOpenSettings: () -> Unit,
    private val onOpenParentMode: (() -> Unit)? = null,
    private val isParentMode: Boolean = false
) : GridComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main)
    private val _state = MutableValue(GridState(isParentMode = isParentMode))
    override val state: Value<GridState> = _state

    private val selectedCategory = MutableStateFlow<Long?>(null)
    private var sentenceSpeakActive = false

    init {
        scope.launch { repository.seedDefaults() }

        scope.launch {
            selectedCategory.collect { catId ->
                val folderId = _state.value.currentFolderId
                val childFlow = when {
                    catId != null && folderId != null -> repository.observeChildrenByCategory(folderId, catId)
                    catId != null -> repository.observeByCategory(catId)
                    else -> repository.observeChildren(folderId)
                }
                childFlow.collect { pictos ->
                    _state.update { it.copy(pictograms = pictos) }
                }
            }
        }

        scope.launch {
            repository.observeCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
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
                if (intent.pictogram.isFolder && isParentMode) {
                    navigateToFolder(intent.pictogram)
                } else {
                    speech.speak(intent.pictogram.speech)
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
                }
            }
            GridIntent.NavigateHome -> {
                _state.update { it.copy(folderStack = listOf(null), folderPath = emptyList()) }
            }
            GridIntent.ToggleParentMode -> {
                // Toggled by the screen, not here
            }
            is GridIntent.EditPictogram -> {
                // Handled by the screen — will open form sheet
            }
            GridIntent.AddPictogram -> {
                // Handled by the screen
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
    }

    override fun onParentalGate() { onOpenParentMode?.invoke() }
}
