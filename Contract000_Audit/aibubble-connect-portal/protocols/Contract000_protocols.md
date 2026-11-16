# Contract 000 — protocols

Repository: AGI144348Outlook/super-duper-octo-winner. Source branch: resolve this represented branch folder through ../../BRANCHES.json. Category scope: Ordered rules and handoff obligations governing interactions; preserve trigger, preconditions, steps, returns and exceptions.

Status: **NOT_AUDITED — no discovery claim is implied by setup.**

## Accumulation protocol

Use append-only source-linked records. Each record needs stable discovery ID; repository/branch/commit/path/blob; relevant location; contextual definition; proposed MCP translation; dependencies; conflicts/alternatives; input/output/effects where relevant; evidence state; audit timestamp; and integration disposition. Keep SOURCE_OBSERVED, PROPOSED, EXECUTION_VERIFIED and UNKNOWN separate. Implementation presence, deployment availability, authorization and verification are separate fields. Do not assume a named glyph grants an executable operation. Reuse IDs for the same item and cross-reference other categories.

## Findings

None recorded by this setup. Add actual discoveries after reading each source file, one at a time. Record no-finding decisions and blockers in the checkpoint rather than inventing findings. Preserve uncertainty and source-specific meanings.

## Comparative consolidation and integration

Compare matching accumulative files across pinned branches. Identify exact duplicates, complementary records, conflicts and dependencies. Before proposing implementation merges, create a Consolidation and Integration Report issue in the receiving repository with source/target pins, record IDs, planned documentation consolidation, implementation units, adapters, validation, blockers and rollback. Append actual outcomes later. Updating these records alone is not an implementation merge.

### AIBUBBLE-PROT-001 — Manual APK build and Cloudflare relay workflow (SOURCE_OBSERVED / PROPOSED)

- **Source pin:** `AGI144348Outlook/super-duper-octo-winner`, branch `aibubble-connect-portal`, commit `8c5636235b1cac3f561ec4b2199f3d4f63e8b822`, path `.github/workflows/aibubble.yml`, blob `22960b3c9efc2161bac2107b8dd07207aa324f59`, complete file (EOF).
- **Observed context:** A `workflow_dispatch` GitHub Actions workflow defines two jobs: `apk` checks out source, extracts `AIBubble.zip`, sets up Java 17 / Gradle 8.9, assembles a debug APK, and uploads an artifact; `relay` separately checks out/extracts and invokes Cloudflare Wrangler using named GitHub secrets. This is workflow *definition*, not evidence of an execution, successful artifact, deployed relay or currently configured secrets.
- **Proposed MCP translation:** a read-only capability/evidence descriptor exposing workflow source reference, manually triggered build intent, APK artifact expectation, and relay deployment *potential* as separate steps. A later authenticated invocation adapter would need explicit operator approval, GitHub Actions permissions, Cloudflare scopes, expiry/revocation, and runtime receipts; no invocation authorized by this discovery.
- **Inputs/outputs/effects:** potential input is a manual workflow dispatch and repository/archive contents; potential outputs are a debug APK artifact and external relay deployment. Effects include GitHub CI compute and possible Cloudflare state changes; do not invoke during maintenance.
- **Dependencies:** GitHub Actions, `AIBubble.zip`, Java/Gradle, artifact upload, Wrangler and protected Cloudflare secrets. Archive contents and external state remain UNREAD/UNKNOWN.
- **Cross-references:** workflows, capabilities, permissions, tools, pipelines, verifications. **Conflicts/alternatives:** splitting build-only and deployment actions could reduce accidental external effects; not implemented or tested.
- **Evidence:** SOURCE_OBSERVED for YAML declaration; PROPOSED for MCP mapping; EXECUTION_VERIFIED = NO; authorization = NOT_GRANTED; deployment = UNKNOWN. Audit date 2026-11-15. Integration disposition: candidate only.

### AIBUBBLE-PROT-002 — Isolated manual Companion Turn Lab APK build (SOURCE_OBSERVED / PROPOSED)

- **Source pin:** `AGI144348Outlook/super-duper-octo-winner`, `aibubble-connect-portal` at `8c5636235b1cac3f561ec4b2199f3d4f63e8b822`, `.github/workflows/companion-turn-lab-android.yml`, blob `0cd3bbbb18015b49fe143284deffc4fed1dcdf79`, complete file (EOF).
- **Original context:** A manually dispatched GitHub Actions job checks out the separate `lab/contract-three-request-companion-app` branch with `persist-credentials: false`, uses Java 17 and Gradle 8.9, runs `:app:assembleDebug` under `labs/companion-turn-lab/android`, and uploads the resulting APK artifact, failing if absent. Job permission is `contents: read`. The checkout target is a branch name, **not a pinned runtime source commit**; the actual code built at execution time remains UNKNOWN.
- **Proposed MCP translation:** Read-only build-intent/evidence descriptor with distinct workflow-definition pin, resolved checkout commit at execution, build receipt, artifact digest, and explicit operator-controlled dispatch capability. No dispatch authority granted.
- **Inputs/outputs/effects:** Potential manual dispatch; build uses checked-out lab source and hosted CI compute; expected output is a debug APK artifact. No Cloudflare deployment step appears in this workflow; compare AIBUBBLE-PROT-001 for separate relay-deployment potential.
- **Dependencies/alternatives:** GitHub Actions, branch checkout, Java/Gradle and artifact storage. Recommend immutable checkout pin and receipt verification before any future reproducibility claim; not implemented. Cross-reference workflows, pipelines, permissions, verifications, capabilities and tools.
- **Evidence:** SOURCE_OBSERVED = workflow YAML only; PROPOSED = MCP mapping; EXECUTION_VERIFIED = NO; deployment = UNKNOWN; authorization = NOT_GRANTED. No CI was triggered. Audit 2026-11-16. Integration disposition: candidate only.
