package com.caa.app.data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual class ArasaacFileSystem {
    private val docs: String get() =
        NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String

    actual fun cacheDir(): String = "$docs/arasaac_cache"
    actual fun pictogramsDir(): String = docs
    actual fun ensureDir(path: String) { NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null) }

    actual fun writeBytes(path: String, bytes: ByteArray): Boolean = memScoped {
        if (bytes.isEmpty()) return@memScoped false
        val fp = fopen(path, "wb") ?: return@memScoped false
        bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), fp)
        }
        fclose(fp)
        true
    }

    actual fun readBytes(path: String): ByteArray? = memScoped {
        val fp = fopen(path, "rb") ?: return@memScoped null
        fseek(fp, 0, SEEK_END)
        val size = ftell(fp)
        if (size <= 0L) { fclose(fp); return@memScoped null }
        fseek(fp, 0, SEEK_SET)
        val bytes = ByteArray(size.toInt())
        bytes.usePinned { pinned ->
            fread(pinned.addressOf(0), 1u, size.toULong(), fp)
        }
        fclose(fp)
        bytes
    }

    actual fun readText(path: String): String? = memScoped {
        val fp = fopen(path, "rb") ?: return@memScoped null
        fseek(fp, 0, SEEK_END)
        val size = ftell(fp)
        if (size <= 0L) { fclose(fp); return@memScoped null }
        fseek(fp, 0, SEEK_SET)
        val bytes = ByteArray(size.toInt())
        bytes.usePinned { pinned ->
            fread(pinned.addressOf(0), 1u, size.toULong(), fp)
        }
        fclose(fp)
        bytes.decodeToString()
    }

    actual fun writeText(path: String, text: String): Boolean = memScoped {
        val bytes = text.encodeToByteArray()
        if (bytes.isEmpty()) return@memScoped false
        val fp = fopen(path, "wb") ?: return@memScoped false
        bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), fp)
        }
        fclose(fp)
        true
    }

    actual fun fileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

    actual fun deleteFile(path: String): Boolean =
        NSFileManager.defaultManager.removeItemAtPath(path, null)
}
