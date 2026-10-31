# NexusPortal MCP connection abstraction from Group Canvas

Status: architectural extraction; not yet wired into Android APK or deployed MCP endpoint. Source inspected 2026-10-31: `AGI144348Outlook/Group_Canvas` main, `docs/CONNECTORS.md`, `src/api.js`, `src/commands/index.js`, `test/connector.test.js`. Keep Group Canvas independent; do not copy its room authority, credentials, or user data.

## Proven design in source

Group Canvas has an inbound MCP endpoint per external AI seat: `POST /api/rooms/:room/mcp/:key`. An authenticated owner calls `POST /api/rooms/:room/connector`, generating a random 64-hex key and storing only SHA-256 hash. A seat is created with `controller:'external'`, an editor-limited agent principal, and a connector record; deleting the seat revokes the link. MCP JSON-RPC methods include `initialize`, `ping`, `tools/list`, `tools/call`; tool names include `room_inspect`, `say`, `widget_create`, `widget_update`, `widget_delete`. The repo's `docs/CONNECTORS.md` reports local SDK and browser verification, not a live Cloudflare or ChatGPT/Claude connector verification.

## Adapt to NexusPortal (do not copy room model)

- **Connection registry:** `Connection {id, displayName, transport, endpoint, capabilityStatus, enabled}`. User may group connections in configuration UI; platform discovery is deferred to Issue #3.
- **Session:** `Session {id, connectionId, state: disconnected|connected|paused|stopped, observation: off|on, annotations: off|on}`. Session starts only on user action. Connection permission, observation permission and overlay permission are separate.
- **MCP tool surface (NexusPortal-specific):** `portal_status` (read only), `annotation_add` (validated type, normalized geometry, TTL), `annotation_clear` (own session), `session_pause` (pause annotations). Do not expose arbitrary Android tap/input, screen contents, or screen capture in v0.2.
- **Authorization:** bind every tool call to the currently authorized session and connection; deny stale/revoked/paused sessions; limit capabilities to explicit grants. Prefer short-lived, revocable credentials stored in Android Keystore-backed storage. Avoid long-lived raw URL keys in logs and UI.
- **Android bridge:** MCP messages must pass through a narrow local adapter to the existing annotation renderer. Renderer accepts validated commands only, not raw model responses. User-facing % opens configuration; long-press % remains Stop. AI connection and screen observation remain off by default.
- **Connection direction matters:** Group Canvas is primarily an **MCP server** receiving inbound AI client calls. NexusPortal may need both a server for AI clients to invoke tools and a client to connect outward to MCP servers. Do not claim Group Canvas's inbound URL automatically makes NexusPortal a client for every installed AI app.

## Extraction map

| Group Canvas | NexusPortal analogue | Reuse |
|---|---|---|
| Connector create / revoke | Session connector pairing / disconnect | lifecycle pattern |
| External AI seat | Per-session AI connection identity | identity model, not room seat UI |
| `room_inspect` | `portal_status` | bounded read-only tool |
| `widget_create/update/delete` | `annotation_add/clear` | schema validation and receipt concepts |
| Role enforcement in command processor | Android bridge capability gate | central enforcement pattern |
| Owner room token, room Durable Object, room budgets | none in first Android prototype | intentionally exclude |

## Acceptance gates before claiming connection works

1. Android configuration screen opens from app and floating %; no regressions to working overlay.
2. Manual connection to a known test MCP endpoint completes protocol initialize and tools/list, with exact transport support documented.
3. AI tool call adds an annotation to overlay only while user session is active.
4. Disconnect/revoke and Stop reject later tool calls; observation remains disabled unless independently granted.
5. Separate on-device and deployed endpoint verification, with logs and no credentials in evidence.

## Current limits

This is a source-backed architectural abstraction only. No Group Canvas source code was copied, no live connection was attempted, and no tests were run. Before any repository tests, follow Timothy's pre-test issue and post-test evidence protocol.
