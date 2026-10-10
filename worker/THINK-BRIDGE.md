# GPT /think bridge probe (inert)
Manual-only. GitHub Actions fetches the public GPT Worker /think endpoint, validates an exact JSON schema and inert status, and commits only `worker-evidence/latest.json` to `gpt-worker-output`. No bot-supplied path, branch, code, or commands are used. No model calls or autonomous schedule.

**Run:** Actions → GPT Manual Think Bridge Probe → Run workflow (main). The Cloudflare Worker must first be redeployed from `bridge-experiment` to include the inert `/think` endpoint (commit `a412d7a`). A 404 or schema mismatch fails closed.

**Security limitation:** GitHub's built-in token has repository-wide contents write during this job. The script restricts its push to the output branch, but strong enforcement requires repository branch rules and least-privilege reviews. No hourly schedule until budget metering and real inference tests are reviewed.
