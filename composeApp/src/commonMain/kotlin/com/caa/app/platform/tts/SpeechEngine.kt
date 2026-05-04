package com.caa.app.platform.tts

import kotlinx.coroutines.flow.StateFlow

interface SpeechEngine {
    val isSpeaking: StateFlow<Boolean>
    fun speak(text: String, interrupt: Boolean = true)
    fun stop()
    fun setRate(rate: Float)
    fun setPitch(pitch: Float)
    fun release()
}

expect class SpeechEngineFactory {
    fun create(): SpeechEngine
}
