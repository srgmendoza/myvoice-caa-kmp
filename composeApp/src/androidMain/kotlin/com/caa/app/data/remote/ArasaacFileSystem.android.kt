package com.caa.app.data.remote

import android.content.Context
import java.io.File

actual class ArasaacFileSystem(private val context: Context) {
    actual fun cacheDir(): String = "${context.filesDir.absolutePath}/arasaac_cache"
    actual fun pictogramsDir(): String = context.filesDir.absolutePath
    actual fun ensureDir(path: String) { File(path).mkdirs() }
    actual fun writeBytes(path: String, bytes: ByteArray): Boolean {
        return runCatching { File(path).outputStream().use { it.write(bytes) }; true }.getOrDefault(false)
    }
    actual fun readBytes(path: String): ByteArray? =
        runCatching { File(path).readBytes() }.getOrNull()
    actual fun readText(path: String): String? =
        runCatching { File(path).readText() }.getOrNull()
    actual fun writeText(path: String, text: String): Boolean {
        return runCatching { File(path).writeText(text); true }.getOrDefault(false)
    }
    actual fun fileExists(path: String): Boolean = File(path).exists()
    actual fun deleteFile(path: String): Boolean =
        runCatching { File(path).delete() }.getOrDefault(false)
}
