package com.caa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arkivanov.decompose.defaultComponentContext
import com.caa.app.domain.repository.PictogramRepository
import com.caa.app.domain.repository.SettingsRepository
import com.caa.app.platform.tts.SpeechEngine
import com.caa.app.presentation.root.App
import com.caa.app.presentation.root.DefaultRootComponent
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val repository: PictogramRepository by inject()
    private val settings: SettingsRepository by inject()
    private val speech: SpeechEngine by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            repository = repository,
            settings = settings,
            speech = speech
        )
        setContent { App(root) }
    }

    override fun onDestroy() {
        speech.release()
        super.onDestroy()
    }
}
