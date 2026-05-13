package com.caa.app.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

expect class ArasaacFileSystem {
    fun cacheDir(): String
    fun pictogramsDir(): String
    fun ensureDir(path: String)
    fun writeBytes(path: String, bytes: ByteArray): Boolean
    fun readBytes(path: String): ByteArray?
    fun readText(path: String): String?
    fun writeText(path: String, text: String): Boolean
    fun fileExists(path: String): Boolean
}

class ArasaacImageDownloader(
    private val fs: ArasaacFileSystem
) {
    private val http = HttpClient()

    suspend fun download(arasaacId: Int, size: Int = 300): String? {
        val dir = "${fs.pictogramsDir()}/arasaac"
        fs.ensureDir(dir)
        val path = "$dir/$arasaacId.jpg"
        if (fs.fileExists(path)) return "file://$path"

        return runCatching {
            val url = "https://static.arasaac.org/pictograms/$arasaacId/${arasaacId}_$size.png"
            val bytes = http.get(url).body<ByteArray>()
            if (fs.writeBytes(path, bytes)) "file://$path" else null
        }.getOrNull()
    }

    fun close() { http.close() }
}
