# AIBubble Connect

Production portal: https://aibubble-connect.vercel.app

This Next.js portal uses Neon Auth for accounts and stores device registration,
MCP credentials and audit metadata in the `bubble_connect` PostgreSQL schema.
The Android application stays in the existing repository archive. The published
download is the pinned November 4 debug APK. Its original relay is independent
of the Connect API.

## Develop or redeploy

Use Node.js 24 and run `npm ci`, then `npm run build`. The Vercel project root
is this directory for Git deployments. Configure the following server-only
runtime values through the hosting secret manager:

- `NEON_AUTH_BASE_URL`: Managed Neon Auth URL, including its auth path.
- `NEON_AUTH_COOKIE_SECRET`: random secret of at least 32 characters.
- `DATABASE_URL`: a scoped PostgreSQL login for the schema below.

Run `sql/schema.sql` as the schema owner. Give the runtime role USAGE on
`bubble_connect` and SELECT and INSERT on its tables. Give UPDATE on
`device_pairings`, `devices`, `ai_connections` and UPDATE(id) on `users`.
Use a dedicated non-owner role. Add the portal origin to the managed auth
trusted domains. Configure production email verification and a sender before
opening registrations broadly. The initial hosted deployment uses the
connected Vercel project and Neon configuration without a new paid service.

## Device protocol

1. Sign in to NexusBoard and generate a five-minute code. Enter it in your
   own compatible device. Device claims supply a locally generated 256-bit
   credential and basic device metadata over HTTPS.
2. Check the reported label and version and approve the claim in NexusBoard.
   The authenticated owner approves exactly once. Database records store
   hashes for both code and device credential.
3. The device uses its credential in the Authorization header to check status.
   This updates its last-seen time after approval. Revoke it from NexusBoard
   to invalidate it. Device credentials expire after 30 days.

The optional source overlay in `android-overlay/` adds registration and
connection checks to the existing app. Extract `AIBubble.zip` into a separate
build checkout, then run `node android-overlay/apply.mjs /path/to/extracted-project`
from the portal directory. The overlay targets the unmodified 0.3 archive.
It uses Android Keystore to encrypt the credential and does not alter the
legacy relay. A new APK must be built, reviewed, tested on devices and signed
with the authorized release key before publication. The preview overlay sends
a heartbeat on app resume and on a manual status check.

## AI connections

The Streamable HTTP endpoint is
`https://aibubble-connect.vercel.app/api/connect/mcp`. Create a seven-day
credential in NexusBoard and enter it in an MCP client's HTTP Bearer field.
The single enabled tool is `session_status`. Each token is linked to one
portal account and can be revoked. Keep it out of URLs and chats.

ChatGPT OAuth onboarding and provider-specific connections require additional
implementation and live verification. An MCP connection is separate from
logging into ChatGPT. Current tool responses contain device status only.

## Device investigation

With the user present and after registering the experiment, use a harmless test
screen. Compare the two phones with Accessibility enabled and inspect
`session_status`, then `read_screen`, then `take_screenshot` on the correct
legacy connector. Record only outcomes:

- Accessibility can report no active window independently of MediaProjection.
- A missing CaptureService or no frame indicates a capture-stage failure.
- A JPEG locally but no MCP image indicates a relay, session or client issue.
- Confirm that the target AI receives the image content, verify both sessions
  are isolated, and stop each session on the device.

This portal does not receive screenshots or audio. Browser automation,
provider OAuth, user-controlled capture transport and signed APK release are
separate outstanding work. Preserve the repository's noncommercial license.

## Verification

Repository pre-test and follow-up: https://github.com/AGI144348Outlook/super-duper-octo-winner/issues/59

`scripts/verify-live.mjs` uses two labeled synthetic accounts to exercise
login, pairing, account isolation, MCP status and credential revocation. It
does not access devices, screen content, or provider accounts.
