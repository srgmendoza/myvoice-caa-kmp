package com.caa.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

object PictogramIcons {
    private val map: Map<String, ImageVector> = mapOf(
        "ic_want" to Icons.Rounded.PanTool,
        "ic_more" to Icons.Rounded.Add,
        "ic_help" to Icons.AutoMirrored.Rounded.Help,
        "ic_play" to Icons.Rounded.SportsEsports,
        "ic_sleep" to Icons.Rounded.Bedtime,
        "ic_toilet" to Icons.Rounded.Wc,
        "ic_eat" to Icons.Rounded.Restaurant,
        "ic_drink" to Icons.Rounded.LocalDrink,
        "ic_apple" to Icons.Rounded.Spa,
        "ic_cookie" to Icons.Rounded.Cookie,
        "ic_mom" to Icons.Rounded.Face3,
        "ic_dad" to Icons.Rounded.Face6,
        "ic_me" to Icons.Rounded.Person,
        "ic_happy" to Icons.Rounded.SentimentVerySatisfied,
        "ic_sad" to Icons.Rounded.SentimentVeryDissatisfied,
        "ic_angry" to Icons.Rounded.Mood,
        "ic_yes" to Icons.Rounded.Check,
        "ic_no" to Icons.Rounded.Close,
        "ic_hello" to Icons.Rounded.WavingHand,
        "ic_bye" to Icons.AutoMirrored.Rounded.ExitToApp,
        "ic_default" to Icons.Rounded.Image
    )

    fun get(key: String): ImageVector = map[key] ?: Icons.Rounded.Image

    fun keys(): List<String> = map.keys.toList()
}
