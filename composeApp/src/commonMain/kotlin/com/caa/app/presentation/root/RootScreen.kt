package com.caa.app.presentation.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.caa.app.presentation.grid.GridScreen
import com.caa.app.presentation.settings.SettingsScreen
import com.caa.app.presentation.theme.CaaTheme
import org.koin.compose.KoinContext

@Composable
fun RootScreen(component: RootComponent) {
    KoinContext {
        CaaTheme {
            Children(stack = component.stack) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Grid -> GridScreen(instance.component)
                    is RootComponent.Child.Parent -> GridScreen(instance.component)
                    is RootComponent.Child.Settings -> SettingsScreen(instance.component)
                }
            }
        }
    }
}

@Composable
fun App(component: RootComponent) = RootScreen(component)
