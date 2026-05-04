package com.caa.app.presentation.edit

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.Pictogram
import com.caa.app.domain.repository.PictogramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class EditState(
    val pictograms: List<Pictogram> = emptyList(),
    val categories: List<Category> = emptyList()
)

interface EditComponent {
    val state: Value<EditState>
    fun onAdd(label: String, speech: String, imagePath: String, categoryId: Long?)
    fun onUpdate(id: Long, label: String, speech: String, imagePath: String, categoryId: Long?)
    fun onDelete(id: Long)
    fun onOpenSettings()
    fun onBack()
}

class DefaultEditComponent(
    componentContext: ComponentContext,
    private val repository: PictogramRepository,
    private val onOpenSettingsCallback: () -> Unit,
    private val onFinished: () -> Unit
) : EditComponent, ComponentContext by componentContext {

    private val scope = coroutineScope(Dispatchers.Main)
    private val _state = MutableValue(EditState())
    override val state: Value<EditState> = _state

    init {
        scope.launch {
            repository.observeAll().collect { list ->
                _state.update { it.copy(pictograms = list) }
            }
        }
        scope.launch {
            repository.observeCategories().collect { list ->
                _state.update { it.copy(categories = list) }
            }
        }
    }

    override fun onAdd(label: String, speech: String, imagePath: String, categoryId: Long?) {
        scope.launch {
            repository.add(
                Pictogram(
                    id = 0,
                    label = label,
                    speech = speech,
                    imagePath = imagePath,
                    categoryId = categoryId,
                    colorHex = null,
                    sortOrder = _state.value.pictograms.size
                )
            )
        }
    }

    override fun onUpdate(id: Long, label: String, speech: String, imagePath: String, categoryId: Long?) {
        scope.launch {
            val existing = _state.value.pictograms.firstOrNull { it.id == id } ?: return@launch
            repository.update(
                existing.copy(
                    label = label,
                    speech = speech,
                    imagePath = imagePath,
                    categoryId = categoryId
                )
            )
        }
    }

    override fun onDelete(id: Long) {
        scope.launch { repository.delete(id) }
    }

    override fun onOpenSettings() = onOpenSettingsCallback()

    override fun onBack() = onFinished()
}
