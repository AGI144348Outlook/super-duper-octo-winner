# Companion Turn Lab — isolated app

This is a **new lab app**. Its source and identity must remain separate from the production Android app. Branch: `lab/contract-three-request-companion-app`; reference baseline: `main` (read-only). Do not deploy over an existing installed app or reuse its Android application ID.

## User-driven cycle
1. User presses configured **ע or פ** toggle once: start hands-free microphone capture.
2. User presses again: stop and finalize the user recording. **Silence never closes the turn.**
3. Send one authorized CONTRACT-{•} envelope containing three ordered user requests:
   - **R1** — Transcribe completed user audio into readable text in the selected companion's chat.
   - **R2** — The **companion** responds to the user's transcribed request.
   - **R3** — Record the companion's spoken response and associate it with the same transaction.
4. Parent receipt completes only when R1, R2 and R3 have independently verifiable fulfillment evidence. Delivery acknowledgement is not fulfillment. Report unsupported capabilities truthfully.

## Boundaries
This initial lab does not implement Neon, duplicate storage, a Vercel dashboard, onboarding, autonomous AI microphone activation or silence-triggered submissions. Provider-specific adapters may lack in-chat rendering or audio capture; never claim those stages worked without evidence. User audio and transcripts must not appear in public issue logs.

## Research tracking
Pre-test plan and results belong in [Issue #57](https://github.com/AGI144348Outlook/super-duper-octo-winner/issues/57). No test should run without first posting exact revision, fixtures, procedure and falsifiers. See #22 and #24 for earlier specifications.

## Native Android app (primary lab target)

Source: [android/](android/). This is a distinct Android application with application ID `org.aibubble.companionturnlab`, independent Gradle settings, Android manifest and Kotlin activity. It is not the existing AIBubble app. It starts the microphone only after a user tap and permission, stops only on the second tap, never ends due to silence, cancels without relay when backgrounded, and shows three honest request receipt statuses. Local recordings stay in private cache. There is no companion adapter, transcription, speech playback/recording or network transport yet. **No build or device tests have been run.**

Main reference: `AIBubble.zip` (unpacked by the existing `.github/workflows/aibubble.yml`). The main project remains untouched. The earlier browser `index.html` is only an archived auxiliary prototype, not the target app.

### Companion adapters
A provider-neutral adapter translates CONTRACT-{•} stages into an authorized companion interface and reports independently verifiable capabilities and results. MCP availability does not automatically imply that the provider can insert a user message into an existing chat, render a transcript there, or export spoken audio. The adapter must report unsupported stages without marking them complete. No paid API is assumed as a prerequisite.
