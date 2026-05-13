package com.caa.app.domain.model

data class Pictogram(
    val id: Long,
    val label: String,
    val speech: String,
    val parentId: Long? = null,
    val isFolder: Boolean = false,
    val imageSource: String = "icon",
    val arasaacId: Int? = null,
    val iconKey: String? = null,
    val customImage: String? = null,
    val fitzKey: String = FitzgeraldKey.Verbs.storage,
    val categoryId: Long? = null,
    val colorHex: String? = null,
    val sortOrder: Int = 0
) {
    val imagePath: String
        get() = when (imageSource) {
            "photo" -> customImage ?: fitzgeraldPlaceholderKey(fitzKey)
            "arasaac" -> customImage ?: fitzgeraldPlaceholderKey(fitzKey)
            "icon" -> iconKey ?: fitzgeraldPlaceholderKey(fitzKey)
            else -> fitzgeraldPlaceholderKey(fitzKey)
        }
}

fun fitzgeraldPlaceholderKey(fitzKey: String): String = when (fitzKey) {
    FitzgeraldKey.People.storage -> "ic_fitz_people"
    FitzgeraldKey.Verbs.storage -> "ic_fitz_verbs"
    FitzgeraldKey.Nouns.storage -> "ic_fitz_nouns"
    FitzgeraldKey.Desc.storage -> "ic_fitz_desc"
    FitzgeraldKey.Social.storage -> "ic_fitz_social"
    FitzgeraldKey.Food.storage -> "ic_fitz_food"
    else -> "ic_default"
}

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
