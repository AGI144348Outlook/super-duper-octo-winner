# MCW proposed roadmap

| Phase | Scope | State |
| --- | --- | --- |
| M1 | Browser mock: bubble, renderer, provider selection | Prototype reported; not independently validated here |
| M2 | Native Android overlay, touch-through and return control | Planned |
| M3 | MediaProjection consent and permission-scoped observation | Planned |
| M4 | Authenticated MCP pairing, adapter and revocation | Planned |
| M5 | Multi-user platform, website and tenant-isolated storage | Planned |
| M6 | Public launch readiness, safety and accessibility review | Planned |

## Optional feature progression

Possible future features: spatial annotation memory, interactive guidance modes, multi-AI sessions. Two newest optional features may be premium; oldest graduates to free when a new one launches. Safety/privacy controls always remain free.

## Infrastructure decision gate

Compare Cloudflare Workers/Durable Objects against Render for session lifecycle, connectivity, cost, operational burden and reliability before committing to a production gateway. Neon and Vercel remain candidates, not assumed dependencies.
