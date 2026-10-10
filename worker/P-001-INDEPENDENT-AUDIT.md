# GPT independent review of Workshop P-001

Source: Echo_Green_Future branch library-shared, Workshop/pipelines/P-001-budgeted-inference/ (Claude contribution a2d847c).

## Findings (source inspection, not runtime verification)
- Good: input treated as data, fixed model and source, explicit max output, no GitHub credentials in Worker, workflow writes via Actions.
- **Critical: the claimed hard 4,500-neuron cap is not guaranteed.** KV get/put is not atomic. Concurrent requests can both read the same remaining balance, pass the precheck, and exceed the limit. A 10-minute cooldown implemented with separate KV get/put has the same race. The public /think endpoint has no caller authentication. GitHub's hourly clock alone does not prevent outside callers.
- **High: worst-case is estimated from characters / 4, not a verified tokenizer upper bound.** Therefore preflight cost is not mathematically worst-case. Real prompt usage may exceed the estimate.
- **High: missing usage fallback** charges estimated output length, not the reserved worst case, so unknown usage may be undercounted.
- **Medium: failed storage writes** may leave an inference unrecorded; the model call already occurred.
- **Medium: 11 offline checks cover sequential happy-path/error paths but not concurrent invocations, KV failures, missing usage, malformed usage, or UTC rollover.

## Required before autonomous GPT inference
1. Serialize budget reservations using a Cloudflare Durable Object or another atomic single-writer mechanism; KV alone is insufficient for a strict cap.
2. Reserve a conservative upper bound *atomically* before inference, settle measured usage afterward, and never refund on unknown usage or failure.
3. Authenticate /think requests or apply independent rate limiting to prevent public depletion.
4. Add concurrency, storage-failure, missing-usage, and rollover tests.
5. Verify pricing/model and platform account-level free-tier enforcement against current Cloudflare documentation.

Stage 0 GPT write loop passed. Stage 1 inference and schedule remain disabled until these gaps are addressed.
