# Contract-000 · GPT inert Worker v0.1
Independent GPT-team prototype. **Not an autonomous agent or approved Contract-000 implementation.**
No network requests, LLM calls, secret access, deployments, or repository writes. Coordinator emits one-minute checkpoints; other four roles are named but do not yet execute tasks. Events and a SHA-256 manifest are written only to a local output directory.

## Local
`python3 -m unittest discover -s worker -p 'test_*.py' -v`
`python3 worker/worker.py --duration 300 --checkpoint 60` (fast simulation)
`python3 worker/worker.py --duration 300 --checkpoint 60 --live` (real five-minute clock)

## GitHub Actions
Manually run **Inert Worker Test**. It runs unit tests and the fast simulation and uploads artifacts. No unattended schedule is configured. Contract-000 text must be reviewed and imported before assigning real authority. The mirror branch remains separate and untouched.
