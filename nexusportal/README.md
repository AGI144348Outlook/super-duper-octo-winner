# NexusPortal

NexusPortal contains a standalone browser renderer and an Android prototype. The renderer is a local demo; Android pairing and voice operate through the authenticated Vercel MCP relay.

## Android ↔ ChatGPT voice relay

See [the v0.7 implementation notes](bridges/mcp-v0.7-implementation.md) for the pairing flow, relay tools, deployment requirements, security limits, and phone setup. The phone displays a one-time code; there is no long-lived manual bearer token to type. Speech is user-initiated and turn-by-turn.

## Renderer

Open [renderer/index.html](renderer/index.html) in a browser, or serve this directory as static files. The renderer demo requires no AI provider or Android permission and renders local demonstration commands only.

## Structure

- [renderer/](renderer/README.md) — HTML Canvas and % controls, annotation command schema
- [adapters/](adapters/README.md) — Android, Vercel and provider integration boundaries
- [bridges/](bridges/README.md) — MCP relay and message contracts
- [research/](research/README.md) — architecture, provenance, symbolic and bidirectional-text research

## Safety boundaries

The Android app does not read other apps' screens or perform touch control. Voice input is a tap-launched Android recognition dialog; only a transcript explicitly submitted by the user reaches the relay. Bubble speech uses Android text-to-speech for a bounded ChatGPT command during an active user-approved voice session. Remote app updates are not part of the relay.
