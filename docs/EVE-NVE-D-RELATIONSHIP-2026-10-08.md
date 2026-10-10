# EVE and NVE-D — Architectural Relationship and Claude Handoff
**Recorded:** 2026-10-08  
**Status:** Working architectural interpretation from discussion; confirm against original EVE design documents before treating terminology and all NVE assignments as canonical.

## Central relationship
**NVE-D is a specialized, recursive meta-specification research environment inside EVE, not EVE itself and not a governing authority over the other NVEs.**

Working hierarchy:
```text
EVE — Emergent Virtual Ecosystem (historical usage also includes Envelope Virtual Environment)
├── Shared architecture
│   ├── Registries
│   ├── Matrices
│   ├── Codices
│   ├── Data and storage
│   └── Governance and interfaces
├── NVE-A — blind structural discovery [working assignment]
├── NVE-B — informed symbolic discovery [working assignment]
├── NVE-C — reconciliation [working assignment]
└── NVE-D — meta-specification experimentation [working assignment]
    ├── IETF substrate
    ├── Kernel seed #
    ├── Candidate kernel expansion
    ├── Relational evaluation
    ├── Document Feedback Evaluation
    └── Meta-audit and revision
```

The NVE-A/B/C descriptions above follow recent experimental organization, **not a verified exhaustive historical inventory** of all EVE/NVE components. Verify against original EVE specifications before implementing system-wide changes. Likewise, EVE expansion terminology needs source confirmation.

## Distinct experimental responsibilities (provisional)
- **NVE-A:** Investigates structure without supplied symbolic interpretation.
- **NVE-B:** Investigates structure with additional symbolic information.
- **NVE-C:** Compares and reconciles their findings, retaining disagreements.
- **NVE-D:** Investigates the kernels, evaluation procedures, rules and assumptions that determine *how* findings are produced and judged—including NVE-D's own assumptions.

NVE-D's research question: **How does an environment determine what counts as a discovery?**

## Nesting and authority boundaries
- EVE contains NVE-D alongside other NVEs.
- An IETF-substrated NVE-D can host its own **finite experimental nested environments**, each with isolated state, snapshots, kernel hypotheses, checkpoints, and audit evidence.
- NVE-D may examine candidate rules from other NVEs and return documented evidence to EVE's governance mechanisms.
- **NVE-D does not automatically rewrite, promote, govern or override other NVEs, Registry 0, or EVE's protected components.** Experimental findings require independent review and explicit authorization for promotion.

## Relation to the kernel-seed discussion
Read alongside `docs/NVE-D-IETF-KERNEL-ARCHITECTURE-HANDOFF-2026-10-08.md`. Preserve the user's literal governing evaluation formula:
```text
(?):(?)::(?):(?)::|#|::(?):(?)::(?):(?)::#::(?)
```
The user specifies `(?)` may take `(?±,=?)` forms and `#` is the seed of potentially unbounded outward kernel expansion:
```text
.......←.......#.......→.......
```
The user's suprastrate–substrate–datastrate–dataset layering describes NVE-D's internal experimental architecture, **not a replacement for EVE's ecosystem-level architecture**. The difference between `|#|` and `#` remains unresolved. Do not equate these symbols with `|x|` or assign fixed computational meanings without testing.

## Claude handoff
1. Treat EVE as the containing ecosystem; NVE-D as one experimental member.
2. Verify original EVE/NVE terminology and any established roles from repository source before rewriting the architecture.
3. Keep NVE-D's IETF nested experimental facilities isolated from production and other NVEs.
4. Use Document Feedback Evaluation at stages 5, 7 and 8; retain contradictions, criteria, evaluator assumptions and audit history.
5. Test candidate kernels and rule interpretations without semantic imposition; preserve source expressions exactly.
6. Report measured results separately from design hypotheses; never claim finite runs prove infinite computation.

**Documentation only. No runtime behavior, governance permissions, protected algorithms, or registries are changed by this file.**
