package com.appriyo.newsvoiceassistant.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TTSManager(context: Context, private val onUtteranceCompleted: (() -> Unit)? = null) :
    TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingQueue = mutableListOf<String>()

    init {
        initializeTTS(context)
    }

    private fun initializeTTS(context: Context) {
        tts = TextToSpeech(context, this).apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Do nothing
                }

                override fun onDone(utteranceId: String?) {
                    onUtteranceCompleted?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    onUtteranceCompleted?.invoke()
                }
            })
        }
    }

    override fun onInit(status: Int) {
        isInitialized = status == TextToSpeech.SUCCESS
        if (isInitialized) {
            tts?.language = Locale.US
            // Process any pending speech
            pendingQueue.forEach { speak(it) }
            pendingQueue.clear()
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "NEWS_${System.currentTimeMillis()}")
        } else {
            pendingQueue.add(text)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun pause() {
        tts?.stop()
    }

    fun resume() {
        // TTS doesn't have pause/resume, we just stop and restart
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}