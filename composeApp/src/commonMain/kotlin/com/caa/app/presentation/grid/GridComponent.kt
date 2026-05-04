package com.caa.app.presentation.grid

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.caa.app.domain.repository.PictogramRepository
import com.caa.app.domain.repository.SettingsRepository
import com.caa.app.platform.tts.SpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    private val onOpenEdit: () -> Unit
) : GridComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main)
    private val _state = MutableValue(GridState())
    override val state: Value<GridState> = _state

    private val selectedCategory = MutableStateFlow<Long?>(null)
    private var sentenceSpeakActive = false

    init {
        scope.launch { repository.seedDefaults() }

        scope.launch {
            combine(
                selectedCategory.flatMapLatest { id ->
                    if (id == null) repository.observeAll() else repository.observeByCategory(id)
                },
                repository.observeCategories(),
                settings.observe()
            ) { pictos, cats, s -> Triple(pictos, cats, s.debounceMs) }.collect { (pictos, cats, debounce) ->
                _state.update { it.copy(pictograms = pictos, categories = cats, isLoading = false, debounceMs = debounce) }
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
                _state.update { it.copy(sentence = it.sentence + intent.pictogram) }
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
        }
    }

    override fun onParentalGate() = onOpenEdit()
}
