package com.caa.app.data.remote

import com.caa.app.domain.model.ArasaacPictogram
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ArasaacClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val http = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    suspend fun search(query: String, language: String = "es"): List<ArasaacPictogram> {
        if (query.isBlank()) return emptyList()
        return http.get("https://api.arasaac.org/v1/pictograms/$language/search/$query").body()
    }

    suspend fun bestSearch(query: String, language: String = "es"): List<ArasaacPictogram> {
        if (query.isBlank()) return emptyList()
        return http.get("https://api.arasaac.org/v1/pictograms/$language/bestsearch/$query").body()
    }

    fun close() { http.close() }
}
