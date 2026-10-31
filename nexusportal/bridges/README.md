# NexusPortal bridge contracts

These are implementation foundations, not live AI or Android connections.

- `protocol.js`: strict, session-scoped message validation for renderer commands and consent state.
- `web-adapter.js`: browser-side message broker for local preview, with no screen capture.
- `android/NexusPortalBridge.java`: native WebView bridge sketch for **trusted bundled assets only**. It does not create an overlay or capture the screen.
- `vercel.json`: static hosting configuration when this folder is selected as Vercel project root. Do not point the unrelated `nexusportals-diagram` project here without inspecting its source and obtaining approval.

## Consent rules

Session creation and consent originate in the native/user-controlled shell, never from arbitrary AI annotation messages. Observe and annotate are separate grants. Stop revokes both. The bridge only accepts annotations when the session ID matches and annotation permission is active. No AI provider is connected by this module.

## Planned next native integration

Separate touchable bubble from `FLAG_NOT_TOUCHABLE` annotation overlay, foreground service, user-authorized MediaProjection, bounded WebView bridge, explicit pause and stop. Respect `FLAG_SECURE` and capture restrictions. Never use an untrusted remote page with `addJavascriptInterface`.
