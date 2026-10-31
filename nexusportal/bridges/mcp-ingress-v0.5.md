# NexusPortal v0.5 — MCP ingress security contract (design, NOT deployed)

## Direction and trust boundary
A remote ChatGPT MCP client cannot directly invoke an Android service behind mobile NAT. An internet-reachable **MCP server/relay** must authenticate callers and forward only validated commands to a paired Android client over an outbound authenticated channel. Do not expose Android localhost or unauthenticated public webhooks. Group Canvas offers a connector lifecycle pattern, not a ready-to-use Android relay.

## Pairing and permissions
1. Android creates a device-scoped public/private key pair in Android Keystore and initiates pairing; relay stores public key, never private key.
2. User approves a named connection on Android; exchange short-lived, scoped credentials over TLS. Never place tokens in GitHub, logs, QR screenshots, or issue comments.
3. User starts a session explicitly; default observation OFF, touch control unavailable, annotations OFF. Service is sole local authority for annotation grant. Session grant must be enforced by Android receiving adapter, not just by UI.
4. Each remote call must carry connection identity, session ID, monotonically unique command ID, expiry, and annotation payload. Relay validates caller, scope, rate and payload. Android verifies signed or authenticated envelope, session binding, TTL, deduplication, and service-side grant before drawing.
5. Revocation invalidates relay credentials and local session grant. Stop disconnects the channel; reconnect must not restore grants. Fail closed on network loss and app restart.

## Proposed minimal MCP tools
- `portal_status`: no screen contents, only connection/session/annotation grant state.
- `annotation_add`: bounded normalized coordinates [0,1], allowed shape/color, duration 1–30 seconds; no text overlays or arbitrary URLs in first pilot.
- `annotation_clear`: only own session's annotations.
- `session_pause`: pause annotation delivery; cannot enable permission remotely.

## Acceptance and falsifiers
- Paired client can request one visible annotation only during an explicitly granted active session.
- No command draws when OFF, revoked, expired, replayed, wrong device, or wrong session.
- Disconnect/Stop blocks later commands, even if queued.
- ChatGPT connector compatibility verified separately; no claims until live tool invocation is observed.
- Screen capture and arbitrary touch remain unimplemented and ungranted.

## Implementation status
This is a protocol design only. No server, websocket, MCP transport, pairing or remote authentication code is deployed. v0.5 Android code only synchronizes local service-owned grant state to the home screen. Do not mistake SharedPreferences status mirroring for a security boundary or secure credential store.
