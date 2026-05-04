package com.caa.app.platform.image

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePicker(onPicked: (String) -> Unit): () -> Unit {
    return { /* iOS picker pending — wire PHPickerViewController later */ }
}

@Composable
actual fun rememberFilePicker(onPicked: (String) -> Unit): () -> Unit {
    return { /* iOS file picker pending — wire UIDocumentPickerViewController later */ }
}
