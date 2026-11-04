package com.aibubble.experiment

/**
 * Pure state machine for microphone frame timestamps. No microphone is opened here.
 * The Android audio service supplies monotonic timestamps and voice activity flags.
 * A turn ends after a continuous quiet interval, initially 4 seconds.
 */
class SilenceTurnDetector(
    val silenceMs: Long = 4000L,
    private val onTurnEnded: () -> Unit
) {
    init { require(silenceMs in 3000L..5000L) }
    private var lastTimeMs: Long = -1L
    private var quietSinceMs: Long? = null
    private var heardSpeech = false
    private var enabled = false
    private var pausedForPlayback = false

    fun start() {
        enabled = true
        pausedForPlayback = false
        resetTurn()
    }

    fun stop() {
        enabled = false
        resetTurn()
    }

    /** Suppress echo from our own TTS. Resume capture after playback finishes. */
    fun setPlaybackActive(active: Boolean) {
        pausedForPlayback = active
        resetTurn()
    }

    /** Timestamp must be monotonic milliseconds, e.g. elapsedRealtime(). */
    fun acceptFrame(timeMs: Long, speechDetected: Boolean) {
        if (!enabled || pausedForPlayback) return
        if (lastTimeMs >= 0L && timeMs < lastTimeMs) {
            resetTurn()
            return
        }
        lastTimeMs = timeMs
        if (speechDetected) {
            heardSpeech = true
            quietSinceMs = null
            return
        }
        if (!heardSpeech) return
        val started = quietSinceMs
        if (started == null) {
            quietSinceMs = timeMs
        } else if (timeMs - started >= silenceMs) {
            resetTurn()
            onTurnEnded()
        }
    }

    private fun resetTurn() {
        lastTimeMs = -1L
        quietSinceMs = null
        heardSpeech = false
    }
}
