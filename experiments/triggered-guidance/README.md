# AIBubble user-triggered guidance prototype

**Status:** Source-only implementation candidate. Not integrated, compiled, tested, deployed or connected to an AI reply service.

## User-approved behavior

- **Screen session**: independent on/off state; the Android host must show the required MediaProjection consent prompt before allowing capture. No continuous monitoring.
- **Voice session**: independent on/off state; Android TTS speaks a received instruction only when voice is on. Voice input/transcription is a separate future capability.
- **Screen Update bubble button**: one explicit press requests one redacted observation and one response from the selected AI/conversation.
- **Stop**: cancels local active generation, clears highlight and stops speech. The network layer should additionally cancel or expire pending requests.

## Current integration seam

`GuidanceSessionController.kt` defines a coordinator and three injected adapters: `ObservationSource`, `GuidanceTransport` and `GuidanceFeedback`. It does **not** assume a ChatGPT conversation can be remotely triggered. The transport must return `Pending` until a supported provider integration actually processes the request.

A connector may support AI-initiated `read_screen` and `highlight` while **not** supporting Android-initiated continuation of the same chat. The app must display “awaiting AI” rather than inventing a reply. For some providers a separate authenticated API conversation may be needed; this is not automatically the same as the ChatGPT app's conversation thread.

## Important implementation requirements

1. Obtain Android capture consent using platform APIs, and release capture when the session stops or the OS revokes it.
2. Block protected applications and sensitive UI content **before** sending accessibility text or screenshots; app allowlisting is preferable to relying on a blacklist alone.
3. Bind every update to an authenticated user, paired device, selected assistant and authorized conversation; reject cross-user and cross-conversation requests.
4. Enforce expiration, idempotency via requestId, rate limits, and replay protection in the relay.
5. Use Android main-thread dispatch for callbacks that modify UI, TTS or overlays.
6. Ensure Quick Settings and bubble controls reflect the same session state.
7. Keep screen and voice permissions independent; stop both on explicit Stop All.
8. Handle device lock, blank/protected screens and app switches conservatively.
9. Do not log screenshots, passwords, private messages or raw transcripts to GitHub.
10. Keep the existing working read/highlight connector and the production relay untouched until tested.

## Proposed acceptance tests (not executed)

- Screen off + voice on: no screen capture, voice can still speak authorized text.
- Screen on + voice off: one update press produces at most one observation and no speech.
- Permission revoked: all active guidance stops, highlight disappears, pending callbacks are ignored.
- Sensitive app: no observation or screenshot leaves the device.
- Double press: requests have distinct IDs and stale callbacks cannot overwrite newer guidance.
- AI unavailable: status remains pending/error, never fabricated success.
- Conversation switching: old response cannot appear in the new conversation.
- Scrolling text: text-anchor tracker updates bounds or hides off-screen targets.

See GitHub Issue #14 for preregistration and subsequent test evidence.
