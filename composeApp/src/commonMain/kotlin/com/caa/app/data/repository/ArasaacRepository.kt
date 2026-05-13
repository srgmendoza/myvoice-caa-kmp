package com.caa.app.data.repository

import com.caa.app.data.remote.ArasaacClient
import com.caa.app.data.remote.ArasaacFileSystem
import com.caa.app.data.remote.ArasaacImageDownloader
import com.caa.app.domain.model.ArasaacPictogram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ArasaacRepository(
    private val client: ArasaacClient,
    private val downloader: ArasaacImageDownloader,
    private val fs: ArasaacFileSystem
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(query: String, language: String = "es"): List<ArasaacPictogram> = withContext(Dispatchers.Default) {
        runCatching { client.search(query, language) }.onSuccess { results ->
            cacheResults(query, results)
        }.getOrElse { loadCached(query) }
    }

    suspend fun downloadImage(arasaacId: Int): String? {
        return downloader.download(arasaacId)
    }

    private suspend fun cacheResults(query: String, results: List<ArasaacPictogram>) = withContext(Dispatchers.Default) {
        val dir = fs.cacheDir()
        fs.ensureDir(dir)
        val path = "$dir/${query.lowercase().hashCode()}.json"
        fs.writeText(path, json.encodeToString(results))
    }

    private suspend fun loadCached(query: String): List<ArasaacPictogram> = withContext(Dispatchers.Default) {
        val path = "${fs.cacheDir()}/${query.lowercase().hashCode()}.json"
        if (!fs.fileExists(path)) return@withContext emptyList()
        runCatching {
            val text = fs.readText(path) ?: return@withContext emptyList()
            json.decodeFromString<List<ArasaacPictogram>>(text)
        }.getOrDefault(emptyList())
    }
}
