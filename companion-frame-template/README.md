# Companion Frame Template — independent foundation

This is a new application, not a continuation of AIBubble, NexusPortal, or Companion Turn Lab. No code or credentials from those applications are dependencies.

## Milestone F0 — Android framing only

- **Normal Activity**: plain white screen, overflow (three vertical dots) in top-right; one option, `Switch to Bubble Mode`.
- **Collapsed overlay**: draggable circular bubble above other apps; tap to expand.
- **Expanded overlay**: non-full-screen white panel spanning most of the available width, beneath bubble selection region and above Android navigation bar, modeled on the user's Messenger screenshots; collapse back to circular bubble without launching the Activity.
- Expanded panel must not become a fullscreen Activity. Android overlay permission is user-granted. Service must not capture audio.
- No microphone, chat, AI connector, MCP, account registration, or background polling in F0.
- Persist framing preference locally; restore safely after service/process termination, subject to Android restrictions.
- Keep screen bounds, insets, drag, dismiss, and back-navigation explicit.

## Milestone F1 — Authentication (only after F0 device acceptance)

- Independent Vercel interface: `companion-frame-template` (project created; not deployed).
- Signup, login, email verification, password reset, logout, session revocation, and separate user records.
- Passwords never stored in plaintext; use mature authentication library/provider with Argon2id or equivalent recommended password hashing, per-user salts, throttling, secure cookies/tokens and server-side verification.
- Vercel website is the account and settings interface; Android app provides native overlay framing. A website alone cannot create an Android system overlay.
- No automatic access to existing ChatGPT conversation; this is not part of F0/F1.

## Acceptance checks (not yet run)

1. Fresh install displays blank white screen and top-right menu.
2. Menu opens exactly one action to switch framing.
3. Android overlay permission prompt only when requested.
4. Collapsed bubble can move and does not obstruct interaction elsewhere.
5. Tap opens a non-fullscreen blank white overlay; underlying app remains in place.
6. Collapse returns to bubble; restoration returns to full app only by deliberate action.
7. No microphone permission, capture, network requests or AI services.
8. Works after screen rotation and process restart as Android allows.

**Testing protocol:** Before any GitHub build, diagnostic, CI or other test, publish a pre-test entry in Issues/Discussions documenting the hypothesis, procedure, expected observations, and falsifiers; afterwards record actual observations and evidence in the same thread. No test has been executed for this branch.

## Project separation

GitHub branch: `fresh/companion-frame-template`. Keep all new source under `companion-frame-template/`. This branch is an isolated workspace inside an existing repository until a wholly separate repository is provisioned. Do not edit or deploy the old projects.
