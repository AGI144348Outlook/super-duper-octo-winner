# NexusPortal Android — overlay, MCP reachability and local voice preview

Native Java Android companion on the `feature/nexusportal-renderer-foundation` branch. The already working system overlay remains intact.

## Added in this milestone

- **MCP check:** manually enter an HTTPS URL ending `/api/mcp` and a **dedicated Android bearer token** (minimum 32 characters). The app performs a one-shot JSON-RPC `ping` using `Authorization: Bearer`. A valid result confirms authentication and network reachability **only**; it does not register an Android session, connect the existing ChatGPT conversation or enable remote annotation.
- **Local microphone preview:** tap **Tap to speak** to open Android's speech-recognition dialog. The speech provider handles any microphone permission prompt. Recognized words are shown inside the app. NexusPortal does not send them to its MCP bridge or ChatGPT, but the installed Android speech recognition provider **may process speech remotely** according to that provider's settings and policies.
- **Local speaker preview:** **Read last transcript aloud** uses Android TextToSpeech. **Stop voice and clear transcript** stops playback and clears the in-memory transcript. No always-on listener or background recording.
- The URL and bearer token are retained only in onscreen text fields for this Activity's lifetime, not saved to preferences or sent to GitHub. **Do not use the Vercel master bearer token**. Set up a separate revocable Android connector credential in the server's `NEXUSPORTAL_MCP_CONNECTORS_JSON`; never commit it or send it in chat.
- Existing floating `%`, touch-through annotation and explicit Stop are unchanged.

## Still missing

There is **no live ChatGPT voice conversation** and no MCP Android command/session transport. Live two-way assistant audio would require a supported voice model/service and a secure bidirectional session architecture. There is no screen observation, MediaProjection capture, background microphone, or remote device control. Android SpeechRecognizer availability depends on the phone's installed voice services.

## Build and verification

Follow pre-test [Issue #9](https://github.com/AGI144348Outlook/super-duper-octo-winner/issues/9). Use the GitHub Actions **NexusPortal Android APK** workflow and download `nexusportal-debug-apk` from the run. After building, inspect the Actions logs and install the updated APK over the old debug install if signatures match.

Manual checks: overlay still works; no network request without pressing the check button; invalid token rejected; valid *separate* connector token yields MCP ping success; speech dialog appears only when tapped; cancel leaves mic inactive; readback plays locally; clearing stops TTS; no screen/control permission is implied. Record observations vs interpretation and limitations in Issue #9.
