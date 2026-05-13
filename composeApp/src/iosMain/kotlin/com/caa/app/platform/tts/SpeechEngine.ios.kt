package com.caa.app.platform.tts

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive

@OptIn(ExperimentalForeignApi::class)
class IosSpeechEngine : SpeechEngine {

    private val synth = AVSpeechSynthesizer()
    private var rate: Float = 0.5f
    private var pitch: Float = 1.0f

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        }
    }

    override fun speak(text: String, interrupt: Boolean) {
        if (interrupt && synth.speaking) synth.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val voice = AVSpeechSynthesisVoice.voiceWithLanguage("es-ES")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-MX")
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("es")
        val utt = AVSpeechUtterance.speechUtteranceWithString(text).apply {
            setVoice(voice)
            setRate(rate)
            setPitchMultiplier(pitch)
        }
        synth.speakUtterance(utt)
    }

    override fun stop() { synth.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate) }
    override fun setRate(rate: Float) { this.rate = rate }
    override fun setPitch(pitch: Float) { this.pitch = pitch }
    override fun release() { stop() }
}

@OptIn(ExperimentalForeignApi::class)
actual class SpeechEngineFactory {
    actual fun create(): SpeechEngine = IosSpeechEngine()
}
