package com.caa.app.presentation.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.caa.app.presentation.theme.CaaRadius

// Resolves path to a renderable: ic_* preset icon, file:// local file, http(s):// remote (future Firebase).
@Composable
fun PictogramImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    when {
        path.startsWith("ic_") -> {
            Icon(
                imageVector = PictogramIcons.get(path),
                contentDescription = contentDescription,
                modifier = modifier,
                tint = tint
            )
        }
        else -> {
            AsyncImage(
                model = path,
                contentDescription = contentDescription,
                modifier = modifier.clip(shape = RoundedCornerShape(topStart = CaaRadius.md, topEnd = CaaRadius.md)),
                contentScale = ContentScale.FillBounds,
                colorFilter = if (tint == Color.Unspecified) null else ColorFilter.tint(tint)
            )
        }
    }
}
