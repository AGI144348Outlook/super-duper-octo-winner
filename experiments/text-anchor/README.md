# Text-anchored highlighting experiment

Status: **source prototype only — not compiled, integrated or device-tested**.

## Problem
AIBubble's existing coordinate highlight remains at fixed screen coordinates while ChatGPT text scrolls away.

## Prototype
`TextAnchorTracker.kt` re-finds a visible accessibility node containing the selected text after a scroll or window/content change. It emits new bounds, or null when the target disappears. It does not inject touches, collect passwords, open a network connection or modify the working Android app.

## Integration checklist
1. Extract `AIBubble.zip` in the isolated branch and review `BridgeService.kt`, overlay drawing and `accessibility_config.xml` before integration.
2. Attach the tracker to an explicitly started highlighting session; stop tracking on session stop, service interruption or permission revocation.
3. Debounce accessibility events; call `refresh(rootInActiveWindow)` on the service main thread. Do not retain accessibility nodes between events.
4. Convert emitted screen bounds to overlay-local coordinates (account for insets and display changes). Repaint existing highlight on bounds updates; hide it on null.
5. Add a text-selection protocol to the existing connector. Keep coordinate highlighting backward-compatible.
6. Run controlled tests on scrolling, off-screen text, repeated text, app switches, password screens and accessibility permission revocation. Post results to Issue #14.

## Limitations
This prototype anchors to the **whole matching accessibility node**, not individual word glyphs. A long paragraph may be one node. Exact word highlighting requires text character bounds, layout information or another explicitly approved method. Duplicate phrases resolve to the first visible match. Accessibility event volume and overlay behavior have not yet been measured.

## Scope
AIBubble only. This file is not part of the packaged APK. Do not deploy the existing Cloudflare relay from this experiment.
