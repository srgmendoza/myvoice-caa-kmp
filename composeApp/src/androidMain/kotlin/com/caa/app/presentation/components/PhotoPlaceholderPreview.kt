package com.caa.app.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.caa.app.presentation.theme.CaaTheme

@Preview(showBackground = true)
@Composable
private fun PhotoStripedFillPreview() {
    CaaTheme {
        PhotoStripedFill(
            accent = Color(0xFF4CAF50),
            modifier = Modifier.size(120.dp)
        )
    }
}
