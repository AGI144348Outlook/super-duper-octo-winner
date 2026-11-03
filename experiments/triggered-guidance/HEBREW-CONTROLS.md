# ע / פ controls — MCP companion experiment

This folder is an **unintegrated prototype**. It does not modify the packaged `AIBubble.zip`, the existing MCP connector, or the live Android installation.

## Controls

- **ע (sight)**: long-press to request start/stop of the consent-gated screen session. Once on, tap for **one** user-initiated screen observation. Android must display its capture permission prompt when needed.
- **פ (sound)**: tap to toggle the independent sound session. A host implementation can expose MCP `speak_text` for Android TextToSpeech and `get_speech_transcript` after an explicitly authorized speech-recognition session.
- Both buttons have visible on/off rings and accessibility descriptions. The host must also provide the persistent status notification required for active capture/recording.

## Intended MCP model

The user's existing ChatGPT/Claude thread is the conversational home. The AI invokes connected MCP tools to read, highlight and speak. **A bubble press cannot be assumed to trigger a new turn in an existing provider thread merely because MCP is connected**. Until that path is verified, the UI must show a pending/awaiting state, not fabricate a companion reply.

The relay is a general-purpose companion transport, not a navigation-only engine: speech, shared observations, user questions and ordinary conversation are all valid. Do not force every reply into a navigation instruction.

## Implementation integration

1. Inspect the Android source inside `AIBubble.zip` and identify its existing overlay, consent, service and MCP transport lifecycle.
2. Instantiate `HebrewSessionControls` within the overlay. Wire callbacks to `GuidanceSessionController` through the host's consent and permissions layer.
3. Map sight state to MediaProjection lifecycle; map sound state to explicit voice permissions, TTS and speech recognition as separate subcapabilities.
4. Keep existing `read_screen`/`highlight` tools backward compatible. Add `speak_text`/`get_speech_transcript` only after their handlers exist.
5. Never transmit protected-app content, password fields or screenshots without an active authorized session.
6. Add a clearly visible status notification, quick settings tiles and drag-to-close only after basic controls are verified.

## Limitations

- Android overlay widgets are source-only; no APK build or on-device tests performed.
- Button layout currently uses long-press for sight session consent and tap for update, to avoid accidentally starting capture.
- Provider-initiated new turns into existing ChatGPT/Claude threads remain unverified.
- The existing text-anchor tracker is a separate experiment and has not been wired to the overlay.
