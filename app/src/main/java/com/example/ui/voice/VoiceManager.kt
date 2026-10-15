package com.example.ui.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _activeUtteranceId = MutableStateFlow<String?>(null)
    val activeUtteranceId: StateFlow<String?> = _activeUtteranceId.asStateFlow()

    private val _autoSpeakEnabled = MutableStateFlow(false)
    val autoSpeakEnabled: StateFlow<Boolean> = _autoSpeakEnabled.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.ITALIAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default locale
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(_speechRate.value)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    _activeUtteranceId.value = utteranceId
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    _activeUtteranceId.value = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    _activeUtteranceId.value = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    _activeUtteranceId.value = null
                }
            })
            isInitialized = true
        } else {
            Log.e("VoiceManager", "Initialization of TextToSpeech failed")
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        _autoSpeakEnabled.value = enabled
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        tts?.setSpeechRate(rate)
    }

    fun speak(text: String, utteranceId: String = "omni_${System.currentTimeMillis()}") {
        if (!isInitialized || tts == null) return

        // Clean markdown symbols for natural vocal reading
        val cleanText = cleanMarkdownForSpeech(text)
        if (cleanText.isBlank()) return

        stop()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isInitialized && tts != null) {
            tts?.stop()
            _isSpeaking.value = false
            _activeUtteranceId.value = null
        }
    }

    fun toggleSpeak(text: String, utteranceId: String) {
        if (_isSpeaking.value && _activeUtteranceId.value == utteranceId) {
            stop()
        } else {
            speak(text, utteranceId)
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun cleanMarkdownForSpeech(input: String): String {
        return input
            .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), " Codice sorgente. ")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
            .replace(Regex("[-*•]\\s+"), "")
            .replace(Regex(">\\s+"), "")
            .trim()
    }
}
