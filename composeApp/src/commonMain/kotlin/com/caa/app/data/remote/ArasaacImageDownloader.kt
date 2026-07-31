package com.caa.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlin.coroutines.cancellation.CancellationException

expect class ArasaacFileSystem {
    fun cacheDir(): String
    fun pictogramsDir(): String
    fun ensureDir(path: String)
    fun writeBytes(path: String, bytes: ByteArray): Boolean
    fun readBytes(path: String): ByteArray?
    fun readText(path: String): String?
    fun writeText(path: String, text: String): Boolean
    fun fileExists(path: String): Boolean
    fun deleteFile(path: String): Boolean
}

/**
 * Downloads ARASAAC pictogram images into `<pictogramsDir>/arasaac/`.
 *
 * Returns a loadable absolute `file://` URI (needed for immediate preview in the UI).
 * The repository normalizes it to a path relative to [ArasaacFileSystem.pictogramsDir]
 * before persisting, so the stored value survives iOS container relocations.
 */
class ArasaacImageDownloader(
    private val http: HttpClient,
    private val fs: ArasaacFileSystem
) {
    suspend fun download(arasaacId: Int, size: Int = 300): String? {
        val dir = "${fs.pictogramsDir()}/arasaac"
        fs.ensureDir(dir)
        val path = "$dir/${arasaacId}_$size.png"
        if (fs.fileExists(path)) return "file://$path"

        return try {
            val url = "https://static.arasaac.org/pictograms/$arasaacId/${arasaacId}_$size.png"
            val response = http.get(url)
            if (!response.status.isSuccess()) return null
            val bytes = response.body<ByteArray>()
            if (bytes.isEmpty()) return null
            if (fs.writeBytes(path, bytes)) {
                "file://$path"
            } else {
                fs.deleteFile(path)
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fs.deleteFile(path)
            null
        }
    }
}
