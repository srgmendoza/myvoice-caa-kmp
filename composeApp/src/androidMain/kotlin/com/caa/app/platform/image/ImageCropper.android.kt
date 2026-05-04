package com.caa.app.platform.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
actual fun rememberImageCropper(): ImageCropper {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidImageCropper(context) }
}

private class AndroidImageCropper(private val context: Context) : ImageCropper {

    override suspend fun cropSquare(
        sourcePath: String,
        srcLeft: Int,
        srcTop: Int,
        srcSide: Int,
        outputSide: Int
    ): String = withContext(Dispatchers.IO) {
        val src = sourcePath.removePrefix("file://")
        val raw = BitmapFactory.decodeFile(src)
            ?: error("Could not decode image: $sourcePath")

        val oriented = applyExifOrientation(src, raw)

        try {
            val left = srcLeft.coerceIn(0, (oriented.width - 1).coerceAtLeast(0))
            val top = srcTop.coerceIn(0, (oriented.height - 1).coerceAtLeast(0))
            val maxSide = minOf(oriented.width - left, oriented.height - top)
            val side = srcSide.coerceIn(1, maxSide)

            val cropped = Bitmap.createBitmap(oriented, left, top, side, side)
            val scaled = if (side != outputSide) {
                Bitmap.createScaledBitmap(cropped, outputSide, outputSide, true)
            } else cropped

            val outDir = File(context.filesDir, "pictograms").apply { mkdirs() }
            val outFile = File(outDir, "${UUID.randomUUID()}.jpg")
            FileOutputStream(outFile).use { os ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 92, os)
            }

            if (cropped !== scaled) cropped.recycle()
            scaled.recycle()
            "file://${outFile.absolutePath}"
        } finally {
            if (oriented !== raw) raw.recycle()
            oriented.recycle()
        }
    }

    override suspend fun readDimensions(sourcePath: String): IntSize? = withContext(Dispatchers.IO) {
        val src = sourcePath.removePrefix("file://")
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return@withContext null

        val orientation = runCatching {
            ExifInterface(src).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val rotated = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
            orientation == ExifInterface.ORIENTATION_TRANSVERSE

        if (rotated) IntSize(opts.outHeight, opts.outWidth)
        else IntSize(opts.outWidth, opts.outHeight)
    }

    private fun applyExifOrientation(path: String, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f); matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f); matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
