# Adapter boundaries

## Browser / Vercel

Static renderer preview and future account dashboard. Browser cannot create system-wide Android overlays or capture protected screens.

## Android shell (not yet implemented)

Native overlay permission, separate `TYPE_APPLICATION_OVERLAY` windows for touch-through Canvas and touchable `%` bubble, foreground service and explicit MediaProjection consent. Respect Android 14+ capture constraints, protected content and revocation. Load trusted bundled WebView assets; expose only narrow, authenticated and permission-checked bridge methods.

## AI connection (not yet implemented)

Session-scoped authorization, separate observation/annotation toggles, normalized renderer messages, per-user isolation, pause and stop. Launching a third-party AI app does not itself connect it to an MCP endpoint.

## Mashet Pad-derived patterns

The provided Pad uses a WebView/Java bridge, logical vs displayed bidi order, indexed symbolic keys, and timeline. These are patterns to adapt, not files to copy wholesale. See [research](../research/README.md).
