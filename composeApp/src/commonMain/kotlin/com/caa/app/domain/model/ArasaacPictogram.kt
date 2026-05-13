package com.caa.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArasaacPictogram(
    @SerialName("_id") val id: Int,
    val keywords: List<ArasaacKeyword> = emptyList(),
    val schematic: Boolean = false,
    val sex: Boolean = false,
    val violence: Boolean = false,
    val categories: List<String> = emptyList(),
    val synsets: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val desc: String = ""
) {
    val primaryKeyword: String
        get() = keywords.firstOrNull()?.keyword ?: ""

    val imageUrl300: String
        get() = "https://static.arasaac.org/pictograms/$id/${id}_300.png"

    val imageUrl500: String
        get() = "https://static.arasaac.org/pictograms/$id/${id}_500.png"
}

@Serializable
data class ArasaacKeyword(
    val idKeyword: Int = 0,
    val keyword: String = "",
    val plural: String? = null,
    val meaning: String? = null,
    val type: Int = 0
)

@Serializable
data class ArasaacSearchResult(
    val results: List<ArasaacPictogram> = emptyList()
)
