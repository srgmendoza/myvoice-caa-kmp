package com.caa.app.presentation.grid

import androidx.compose.runtime.Immutable
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.Pictogram

@Immutable
data class GridState(
    val pictograms: List<Pictogram> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val sentence: List<Pictogram> = emptyList(),
    val isLoading: Boolean = true,
    val debounceMs: Long = 350L,
    val isSpeaking: Boolean = false,
    val isParentMode: Boolean = false,
    val folderStack: List<Long?> = listOf(null),
    val folderPath: List<Pictogram> = emptyList()
) {
    val currentFolderId: Long? get() = folderStack.lastOrNull() ?: null
}

sealed interface GridIntent {
    data class TapPictogram(val pictogram: Pictogram) : GridIntent
    data object SpeakSentence : GridIntent
    data object ClearSentence : GridIntent
    data object RemoveLastFromSentence : GridIntent
    data class SelectCategory(val id: Long?) : GridIntent
    data class NavigateToFolder(val folderId: Long) : GridIntent
    data object NavigateBack : GridIntent
    data object NavigateHome : GridIntent
    data object ToggleParentMode : GridIntent
    data class EditPictogram(val pictogram: Pictogram) : GridIntent
    data object AddPictogram : GridIntent
    data class DeletePictogram(val id: Long) : GridIntent
}
