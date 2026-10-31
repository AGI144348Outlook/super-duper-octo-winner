# NexusPortal v0.7 device voice relay

## Current feature-branch implementation

- `api/mcp.js` provides authenticated MCP initialize, ping, and tools:
  - `portal_status`: reports MCP auth, phone heartbeat, voice-session state, and a current Android session ID for the ChatGPT-named connector only.
  - `approve_device_pairing`: approves the one-time code that the phone displays after the user starts pairing.
  - `speak_text`: queues bounded text for Android TTS. It requires the live session ID and a unique request ID.
  - `get_speech_transcript`: consumes one text transcript submitted by the user from Android's recognition dialog.
  - `revoke_device`: revokes the phone and cancels queued commands.
- `api/device/[action].js` implements pairing, status check, heartbeat, session stop, speech submit, command claim/ack, and local revoke.
- Pairing codes and Android tokens are generated on the phone. Only their SHA-256 hashes are stored on the relay; the Android token is encrypted with Android Keystore.
- A phone heartbeat expires from connected status after 45 seconds. Voice is enabled only while the user has explicitly started a voice session in NexusPortal.
- Commands are bound to the active phone session, have a 45-second TTL, and are claimed once. Duplicate request IDs, stale session IDs, revoked devices, and expired sessions fail closed.
- Speech recognition is one-shot and initiated by a visible Android action. NexusPortal sends the recognized text only after the user taps **Send last transcript to ChatGPT**. Raw microphone audio is not sent by NexusPortal.
- The overlay service polls only while it is running. The persistent notification/Android foreground-service rules still apply.

## Required deployment setup

- Vercel project root directory: `nexusportal`.
- `DATABASE_URL`: sensitive Vercel environment variable for production and preview, using the dedicated `nexusportal_runtime` role.
- Database schema migration: `db/migrations/0001_device_voice.sql`.
- ChatGPT custom MCP connector capability must be enabled in `NEXUSPORTAL_MCP_CONNECTORS_JSON` with a connector name containing `chatgpt`. Other connector entries may query status, but cannot approve pairing, read submitted speech, queue speech, or revoke the phone through the MCP tool handler.
- The current production app/relay must be deployed from this feature branch before the phone can pair. The Android app must be rebuilt and installed; the old manual ping-only APK cannot perform pairing or speech delivery.

## Pairing and use

1. In NexusPortal, tap **Pair this phone**. It generates a 10-minute code.
2. In the ChatGPT NexusPortal chat, provide that code and ask for phone pairing approval. The ChatGPT connector calls `approve_device_pairing`.
3. Return to NexusPortal and tap **Check pairing approval**.
4. Tap **Start voice session** to grant session-scoped voice consent. A heartbeat makes Android status connected.
5. In ChatGPT, call `portal_status`; use its session ID for `speak_text`. To send speech in the other direction, tap NexusPortal's Android speech dialog, then tap **Send last transcript to ChatGPT**; ChatGPT can call `get_speech_transcript`.
6. Stop the voice session or use **Revoke this phone** / `revoke_device` to cut access.

## Limits and security boundaries

- This is turn-by-turn text/TTS, not full-duplex audio, a raw microphone stream, or ChatGPT's native voice mode. ChatGPT must invoke `speak_text`; custom MCP cannot force every answer to be spoken automatically.
- Android speech recognition is provided by the recognizer selected by Android; that provider may process audio using its own cloud service. NexusPortal relays only the returned transcript after the explicit Send action.
- The session ID binds commands to the current user-approved Android voice session. The MCP connector does not expose a verified per-ChatGPT-conversation identity here, so this implementation cannot cryptographically distinguish two ChatGPT conversations that share the same connector capability.
- No screen reading, screen capture, touch control, annotation delivery, microphone streaming, app code updates, or remote APK installation is exposed.
- Build and live-device verification have not been completed. Do not present the phone as connected until the updated app is installed, pairing is approved, and a fresh heartbeat is observed.
