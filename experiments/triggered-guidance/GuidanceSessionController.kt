package com.aibubble.experiment

import java.util.UUID

/**
 * User-initiated guidance coordinator. No background monitoring and no
 * autonomous control. The host Android service supplies permission status,
 * captures a redacted observation, and delivers it to an authenticated relay.
 *
 * This is an integration candidate, NOT wired into the current APK.
 */
class GuidanceSessionController(
    private val observationSource: ObservationSource,
    private val transport: GuidanceTransport,
    private val feedback: GuidanceFeedback
) {
    enum class ScreenState { OFF, ON }
    enum class VoiceState { OFF, ON }

    var screenState: ScreenState = ScreenState.OFF
        private set
    var voiceState: VoiceState = VoiceState.OFF
        private set

    private var generation: Long = 0
    private var activeConversation: String? = null
    private var selectedAssistant: String? = null

    fun setScreenSession(enabled: Boolean, permissionGranted: Boolean) {
        if (enabled && !permissionGranted) {
            feedback.status("Screen permission required")
            return
        }
        screenState = if (enabled) ScreenState.ON else ScreenState.OFF
        generation++
        if (!enabled) feedback.clearHighlight()
        feedback.status(if (enabled) "Screen session on" else "Screen session off")
    }

    fun setVoiceSession(enabled: Boolean) {
        voiceState = if (enabled) VoiceState.ON else VoiceState.OFF
        if (!enabled) feedback.stopSpeaking()
        feedback.status(if (enabled) "Voice session on" else "Voice session off")
    }

    fun selectConversation(assistantId: String, conversationId: String) {
        require(assistantId.isNotBlank() && conversationId.isNotBlank())
        selectedAssistant = assistantId
        activeConversation = conversationId
        generation++
    }

    /**
     * Called ONLY from an explicit Screen Update bubble press.
     * The host must enforce its screen-capture consent and privacy policy.
     */
    fun onScreenUpdatePressed() {
        val assistant = selectedAssistant
        val conversation = activeConversation
        if (screenState != ScreenState.ON || assistant == null || conversation == null) {
            feedback.status("Start screen session and select a conversation")
            return
        }
        val requestGeneration = ++generation
        val observation = observationSource.captureRedacted()
        if (observation == null) {
            feedback.clearHighlight()
            feedback.status("Screen unavailable or protected")
            return
        }
        val request = GuidanceRequest(
            requestId = UUID.randomUUID().toString(),
            assistantId = assistant,
            conversationId = conversation,
            observation = observation
        )
        transport.submit(request) { result ->
            // The transport callback must be marshalled to the Android main thread.
            if (requestGeneration != generation || screenState != ScreenState.ON) return@submit
            when (result) {
                is GuidanceResult.Ready -> {
                    result.highlight?.let(feedback::highlight) ?: feedback.clearHighlight()
                    if (voiceState == VoiceState.ON) feedback.speak(result.instruction)
                    feedback.status(result.instruction)
                }
                is GuidanceResult.Pending -> feedback.status("Update queued; awaiting AI")
                is GuidanceResult.Failed -> feedback.status("Update failed: ${result.reason}")
            }
        }
    }

    fun stopAll() {
        generation++
        screenState = ScreenState.OFF
        voiceState = VoiceState.OFF
        feedback.clearHighlight()
        feedback.stopSpeaking()
        feedback.status("All sessions stopped")
    }

    fun onPermissionRevoked() = stopAll()
}

data class RedactedObservation(
    val appPackage: String,
    val visibleText: String,
    val screenshotReference: String? = null
)

data class GuidanceRequest(
    val requestId: String,
    val assistantId: String,
    val conversationId: String,
    val observation: RedactedObservation
)

data class HighlightBounds(val x: Int, val y: Int, val width: Int, val height: Int)

sealed class GuidanceResult {
    data class Ready(val instruction: String, val highlight: HighlightBounds? = null) : GuidanceResult()
    object Pending : GuidanceResult()
    data class Failed(val reason: String) : GuidanceResult()
}

interface ObservationSource {
    /** Return null for locked, sensitive or otherwise unavailable screens. */
    fun captureRedacted(): RedactedObservation?
}

interface GuidanceTransport {
    /** Must not claim an AI response exists until the provider actually replies. */
    fun submit(request: GuidanceRequest, callback: (GuidanceResult) -> Unit)
}

interface GuidanceFeedback {
    fun status(message: String)
    fun highlight(bounds: HighlightBounds)
    fun clearHighlight()
    fun speak(text: String)
    fun stopSpeaking()
}
