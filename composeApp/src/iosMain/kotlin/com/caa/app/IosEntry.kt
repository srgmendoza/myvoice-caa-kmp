package com.caa.app

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.caa.app.di.initKoin
import com.caa.app.domain.repository.PictogramRepository
import com.caa.app.platform.tts.SpeechEngine
import com.caa.app.presentation.root.App
import com.caa.app.presentation.root.DefaultRootComponent
import org.koin.core.context.GlobalContext
import platform.UIKit.UIViewController

private val koinStarted = run { initKoin(); true }

fun MainViewController(): UIViewController {
    require(koinStarted)
    val koin = GlobalContext.get()
    val repo = koin.get<PictogramRepository>()
    val speech = koin.get<SpeechEngine>()
    val lifecycle = LifecycleRegistry()
    val root = DefaultRootComponent(
        componentContext = DefaultComponentContext(lifecycle),
        repository = repo,
        speech = speech
    )
    return ComposeUIViewController { App(root) }
}
