package com.caa.app.platform.image

import androidx.compose.runtime.Composable

// Photo picker (gallery). Returns persisted file:// path via onPicked.
@Composable
expect fun rememberImagePicker(onPicked: (String) -> Unit): () -> Unit

// Files / Storage Access Framework picker. Filters by image mime types.
@Composable
expect fun rememberFilePicker(onPicked: (String) -> Unit): () -> Unit

// Common image mime list — used by file picker filter.
val ImageMimeTypes: Array<String> = arrayOf(
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp",
    "image/bmp",
    "image/heic",
    "image/heif"
)
