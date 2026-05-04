package com.caa.app.domain.model

data class Pictogram(
    val id: Long,
    val label: String,
    val speech: String,
    val imagePath: String,
    val categoryId: Long?,
    val colorHex: String?,
    val sortOrder: Int
)

enum class FitzgeraldKey(val storage: String, val displayName: String) {
    People("people", "Personas"),
    Verbs("verbs", "Verbos"),
    Nouns("nouns", "Sustantivos"),
    Desc("desc", "Descriptivos"),
    Social("social", "Social"),
    Food("food", "Comida");

    companion object {
        fun fromStorage(value: String?): FitzgeraldKey =
            entries.firstOrNull { it.storage == value } ?: Verbs
    }
}

data class Category(
    val id: Long,
    val name: String,
    val colorHex: String,
    val sortOrder: Int,
    val key: FitzgeraldKey
)
