package com.caa.app.data.remote

import com.caa.app.domain.model.ArasaacPictogram
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ArasaacClient(private val http: HttpClient) {

    suspend fun search(query: String, language: String = "es"): List<ArasaacPictogram> {
        if (query.isBlank()) return emptyList()
        return http.get("https://api.arasaac.org/v1/pictograms/$language/search/$query").body()
    }

    suspend fun bestSearch(query: String, language: String = "es"): List<ArasaacPictogram> {
        if (query.isBlank()) return emptyList()
        return http.get("https://api.arasaac.org/v1/pictograms/$language/bestsearch/$query").body()
    }
}
