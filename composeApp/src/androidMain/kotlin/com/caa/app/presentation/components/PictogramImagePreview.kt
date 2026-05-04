package com.caa.app.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caa.app.presentation.theme.CaaTheme

@Preview(showBackground = true)
@Composable
private fun PictogramImage_Icon_Preview() {
    CaaTheme {
        PictogramImage(
            path = "ic_want",
            contentDescription = "Want",
            modifier = Modifier.size(100.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PictogramImage_URL_Preview() {
    CaaTheme {
        PictogramImage(
            path = "https://example.com/pictogram.png",
            contentDescription = "Example",
            modifier = Modifier.size(100.dp)
        )
    }
}
