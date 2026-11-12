# Maplenou Customer Service

This repository contains the Maplenou Customer Service and Ticket Management MVP.

## Project objective

Deliver the first functional ticket lifecycle:

`Create → Categorize → Assign → Investigate → Communicate → Resolve → Close`

The MVP integrates customer support with users, orders, payments, sellers, deliveries, refunds, and notifications.

**Status (2026-10-09):** CS-01 and CS-02 approved; CS-03 → CS-09 implemented and validated, awaiting owner
approval. Iteration 2 added FR/EN and light/dark/system themes, three documentation sites (user, staff, developer)
in Zensical with an Ollama documentation assistant, design deliverables and AI-agent instructions. Iteration 3
moved the mockups to a Claude Design canvas (EN/FR × light/dark × desktop/tablet/mobile), slimmed the Docker
images, moved every Compose value to `.env`, added bounded caching and helper scripts. Iteration 5 (2026-10-29)
moved this module into the Maplenou repository as `ServiceClient/` (sign-in with the Maplenou account, real orders
read from Bakend, import of the former tickets), counts SLAs 24 hours a day, 7 days a week, and runs the assistant on
Ollama cloud — see [`docs/project/PROJECT-CONTINUITY.md`](docs/project/PROJECT-CONTINUITY.md) and
[`docs/guide/maplenou-integration.md`](docs/guide/maplenou-integration.md).

AI agents and new contributors: start with [`AGENTS.md`](AGENTS.md).

## Quick start (full stack with demo data)

Requirements: Docker with Compose — nothing else (no Java, Node or Python on your machine).

```bash
cp .env.example .env                                 # required: every Compose value comes from .env
docker compose up -d --build --wait                  # app + API (waits until healthy)
docker compose --profile docs up -d --build --wait   # + documentation sites and the Ollama assistant
scripts/smoke-test.sh                                # checks the running stack (Windows: scripts\smoke-test.ps1)
scripts/doctor.sh                                    # something does not start? read-only diagnosis (Windows: scripts\doctor.ps1)
docker compose down                                  # stop (data is kept in volumes)
```

- App: <http://localhost:3001> — choose a demo profile (client, vendeur, livreur, agent, responsable, admin);
  switch language (FR/EN) and theme (system/light/dark) in the header or the Account page.
- API documentation: <http://localhost:8081/swagger-ui.html>.
- Documentation: <http://localhost:8006> (user, staff and developer guides in French and English, with the
  "Ask a question" assistant).
- Ollama: by default the assistant uses **Ollama cloud** (nothing installed, no model on your disk): create a free
  account on <https://ollama.com>, a key at <https://ollama.com/settings/keys>, and paste it in `.env` as
  `OLLAMA_API_KEY` (model `gpt-oss:20b`). A local Ollama (computer or `--profile ollama`) is still possible — see
  [`docs/guide/assistant.md`](docs/guide/assistant.md).
- Maplenou platform: sign-in with the Maplenou account, real orders and the import of the former tickets need
  Bakend running (port 8080) and the same `SUPPORT_SERVICE_TOKEN` on both sides — see
  [`docs/guide/maplenou-integration.md`](docs/guide/maplenou-integration.md).
- Design catalogue: <http://localhost:3001/design>. Mockups: the Claude Design canvas linked from
  [`docs/ux-ui/screens.md`](docs/ux-ui/screens.md).

Disk space: `scripts/clean.sh` (Windows: `scripts\clean.ps1`) removes build outputs, dangling images and old build
cache; `--deps` / `-Deps` also removes `node_modules` and virtual environments. See
[`docs/guide/caching.md`](docs/guide/caching.md).

PostgreSQL is not published to your machine, so a local PostgreSQL on port 5432 does not conflict. To reach the
database with a SQL client, copy `docker-compose.override.example.yml` to `docker-compose.override.yml` (`POSTGRES_HOST_PORT` in `.env`, default 5433).
Native developer setup (Java 21, Node 22, Python 3.12, PostgreSQL 16) is described in
[`docs/governance/REPRODUCTION.md`](docs/governance/REPRODUCTION.md) and the developer guide.

## Project continuity

Before making development decisions, read these files in this order:

