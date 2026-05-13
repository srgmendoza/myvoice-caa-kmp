package com.caa.app.platform.image

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
actual fun rememberImagePicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) persistAndNotify(scope, context, uri, onPicked)
    }
    return {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

@Composable
actual fun rememberCameraCapture(onPicked: (String) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val canCapture = remember(context) {
        val pm = context.packageManager
        val hasFeature = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val probe = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val resolves = probe.resolveActivity(pm) != null
        hasFeature && resolves
    }
    if (!canCapture) return null

    val pendingHolder = remember { arrayOfNulls<File>(1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingHolder[0]
        pendingHolder[0] = null
        if (success && file != null && file.exists() && file.length() > 0) {
            onPicked("file://${file.absolutePath}")
        } else {
            file?.delete()
        }
    }

    return {
        val dir = File(context.filesDir, "pictograms").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        pendingHolder[0] = file
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        try {
            launcher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Log.w("ImagePicker", "No camera app available to handle IMAGE_CAPTURE", e)
            pendingHolder[0] = null
            file.delete()
        }
    }
}

@Composable
actual fun rememberFilePicker(onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) persistAndNotify(scope, context, uri, onPicked)
    }
    return { launcher.launch(ImageMimeTypes) }
}

// TODO(cross-device): transcode HEIC/HEIF -> JPEG on import. Current copy-as-is works on API 28+ ImageDecoder
// but renders inconsistently on older devices, some OEMs, and future iOS path. Convert in IO before write.
private fun persistAndNotify(
    scope: CoroutineScope,
    context: Context,
    uri: Uri,
    onPicked: (String) -> Unit
) {
    scope.launch {
        val path = withContext(Dispatchers.IO) {
            runCatching {
                val ext = guessExtension(context, uri)
                val dir = File(context.filesDir, "pictograms").apply { mkdirs() }
                val file = File(dir, "${UUID.randomUUID()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
                "file://${file.absolutePath}"
            }.getOrNull()
        }
        if (path != null) onPicked(path)
    }
}

private fun guessExtension(context: Context, uri: Uri): String {
    val mime = context.contentResolver.getType(uri) ?: return "img"
    return when (mime) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/bmp" -> "bmp"
        "image/heic" -> "heic"
        "image/heif" -> "heif"
        else -> mime.substringAfter('/', "img")
    }
}
