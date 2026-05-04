package com.caa.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import caa_kmp.composeapp.generated.resources.Res
import caa_kmp.composeapp.generated.resources.nunito
import org.jetbrains.compose.resources.Font

@Composable
fun nunitoFamily(): FontFamily = FontFamily(
    Font(Res.font.nunito, weight = FontWeight.SemiBold),
    Font(Res.font.nunito, weight = FontWeight.Bold),
    Font(Res.font.nunito, weight = FontWeight.ExtraBold),
    Font(Res.font.nunito, weight = FontWeight.Black)
)

@Composable
fun caaTypography(): Typography {
    val nunito = nunitoFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = nunito, fontSize = 64.sp, fontWeight = FontWeight.Black),
        headlineLarge = TextStyle(fontFamily = nunito, fontSize = 40.sp, fontWeight = FontWeight.Black),
        headlineMedium = TextStyle(fontFamily = nunito, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
        titleLarge = TextStyle(fontFamily = nunito, fontSize = 22.sp, fontWeight = FontWeight.Black),
        titleMedium = TextStyle(fontFamily = nunito, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
        bodyLarge = TextStyle(fontFamily = nunito, fontSize = 17.sp, fontWeight = FontWeight.Bold),
        bodyMedium = TextStyle(fontFamily = nunito, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontFamily = nunito, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
        labelSmall = TextStyle(fontFamily = nunito, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    )
}
