package com.aibubble.experiment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.ContextWrapper

/**
 * In-app speech loop: no ACTION_RECOGNIZE_SPEECH activity is launched.
 * Requires RECORD_AUDIO permission and a visible user-controlled voice session.
 *
 * Android's SpeechRecognizer may still show system microphone indicators.
 * Not all recognition services support truly continuous recognition; this class
 * restarts sessions, with a cooldown, after final results and recoverable errors.
 *
 * This only delivers transcripts to a host callback. It CANNOT by itself create
 * a new turn in an existing ChatGPT/Claude conversation through MCP.
 */
class HandsFreeSpeechLoop(
    private val context: Context,
    private val listener: Listener,
    private val silenceMs: Long = 4000L
) {
    interface Listener {
        fun onPartialTranscript(text: String)
        fun onFinalUtterance(text: String)
        fun onStateChanged(state: String)
        fun onFailure(message: String)
    }

    init { require(silenceMs in 3000L..5000L) }
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var enabled = false
    private var playingReply = false
    private var listening = false
    private var utterance = ""
    private var generation = 0
    private var silenceStartedAt: Long? = null
    private val silenceCheck = object : Runnable {
        override fun run() {
            if (!enabled || playingReply) return
            val start = silenceStartedAt
            if (start != null && utterance.isNotBlank() &&
                SystemClock.elapsedRealtime() - start >= silenceMs) {
                finishUtterance()
            } else main.postDelayed(this, 200L)
        }
    }

    fun start() = main.post {
        if (enabled) return@post
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            listener.onFailure("Microphone permission required")
            return@post
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onFailure("Speech recognition service unavailable")
            return@post
        }
        enabled = true
        listener.onStateChanged("listening")
        beginRecognition()
    }

    fun stop() = main.post {
        enabled = false
        playingReply = false
        generation++
        main.removeCallbacks(silenceCheck)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        listening = false
        utterance = ""
        silenceStartedAt = null
        listener.onStateChanged("off")
    }

    /** Call before TTS begins, and again after it ends. */
    fun setReplyPlaying(active: Boolean) = main.post {
        if (!enabled) return@post
        playingReply = active
        generation++
        main.removeCallbacks(silenceCheck)
        recognizer?.cancel()
        listening = false
        utterance = ""
        silenceStartedAt = null
        if (active) listener.onStateChanged("speaking")
        else {
            listener.onStateChanged("listening")
            main.postDelayed({ if (enabled && !playingReply) beginRecognition() }, 300L)
        }
    }

    private fun beginRecognition() {
        if (!enabled || playingReply || listening) return
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val thisGeneration = ++generation
        val sr = recognizer ?: return
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (thisGeneration == generation) listener.onStateChanged("listening")
            }
            override fun onBeginningOfSpeech() {
                if (thisGeneration == generation) silenceStartedAt = null
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (thisGeneration == generation && utterance.isNotBlank())
                    silenceStartedAt = SystemClock.elapsedRealtime()
            }
            override fun onError(error: Int) {
                if (thisGeneration != generation) return
                listening = false
                if (!enabled || playingReply) return
                if (utterance.isNotBlank()) finishUtterance()
                else restartLater(700L)
            }
            override fun onResults(results: Bundle?) {
                if (thisGeneration != generation) return
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty().trim()
                if (text.isNotBlank()) utterance = text
                // Android often ends recognition before the requested silence interval.
                // Keep the transcript buffered, then restart recognition to allow
                // additional words to extend the same conversational turn.
                if (utterance.isNotBlank() && silenceStartedAt == null)
                    silenceStartedAt = SystemClock.elapsedRealtime()
                restartLater(350L)
            }
            override fun onPartialResults(partialResults: Bundle?) {
                if (thisGeneration != generation) return
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty().trim()
                if (text.isNotBlank()) {
                    utterance = text
                    silenceStartedAt = null
                    listener.onPartialTranscript(text)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            listening = true
            sr.startListening(intent)
            main.removeCallbacks(silenceCheck)
            main.postDelayed(silenceCheck, 200L)
        } catch (e: Exception) {
            listening = false
            listener.onFailure("Recognizer start failed: ${e.javaClass.simpleName}")
            restartLater(1000L)
        }
    }

    private fun restartLater(delayMs: Long) {
        val token = generation
        main.postDelayed({
            if (token == generation && enabled && !playingReply) {
                listening = false
                beginRecognition()
            }
        }, delayMs)
    }

    private fun finishUtterance() {
        val finalText = utterance.trim()
        utterance = ""
        silenceStartedAt = null
        main.removeCallbacks(silenceCheck)
        if (finalText.isNotBlank()) {
            listener.onStateChanged("awaiting-reply")
            listener.onFinalUtterance(finalText)
        }
        // Host may pause during playback; otherwise keep listening.
        if (enabled && !playingReply) main.postDelayed(silenceCheck, 200L)
    }
}
