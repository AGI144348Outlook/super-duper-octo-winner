package com.aibubble.experiment

/**
 * Coordinates one voice turn at a time.
 *
 * PromptSubmitter must be backed by a VERIFIED provider-specific mechanism that
 * creates a real user turn in the selected existing conversation. An MCP tool
 * response or relay queue alone is NOT a valid implementation.
 */
class ConversationTurnCoordinator(
    private val submitter: PromptSubmitter,
    private val speaker: Speaker,
    private val capture: Capture
) {
    enum class State { OFF, LISTENING, SUBMITTING, WAITING_FOR_REPLY, SPEAKING }
    interface PromptSubmitter {
        fun submitUserTurn(text: String, callback: (Result<String>) -> Unit)
    }
    interface Speaker {
        fun speak(text: String, onCompleted: () -> Unit, onError: (Throwable) -> Unit)
        fun stop()
    }
    interface Capture {
        fun start()
        fun stop()
    }

    var state: State = State.OFF
        private set
    private var epoch = 0L

    fun start() {
        if (state != State.OFF) return
        epoch++
        state = State.LISTENING
        capture.start()
    }

    fun stop() {
        epoch++
        state = State.OFF
        capture.stop()
        speaker.stop()
    }

    fun onFinalTranscript(text: String) {
        if (state != State.LISTENING || text.isBlank()) return
        val token = ++epoch
        capture.stop()
        state = State.SUBMITTING
        submitter.submitUserTurn(text.trim()) { result ->
            if (token != epoch || state == State.OFF) return@submitUserTurn
            result.fold(
                onSuccess = { reply ->
                    state = State.WAITING_FOR_REPLY
                    if (reply.isBlank()) {
                        resume(token)
                    } else {
                        state = State.SPEAKING
                        speaker.speak(reply,
                            onCompleted = { resume(token) },
                            onError = { resume(token) }
                        )
                    }
                },
                onFailure = { resume(token) }
            )
        }
    }

    private fun resume(token: Long) {
        if (token != epoch || state == State.OFF) return
        state = State.LISTENING
        capture.start()
    }
}
