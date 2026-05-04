package com.caa.app.platform.tts

interface SpeechEngine {
    fun speak(text: String, interrupt: Boolean = true)
    fun stop()
    fun setRate(rate: Float)
    fun setPitch(pitch: Float)
    fun release()
}

expect class SpeechEngineFactory {
    fun create(): SpeechEngine
}