1. `docs/project/PROJECT-CONTINUITY.md`
2. `docs/project/DEVELOPMENT-MATRIX.md`
3. `docs/project/PROJECT-PLAN.mmd`
4. `docs/architecture/README.md` and `docs/architecture/decisions.md`
5. Relevant UML/behavior diagrams in `docs/diagrams/`
6. `docs/governance/REFERENCES.md`

These files are the project's continuity and governance sources. Do not restart planning from zero when the project already contains a validated decision.

## Documentation

Six [Zensical](https://zensical.org) sites — three audiences in French and English — plus a local assistant:

| Site | Audience | Source |
| --- | --- | --- |
| User guide | customers, sellers, delivery drivers (public) | `documentation/user/{fr,en}/` |
| Support guide | Maplenou support agents, managers, administrators (internal) | `documentation/staff/{fr,en}/` |
| Developer documentation | developers (internal) | `zensical.toml` + `docs/` (EN), `documentation/developer/fr/` (FR) |

```bash
uv sync --locked                                         # zensical 0.0.69 from uv.lock
uv run python documentation/build.py --strict            # builds the six sites into site/
uv run python documentation/build.py --prepare-only && uv run zensical serve -a localhost:8007   # live preview of the developer site
```

Zensical Studio (editor extension) opens any of these folders thanks to their `zensical.toml`. GitHub Pages
publishes the user guide only. The assistant (`assistant/`) answers from the documentation with numbered
references, using Ollama (cloud `gpt-oss:20b` by default; locally `qwen2.5:3b` + `nomic-embed-text`).

## Repositories

- `backend/` — Spring Boot 4.1 API (Java 21, PostgreSQL 16, Flyway)
- `frontend/` — Next.js 16 web application (requester screens + support back-office)
- `assistant/` — documentation assistant (Python, Ollama RAG with references)
- `documentation/` — Zensical sites (user, staff, developer FR), shared assets, build script, Docker image
- `docs/` — project records (requirements, UX, architecture, validation, governance) + English developer guide
- `scripts/` — `test`, `smoke-test`, `doctor`, `clean`, `backup` (`.sh` and `.ps1`)
- `backend/http/support-api.http` — manual API walkthrough; `docs/guide/backend-processing.md` — how requests are processed
- `.env.example` — every configurable value (copy to `.env`, never commit `.env`)
- `docker-compose.yml` — local full stack · CI: `.github/workflows/service-client-*.yml` at the root of the Maplenou repository (backend, frontend, assistant, end-to-end, docs)
- `AGENTS.md` / `CLAUDE.md` — instructions for AI coding agents (root and per folder)

## Tests

Without any toolchain: `scripts/test.sh all` (Windows: `scripts\test.ps1 all`) runs everything in containers.
The full list (with and without Docker, bash and PowerShell) is in
[`docs/guide/testing.md`](docs/guide/testing.md#commands-to-verify-the-project).

| Command | What it runs |
| --- | --- |
| `cd backend && ./mvnw verify` | 53 unit + 66 integration tests (PostgreSQL via Testcontainers or `TEST_DATABASE_URL`) |
| `cd frontend && npm run lint && npm run typecheck && npm test` | lint, types (incl. FR/EN key parity), 35 unit/component tests |
| `cd frontend && npx playwright test` | browser end-to-end scenarios incl. language/theme switch (stack must be running) |
| `cd assistant && uv sync --locked && uv run pytest` | 20 assistant tests (fake Ollama) |
| `uv sync --locked && uv run python documentation/build.py --strict` | the six documentation sites, failing on broken links |

API by hand: Swagger UI (<http://localhost:8081/swagger-ui.html>) and the Postman collection
`backend/http/postman/` (28 requests, 36 checks, also runnable with `newman`) — see
[`docs/guide/api-testing.md`](docs/guide/api-testing.md). Push notifications (Firebase Cloud Messaging, browser and
Flutter apps): [`docs/guide/firebase.md`](docs/guide/firebase.md).

## Development principle

The MVP is developed incrementally around a complete ticket lifecycle. New product ideas must not silently enter the current scope. They must first be proposed, evaluated, and explicitly accepted.
