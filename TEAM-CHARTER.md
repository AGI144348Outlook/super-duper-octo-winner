# GPT Contract-000 team repository

This repository is assigned to the GPT team. Its existing main branch is preserved. The `echo-upstream-mirror` branch is reserved for a read-only-source mirror of `AGI144348Outlook/Echo_Green_Future`.

## Rules
- Echo_Green_Future is upstream; synchronization is one-way into `echo-upstream-mirror`.
- Team-specific experiments belong on separate branches, not on the mirror branch.
- Contract-000 is authoritative only after its exact approved version is imported and verified.
- No autonomous execution, external API spending, or production deployment is authorized by this README.
- Claude controls Claude-team structure; GPT controls GPT-team structure, within approved constraints.

## Mirror
Use Actions > Mirror Echo Upstream > Run workflow. The workflow fetches upstream main and updates only the `echo-upstream-mirror` branch. It never rewrites this repository's main branch.
