package com.caa.app.platform.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "AndroidSpeechEngine"
private const val GOOGLE_TTS_PKG = "com.google.android.tts"

class AndroidSpeechEngine(private val context: Context) : SpeechEngine {

    private val initialized = AtomicBoolean(false)
    private val recreating = AtomicBoolean(false)
    private val utteranceCounter = AtomicLong(0)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pending = ArrayDeque<Pair<String, Boolean>>()

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var rate: Float = 1.0f
    @Volatile private var pitch: Float = 1.0f

    init { create() }

    private fun create() {
        if (!recreating.compareAndSet(false, true)) {
            Log.i(TAG, "create skipped, already recreating")
            return
        }
        initialized.set(false)
        val appCtx = context.applicationContext
        val engine = pickEngine(appCtx)
        Log.i(TAG, "create engine=$engine")
        val listener = TextToSpeech.OnInitListener { status ->
            Log.i(TAG, "onInit status=$status")
            recreating.set(false)
            if (status == TextToSpeech.SUCCESS) {
                applyLocale()
                tts?.setSpeechRate(rate)
                tts?.setPitch(pitch)
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )
                logAudioState()
                logVoiceState()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.i(TAG, "utterance onStart id=$utteranceId")
                    }
                    override fun onDone(utteranceId: String?) {
                        Log.i(TAG, "utterance onDone id=$utteranceId")
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.w(TAG, "utterance onError(legacy) id=$utteranceId")
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.w(TAG, "utterance onError id=$utteranceId code=$errorCode")
                    }
                })
                initialized.set(true)
                drainPending()
            } else {
                Log.e(TAG, "onInit FAILED status=$status")
            }
        }
        tts = if (engine != null) {
            TextToSpeech(appCtx, listener, engine)
        } else {
            TextToSpeech(appCtx, listener)
        }
    }

    private fun logAudioState() {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val volMusic = am.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val volA11y = am.getStreamVolume(AudioManager.STREAM_ACCESSIBILITY)
            val maxA11y = am.getStreamMaxVolume(AudioManager.STREAM_ACCESSIBILITY)
            val mode = am.mode
            val musicActive = am.isMusicActive
            Log.i(TAG, "audio: musicVol=$volMusic/$maxMusic a11yVol=$volA11y/$maxA11y mode=$mode musicActive=$musicActive")
        } catch (e: Exception) {
            Log.w(TAG, "audio state log failed", e)
        }
    }

    private fun logVoiceState() {
        try {
            val t = tts ?: return
            val esVoice = t.isLanguageAvailable(Locale("es", "ES"))
            val defaultVoice = runCatching { t.defaultVoice?.name }.getOrNull()
            val voice = runCatching { t.voice?.name }.getOrNull()
            val engines = runCatching { t.engines.joinToString { it.name } }.getOrNull()
            Log.i(TAG, "voice: esES=$esVoice default=$defaultVoice current=$voice engines=$engines")
        } catch (e: Exception) {
            Log.w(TAG, "voice state log failed", e)
        }
    }

    private fun pickEngine(ctx: Context): String? {
        return try {
            ctx.packageManager.getPackageInfo(GOOGLE_TTS_PKG, 0)
            GOOGLE_TTS_PKG
        } catch (_: Exception) { null }
    }

    private fun applyLocale() {
        val t = tts ?: return
        val preferred = Locale("es", "ES")
        val res = t.setLanguage(preferred)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            val fallback = Locale("es")
            val r2 = t.setLanguage(fallback)
            if (r2 == TextToSpeech.LANG_MISSING_DATA || r2 == TextToSpeech.LANG_NOT_SUPPORTED) {
                t.setLanguage(Locale.getDefault())
            }
        }
        pickBestVoice()
    }

    private fun pickBestVoice() {
        val t = tts ?: return
        val voices: Set<Voice> = runCatching { t.voices }.getOrNull() ?: return
        Log.i(TAG, "voices count=${voices.size}")
        voices.forEach { v ->
            val notInstalled = v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true
            Log.i(TAG, "  voice name=${v.name} locale=${v.locale} network=${v.isNetworkConnectionRequired} notInstalled=$notInstalled features=${v.features}")
        }
        val candidates = voices.filter { v ->
            !v.isNetworkConnectionRequired &&
            v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }
        val esES = candidates.firstOrNull { it.locale.language == "es" && it.locale.country == "ES" }
        val esAny = candidates.firstOrNull { it.locale.language == "es" }
        val anyOffline = candidates.firstOrNull()
        val chosen = esES ?: esAny ?: anyOffline
        if (chosen != null) {
            val r = runCatching { t.setVoice(chosen) }.getOrNull()
            Log.i(TAG, "chosen voice=${chosen.name} locale=${chosen.locale} setVoice=$r")
        } else {
            Log.w(TAG, "no offline+installed voice found")
        }
    }

    private fun drainPending() {
        val items: List<Pair<String, Boolean>>
        synchronized(pending) {
            if (pending.isEmpty()) return
            items = pending.toList()
            pending.clear()
        }
        Log.i(TAG, "drainPending size=${items.size}")
        for ((text, interrupt) in items) {
            doSpeak(text, interrupt)
        }
    }

    private fun doSpeak(text: String, interrupt: Boolean) {
        val t = tts
        if (t == null || !initialized.get()) {
            enqueue(text, interrupt)
            if (!recreating.get()) recreate()
            return
        }
        val mode = if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val id = utteranceCounter.incrementAndGet().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, 0.0f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        Log.i(TAG, "speak invoke id=$id mode=$mode text='${text.take(40)}'")
        val result = try {
            t.speak(text, mode, params, id)
        } catch (e: DeadObjectException) {
            Log.w(TAG, "DeadObject on speak", e)
            TextToSpeech.ERROR
        } catch (e: Exception) {
            Log.w(TAG, "speak threw", e)
            TextToSpeech.ERROR
        }
        Log.i(TAG, "speak result=$result (SUCCESS=${TextToSpeech.SUCCESS} ERROR=${TextToSpeech.ERROR})")
        if (result == TextToSpeech.ERROR) {
            Log.w(TAG, "speak ERROR, recreating TTS text=$text")
            enqueue(text, interrupt)
            recreate()
        }
    }

    private fun recreate() {
        if (recreating.get()) return
        initialized.set(false)
        val old = tts
        tts = null
        runCatching { old?.stop() }
        runCatching { old?.shutdown() }
        // Slight delay so old service unbind finishes before new bind
        mainHandler.postDelayed({ create() }, 100)
    }

    private fun enqueue(text: String, interrupt: Boolean) {
        synchronized(pending) {
            if (interrupt) pending.clear()
            pending.addLast(text to interrupt)
        }
    }

    override fun speak(text: String, interrupt: Boolean) {
        if (text.isBlank()) return
        if (initialized.get()) {
            doSpeak(text, interrupt)
        } else {
            enqueue(text, interrupt)
            if (!recreating.get() && tts == null) recreate()
        }
    }

    override fun stop() { runCatching { tts?.stop() } }
    override fun setRate(rate: Float) {
        this.rate = rate
        runCatching { tts?.setSpeechRate(rate) }
    }
    override fun setPitch(pitch: Float) {
        this.pitch = pitch
        runCatching { tts?.setPitch(pitch) }
    }
    override fun release() {
        initialized.set(false)
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
    }
}

actual class SpeechEngineFactory(private val context: Context) {
    actual fun create(): SpeechEngine = AndroidSpeechEngine(context)
}
