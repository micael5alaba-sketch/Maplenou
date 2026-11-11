# CS-10 Validation — Languages, themes, documentation sites, assistant, design, agent files

**Work package:** CS-10 (requested by the project owner on 2026-10-09)
**Result:** PASSED — technically validated, awaiting project-owner approval
**Date:** 2026-10-09

## Scope requested

1. Design mockups of the frontend.
2. Three documentation sites: users, internal Maplenou staff, developers.
3. Fast search and a chatbot for every documentation site.
4. Language and theme switch for the documentation and the web interface.
5. Migration from MkDocs to Zensical (editable with Zensical Studio).
6. References in the chatbot's answers.
7. `AGENTS.md` / `CLAUDE.md` files so any AI agent can understand and reproduce the project.
8. Ollama for the documentation chatbot.
9. Review of what the project structure is missing or does not need.
10. Fix: `docker compose up` failed with "Bind for 0.0.0.0:5432 failed: port is already allocated"; owner has no Java.

## Evidence

| Check | Result |
| --- | --- |
| Backend `./mvnw verify` (V2 migration, notification facts) | 53 unit + 62 integration tests passed |
| Frontend lint, typecheck (FR/EN key parity), unit tests, production build | passed — 29 tests |
| Browser end-to-end (desktop + mobile), incl. new "language and theme can be switched and are remembered" | 5 passed, 1 skipped (mobile-only test on desktop) |
| Assistant `pytest` (retrieval, citations, streaming, Ollama down, scope, validation, cache, rate limit) | 15 passed |
| `python documentation/build.py --strict` — six sites | passed (user 61, staff 83, developer FR 110, developer EN ~290 search sections) |
| Assistant against the built indexes (retrieval-only and with an Ollama-compatible stub) | answers with references linking to exact sections |
| Language switch in the docs keeps the page (`/developer/en/guide/testing/` → `/developer/fr/guide/testing/`), falls back to the home page when the page has no translation | verified in a browser |
| Visual review of the app and docs in FR/EN, light/dark, desktop/mobile | screenshots in [Mockups and screens](../ux-ui/screens.md) |
| Docker images (`documentation/Dockerfile`, `assistant/Dockerfile`) | `docker compose config` valid; images not built here (Docker Hub not reachable from the build environment) — build them once with `docker compose --profile docs up --build` |
| Real Ollama models | not downloadable in the build environment; the assistant was tested with a stub of the Ollama API and in retrieval-only mode |

## Decisions

DEC-CS10-01 … DEC-CS10-15 in [decisions.md](../architecture/decisions.md).

## Known limits

- Push notifications (Firebase) and backend error details remain French (UPGRADE-PLAN "Message templates per language").
- Category names come from the database in French and are shown as stored in the English UI.
- *(Iteration 2, superseded in iteration 3 by the Claude Design canvas — DEC-CS10-09.)* The Figma file was on the owner's Starter plan: one variable mode per collection, so light and dark are two
  collections (`Color`, `Color · Dark`); screenshots could not be uploaded to Figma from the build environment and
  are kept in `docs/ux-ui/screens/`.
- Zensical Studio is a public beta under a commercial licence; the sites build with the open-source `zensical` CLI.

## Iteration 3 (2026-10-09)

Owner requests: mockups in Claude Design for every device in light and dark, one language per page; smaller
Docker images and less disk; remove the unused Ollama container; `.env` for every Compose value; missing project
files; AI-agent files and documentation updated; backend processing and manual tests explained; caching.

| Check | Result |
| --- | --- |
| Backend `./mvnw verify` (incl. `CachingIT`) | 53 unit + 64 integration tests passed |
| Frontend lint, typecheck, unit tests, production build | passed — 32 tests |
| Assistant `uv run pytest` | 20 passed |
| `scripts/smoke-test.sh` against the running stack | passed (health, ETag/304, create → assign → reply → resolve, 403, EN login page, docs) |
| `docker compose config` with `.env.example` | valid; Compose stops with a clear message when a value is missing |
| Design canvas | 5 pages, 53 artboards (overview + 4 × 12 screens) |
| Docker images | not built here (Docker Hub / ghcr.io not reachable from the build environment) — build with `docker compose --profile docs up -d --build --wait` |

## Iteration 4 (2026-10-14)

Owner requests: test the API with Postman and Swagger; documentation on port 8006; fix the Ollama setup in
Docker; explain the backend; connect Firebase as described in the roadmap and the ticket specification.

| Check | Result |
| --- | --- |
| Backend `./mvnw verify` (incl. push-status, test push, invalid token removal, 502 in `NotificationIT`) | 53 unit + 66 integration tests passed |
| Postman collection with newman against the demo backend | 28 requests, 36 checks passed |
| Swagger UI `/swagger-ui.html`, `/v3/api-docs` (new endpoints present) | 200; `openapi.json` regenerated |
| Frontend lint, typecheck, unit tests (incl. `pushConfig.test.ts`), build, browser tests (incl. push card and `/?ticket=` redirect) | 35 unit tests, 6 browser tests passed |
| Browser push with a test Firebase configuration (headless Chromium) | card states and localized error verified; real delivery needs the owner's project |
| `uv run python documentation/build.py --strict` | six sites built |
| Assistant tests | 20 passed |
| `docker compose config` with the `docs` and `docs` + `ollama` profiles | valid |
| Firebase delivery to a real browser/device | needs the owner's Firebase project (docs/guide/firebase.md §4) |
| Docker images and the Windows Ollama connection | not runnable in the build environment (registries blocked); `scripts\doctor.ps1` diagnoses them on the PC |

## Iteration 5 (2026-10-29)

Owner requests: run the assistant on the free Ollama cloud plan (no local Ollama); move this module into the
Maplenou repository without losing anything; count weekends and public holidays in SLAs; build a public website
for the marketplace, separate from the ticketing app.

| Check | Result |
| --- | --- |
| Backend `mvn verify` (incl. `ContinuousCalendarTest`, `SlaModeIT`, `MaplenouIntegrationIT` with `FakeMaplenouCore`) | 58 unit + 72 integration tests passed |
| Bakend `./mvnw test` (incl. `SupportBridgeIntegrationTest`, `SupportBridgeDisabledIntegrationTest`, internal API on real orders in `OrderLifecycleIntegrationTest`) | 89 tests passed |
| Postman collection with newman against the demo backend on 8081 | 29 requests, 38 checks passed |
| `/v3/api-docs` with `MAPLENOU_LOGIN_ENABLED=true` | 42 paths; `openapi.json` regenerated |
| Frontend lint, typecheck, unit tests, build (Next.js 16.4.3), browser tests on 3001 → 8081 | 35 unit tests, 6 browser tests passed |
| Assistant tests (incl. `test_cloud.py`: key sent as Bearer, no pull, `think`) | 24 passed |
| `uv run python documentation/build.py --strict` | six sites built, no issues |
| `Site_vitrine` lint, typecheck, tests, build; pages with and without the Maplenou API; FR/EN and theme switch; 390 px width | passed — no horizontal scroll, fallback content when the API is down |
| Real calls to ollama.com, Docker image builds, Bakend + ServiceClient together on the PC | not runnable in the build environment (ollama.com and registries blocked); `scripts\doctor.ps1` checks the key, the model and Bakend on the PC |
