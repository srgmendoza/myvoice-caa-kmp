package com.caa.app.platform.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize

// iOS picker is still a stub; for now the cropper is a passthrough so
// downstream code can call through without breaking. Wire up real cropping
// once PHPickerViewController is added in ImagePicker.ios.kt.
@Composable
actual fun rememberImageCropper(): ImageCropper = remember { IosImageCropper() }

private class IosImageCropper : ImageCropper {
    override suspend fun cropSquare(
        sourcePath: String,
        srcLeft: Int,
        srcTop: Int,
        srcSide: Int,
        outputSide: Int
    ): String = sourcePath

    override suspend fun readDimensions(sourcePath: String): IntSize? = null
}
