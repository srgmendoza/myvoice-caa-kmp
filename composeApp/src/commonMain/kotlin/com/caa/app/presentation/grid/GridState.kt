package com.caa.app.presentation.grid

import androidx.compose.runtime.Immutable
import com.caa.app.domain.model.Category
import com.caa.app.domain.model.FitzgeraldKey
import com.caa.app.domain.model.Pictogram

data class PictogramFormData(
    val label: String = "",
    val speech: String = "",
    val fitzKey: FitzgeraldKey = FitzgeraldKey.Verbs,
    val isFolder: Boolean = false,
    val imageSource: String = "arasaac",
    val arasaacId: Int? = null,
    val customImage: String? = null,
    val childIds: List<Long> = emptyList()
)

@Immutable
data class GridState(
    val pictograms: List<Pictogram> = emptyList(),
    val allPictograms: List<Pictogram> = emptyList(),
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
    data object OpenSettings : GridIntent
    data object ToggleParentMode : GridIntent
    data class EditPictogram(val pictogram: Pictogram) : GridIntent
    data object AddPictogram : GridIntent
    data class SavePictogram(val form: PictogramFormData, val editId: Long? = null) : GridIntent
    data class DeletePictogram(val id: Long) : GridIntent
    data class AddExistingReferences(val folderId: Long, val pictogramIds: List<Long>) : GridIntent
}
