# NexusPortal — renderer foundation

This folder contains a **standalone, browser-testable renderer foundation**, separate from Contract-000 governance and Echo kernel. NexusPortal is the product; MCW denotes connection capability only.

## Open

Open [renderer/index.html](renderer/index.html) in a browser, or serve this directory as static files. The demo requires no AI provider, Android permissions or backend. It renders **local demonstration commands only**.

## Structure

- [renderer/](renderer/README.md) — HTML Canvas and `%` controls, annotation command schema
- [adapters/](adapters/README.md) — future Android, Vercel and provider integration boundaries
- [research/](research/README.md) — Mashet Pad architectural extraction, provenance and symbolic/bidi research
- [Issue #1](https://github.com/AGI144348Outlook/super-duper-octo-winner/issues/1) — bidirectional text and symbolic notation research questions

## Non-goals of this foundation

No MediaProjection, system overlay, live AI pairing, native Android build, account system, or autonomous device control is implemented. A browser Canvas cannot draw over other Android apps. No CI workflows or tests are configured or triggered by these files.

## Source handling

Mashet Pad v0.2 was provided as a ZIP for architectural review. This foundation is newly written and does not include Mashet Pad source, its keystore, or its generated Unicode tables. Reuse requires explicit adapter design and provenance tracking.
