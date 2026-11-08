## What and why

<!-- What changed, why, and which matrix item (CS-xx) or change request it implements. -->

## Checklist (definition of done — AGENTS.md §5)

- [ ] Tests added/updated and green (`scripts/test.ps1 all` or the suites concerned)
- [ ] French **and** English updated (app strings, documentation pages, assistant texts)
- [ ] `python documentation/build.py --strict` passes
- [ ] Records updated when relevant (`docs/architecture/*`, `openapi.json`, DEVELOPMENT-MATRIX, PROJECT-CONTINUITY, CHANGELOG)
- [ ] No secret, no real customer data, `.env` not committed
- [ ] Docker images still build (`docker compose build`) and stay small (no new large base image)

## Business-rule or architecture impact

<!-- BR-xxx / DEC-xxx affected, or "none". -->
