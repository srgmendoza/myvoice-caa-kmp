package com.caa.app.platform.tts

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundaryImmediate
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive

class IosSpeechEngine : SpeechEngine {

    private val synth = AVSpeechSynthesizer()
    private var rate: Float = 0.5f
    private var pitch: Float = 1.0f

    init {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        }
    }

    override fun speak(text: String, interrupt: Boolean) {
        if (interrupt && synth.speaking) synth.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
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

    override fun stop() { synth.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate) }
    override fun setRate(rate: Float) { this.rate = rate }
    override fun setPitch(pitch: Float) { this.pitch = pitch }
    override fun release() { stop() }
}

actual class SpeechEngineFactory {
    actual fun create(): SpeechEngine = IosSpeechEngine()
}
