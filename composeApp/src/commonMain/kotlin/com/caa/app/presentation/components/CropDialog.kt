package com.caa.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import caa_kmp.composeapp.generated.resources.Res
import caa_kmp.composeapp.generated.resources.action_cancel
import caa_kmp.composeapp.generated.resources.crop_action
import caa_kmp.composeapp.generated.resources.crop_hint
import caa_kmp.composeapp.generated.resources.crop_title
import coil3.compose.AsyncImage
import com.caa.app.platform.image.rememberImageCropper
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun CropDialog(
    sourcePath: String,
    onCancel: () -> Unit,
    onConfirm: (croppedPath: String) -> Unit
) {
    val cropper = rememberImageCropper()
    val scope = rememberCoroutineScope()

    var imgSize by remember(sourcePath) { mutableStateOf(IntSize.Zero) }
    var viewportPx by remember { mutableStateOf(0f) }
    var userScale by remember(sourcePath) { mutableStateOf(1f) }
    var tx by remember(sourcePath) { mutableStateOf(0f) }
    var ty by remember(sourcePath) { mutableStateOf(0f) }
    var working by remember { mutableStateOf(false) }

    LaunchedEffect(sourcePath) {
        cropper.readDimensions(sourcePath)?.let { imgSize = it }
    }

    fun clampPan() {
        if (viewportPx <= 0f || imgSize == IntSize.Zero) return
        val baseCover = max(viewportPx / imgSize.width, viewportPx / imgSize.height)
        val totalScale = baseCover * userScale
        val maxTx = max(0f, (imgSize.width * totalScale - viewportPx) / 2f)
        val maxTy = max(0f, (imgSize.height * totalScale - viewportPx) / 2f)
        tx = tx.coerceIn(-maxTx, maxTx)
        ty = ty.coerceIn(-maxTy, maxTy)
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        userScale = (userScale * zoomChange).coerceIn(1f, 5f)
        tx += panChange.x
        ty += panChange.y
        clampPan()
    }

    LaunchedEffect(userScale, viewportPx, imgSize) { clampPan() }

    Dialog(
        onDismissRequest = { if (!working) onCancel() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !working,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF111111)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (!working) onCancel() }) {
                        Icon(Icons.Rounded.Close, stringResource(Res.string.action_cancel), tint = Color.White)
                    }
                    Text(
                        stringResource(Res.string.crop_title),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Viewport
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth(0.92f)
                            .onSizeChanged { viewportPx = it.width.toFloat() }
                            .clipToBounds()
                            .background(Color.Black)
                            .border(2.dp, Color.White, RoundedCornerShape(2.dp))
                    ) {
                        AsyncImage(
                            model = sourcePath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    scaleX = userScale
                                    scaleY = userScale
                                    translationX = tx
                                    translationY = ty
                                }
                                .transformable(transformableState)
                        )
                    }

                    if (working) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }

                Text(
                    stringResource(Res.string.crop_hint),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FooterButton(
                        text = stringResource(Res.string.action_cancel),
                        background = Color(0xFF333333),
                        contentColor = Color.White,
                        enabled = !working,
                        modifier = Modifier.weight(1f),
                        onClick = onCancel
                    )
                    FooterButton(
                        text = stringResource(Res.string.crop_action),
                        leadingIcon = Icons.Rounded.Check,
                        background = Brush.linearGradient(
                            listOf(Color(0xFF43A047), Color(0xFF2E7D32))
                        ),
                        contentColor = Color.White,
                        enabled = !working && imgSize != IntSize.Zero && viewportPx > 0f,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            working = true
                            scope.launch {
                                runCatching {
                                    val baseCover = max(
                                        viewportPx / imgSize.width,
                                        viewportPx / imgSize.height
                                    )
                                    val totalScale = baseCover * userScale
                                    val srcSidePx = viewportPx / totalScale
                                    val centerX = imgSize.width / 2f - tx / totalScale
                                    val centerY = imgSize.height / 2f - ty / totalScale
                                    val srcL = (centerX - srcSidePx / 2f)
                                        .toInt()
                                        .coerceAtLeast(0)
                                    val srcT = (centerY - srcSidePx / 2f)
                                        .toInt()
                                        .coerceAtLeast(0)
                                    val maxSide = min(imgSize.width - srcL, imgSize.height - srcT)
                                    val srcS = srcSidePx.toInt().coerceIn(1, maxSide)
                                    cropper.cropSquare(sourcePath, srcL, srcT, srcS)
                                }.fold(
                                    onSuccess = { onConfirm(it) },
                                    onFailure = { onCancel() }
                                )
                                working = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterButton(
    text: String,
    background: Color,
    contentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) background else background.copy(alpha = 0.4f),
        onClick = onClick,
        enabled = enabled
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f),
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun FooterButton(
    text: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Brush,
    contentColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        onClick = onClick,
        enabled = enabled
    ) {
        Box(
            modifier = Modifier
                .background(background)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(leadingIcon, null, tint = contentColor, modifier = Modifier.size(18.dp))
                Text(
                    text,
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
            }
        }
    }
}
