# Third-Party Research Seeding Ledger

This file records repositories consulted for architectural/research enrichment. A listing here does **not** imply that third-party code was copied. Each entry must identify which ECHO branch it helped seed.

| Upstream | License checked | Use | ECHO branch seeded | Code copied? | Reciprocity status |
|---|---|---|---|---|---|
| AGI144348Outlook/dual-order-os | project-owned repository; review before cross-project copying | matrices / indices / libraries organization; bidirectional index precedent | algebra | No | Internal cross-project lineage recorded |
| sympy/sympy | BSD-style license in upstream LICENSE | symbolic algebra/expression architecture reference; independent-verifier direction | algebra | No | Candidate upstream-useful verification example/test only after contribution-guideline review |
| networkx/networkx | BSD-3-Clause | graph/relation/index architecture reference | algebra | No | Candidate provenance/bidirectional-index example only if generally useful upstream |
| lark-parser/lark | MIT | grammar/parser architecture reference for future symbolic grammar | algebra | No | Candidate Unicode symbolic grammar example/test after upstream review |
| spdx/tools + SPDX specifications | Apache-2.0 for spdx/tools; SPDX specifications/reference materials checked separately | machine-readable licensing/provenance discipline | algebra | No | Candidate interoperability report/test if a concrete issue is found |

## Required protocol

Before importing any external code or data:
1. Pin the upstream repository and revision.
2. Read the exact LICENSE and NOTICE files at that revision.
3. Check compatibility with Echo_Green_Future's license and commercialization restrictions.
4. Preserve all legally required notices and attribution.
5. Record upstream repo/revision/files, local destination, and the ECHO branch seeded.
6. Prefer learning from architecture/API concepts over copying implementation when copying is unnecessary.
7. Identify whether ECHO work produces a generally useful bug fix, test, example, documentation improvement, or interoperability result that can be offered upstream.
8. Never state that ECHO contributed upstream until an actual issue/PR/contribution exists and is linked here.

## 2026-09-25 algebra seed

The `algebra` branch established separate mathematical, Hebrew-operational, and Mashet-symbolic substrate registries plus formula, symbol, Notebook, and cross-substrate indices. External repositories above were used as research references; the seed contains no copied third-party source code.
