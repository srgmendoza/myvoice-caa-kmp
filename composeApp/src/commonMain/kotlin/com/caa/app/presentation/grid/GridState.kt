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
    val isSpeaking: Boolean = false
)

sealed interface GridIntent {
    data class TapPictogram(val pictogram: Pictogram) : GridIntent
    data object SpeakSentence : GridIntent
    data object ClearSentence : GridIntent
    data object RemoveLastFromSentence : GridIntent
    data class SelectCategory(val id: Long?) : GridIntent
}
