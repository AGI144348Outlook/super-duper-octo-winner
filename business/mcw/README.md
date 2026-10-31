# MCW — Universal AI Companion

**Status: business candidate / proposal.** MCW is a public multi-user Android companion, not an Echo subsystem or personal-only tool.

## Product

A user invokes an AI connection through a floating `%` control. A companion app manages independently granted screen-observation permissions and draws transparent annotations. AI device control is out of scope for v1. User consent, stop/revoke, privacy, accessibility, and tenant isolation are essential free capabilities.

## Business model

- Free core features remain usable by everyone.
- Maintain a rolling window of two newest **optional** premium features.
- On release of a third optional feature, graduate the oldest of the two to free; never gate safety-critical controls.
- Pricing, demand, operating cost and provider interoperability require validation.

## Delivery boundaries

- **Product repository:** dedicated MCW engineering repository (not yet selected/created).
- **Vercel:** public website and onboarding, proposed.
- **Cloudflare:** potential MCP edge gateway and Durable Objects sessions, evaluate.
- **Render:** potential persistent gateway, evaluate; no MCW service deployed.
- **Neon:** potential tenant-scoped records, separate project proposed.
- **GitHub:** source, issues, releases and Android build workflows.

See [concept](concept.md), [roadmap](roadmap.md), and [business index](../README.md).

## Validation and governance

No claim of a working live AI-app MCP integration. Provider-specific support must be checked. Before any repository tests, publish the required pre-test Issue/Discussion; follow up in the same thread with observations, evidence, limitations and next steps. Contract-000 business work and product engineering remain separate.
