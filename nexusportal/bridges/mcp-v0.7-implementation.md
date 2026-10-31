# NexusPortal MCP v0.7 foundation

Endpoint source: `nexusportal/api/mcp.js` (Vercel project must use `nexusportal` as root directory).

**Current functionality:** JSON-RPC MCP initialize, ping, tools/list, tools/call with **only** `portal_status`. It reports Android `not_connected`. No relay, device pairing, annotation delivery, screen capture or touch control exists yet. This is not a working ChatGPT-to-Android connection.

**Default security:** Requests fail with 503 until `NEXUSPORTAL_MCP_BEARER_TOKEN` is configured as a Vercel environment secret (random 32+ characters). The endpoint then requires a matching Authorization Bearer token. Never commit a token or put it in GitHub Issues. This is a provisional developer transport, **not** the OAuth pairing required for the user's Custom MCP plugin screenshot. Do not remove deployment protection or publicly expose a usable token for convenience.

**Compatibility gap:** The ChatGPT Custom MCP plugin screenshot defaults to OAuth. Confirm whether manual OAuth or other authenticated connection modes are available before configuring. MCP streamable HTTP clients may require Accept handling, protocol negotiation, and OAuth discovery; these are not yet implemented. Do not tell the user to register this endpoint as working.

**Next engineering milestones:** (1) verify Vercel root directory and build, (2) implement OAuth-compatible authorization server or supported secure tunnel, (3) implement authenticated outbound Android channel and pairing, (4) server-side command queue with per-session consent and replay protection, (5) add `annotation_add`/`annotation_clear` only after authorization is enforced on device, (6) publish pre-test plan before each run and results in issue #8. Preserve the Android APK update compatibility investigation.
