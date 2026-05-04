package com.caa.app.platform.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize

interface ImageCropper {
    /**
     * Crops [sourcePath] to a square region defined in source-pixel coordinates
     * (post-EXIF rotation) and writes a JPEG of [outputSide]x[outputSide]
     * pixels into the app's pictograms directory. Returns the resulting
     * "file://..." path.
     */
    suspend fun cropSquare(
        sourcePath: String,
        srcLeft: Int,
        srcTop: Int,
        srcSide: Int,
        outputSide: Int = 1024
    ): String

    /**
     * Returns the dimensions of [sourcePath] in displayed orientation
     * (i.e. EXIF rotation applied), or null if the file can't be read.
     */
    suspend fun readDimensions(sourcePath: String): IntSize?
}

@Composable
expect fun rememberImageCropper(): ImageCropper
