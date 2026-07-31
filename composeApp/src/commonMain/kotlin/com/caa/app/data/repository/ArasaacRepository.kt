package com.caa.app.data.repository

import com.caa.app.data.remote.ArasaacClient
import com.caa.app.data.remote.ArasaacFileSystem
import com.caa.app.data.remote.ArasaacImageDownloader
import com.caa.app.domain.model.ArasaacPictogram
import kotlin.coroutines.cancellation.CancellationException
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
        try {
            val results = client.search(query, language)
            cacheResults(query, language, results)
            results
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadCached(query, language)
        }
    }

    suspend fun downloadImage(arasaacId: Int): String? {
        return downloader.download(arasaacId)
    }

    private fun cacheKey(query: String, language: String): String =
        "$language-${query.lowercase()}".map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")

    private fun cacheResults(query: String, language: String, results: List<ArasaacPictogram>) {
        val dir = fs.cacheDir()
        fs.ensureDir(dir)
        val path = "$dir/${cacheKey(query, language)}.json"
        fs.writeText(path, json.encodeToString(results))
    }

    private fun loadCached(query: String, language: String): List<ArasaacPictogram> {
        val path = "${fs.cacheDir()}/${cacheKey(query, language)}.json"
        if (!fs.fileExists(path)) return emptyList()
        return try {
            val text = fs.readText(path) ?: return emptyList()
            json.decodeFromString<List<ArasaacPictogram>>(text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }
}
