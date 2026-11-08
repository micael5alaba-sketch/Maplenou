# Repository tour

One repository holds the backend, the frontend, the documentation sites, the documentation assistant, the project
records and the CI. This page lists what each folder contains and where new work belongs.

## Top-level layout

```text
.
├── AGENTS.md                      instructions for AI coding agents (CLAUDE.md imports it)
├── README.md · CONTRIBUTING.md · CHANGELOG.md · RELEASE.md
├── SECURITY.md                    how to report a vulnerability
├── LICENSE                        proprietary licence (placeholder, to be confirmed by Maplenou)
├── .editorconfig                  same formatting in every editor (UTF-8, LF, indentation)
├── .dockerignore                  allow-list build context of documentation/Dockerfile (repository root)
├── docker-compose.yml             local stack: postgres, backend, frontend (+ docs profile: docs, assistant; ollama profile: ollama)
├── docker-compose.test.yml        every test suite in containers
├── docker-compose.override.example.yml   publishes PostgreSQL on host port 5433 (copy to *.override.yml)
├── .env.example                   every variable of the Compose files, documented (copy to .env)
├── zensical.toml                  English developer documentation site (docs/)
├── pyproject.toml · uv.lock · .python-version   documentation toolchain (zensical 0.0.69)
├── .github/
│   ├── workflows/                 backend.yml, frontend.yml, assistant.yml, e2e.yml, docs.yml
│   ├── ISSUE_TEMPLATE/            bug_report.yml, change_request.yml, config.yml
│   ├── PULL_REQUEST_TEMPLATE.md
│   ├── CODEOWNERS                 automatic reviewers (placeholder @OWNER)
│   ├── copilot-instructions.md
│   └── instructions/mermaid.instructions.md
├── backend/                       Spring Boot 4.1 API (Java 21, PostgreSQL 16, Flyway)
├── frontend/                      Next.js 16 web app (React 19, TypeScript)
├── assistant/                     documentation assistant (Python 3.12, Starlette, Ollama, uv)
├── documentation/                 build script, shared assets and the user/staff/French developer sites
├── docs/                          English developer site: this guide + the project records
├── scripts/                       test, smoke-test, doctor, env-sync, clean, backup (.sh for macOS/Linux, .ps1 for Windows)
├── deploy/                        Caddyfile and guard of the public profile (domains, HTTPS)
├── monitoring/                    Prometheus configuration, alert rules, Grafana provisioning (monitoring profile)
└── secrets/                       secret files (Firebase key), git-ignored except README.md, mounted read-only into the backend
```

## Backend — `backend/`

| Path | Content |
| --- | --- |
| `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/` | Maven build (parent `spring-boot-starter-parent` 4.1.1) and wrapper |
| `Dockerfile`, `.dockerignore` | multi-stage build: `eclipse-temurin:21-jdk-noble` (Maven wrapper, layered jar) → `eclipse-temurin:21-jre-alpine`, non-root user, health check |
| `src/main/java/com/maplenou/support/` | application code, one package per module (see [Backend](backend.md)) |
| `src/main/java/.../<module>/domain`, `application`, `application/port`, `adapter/in/…`, `adapter/out/…` | each module (`ticket`, `sla`, `notification`, `attachment`, `context`, `identity`, `report`) in hexagonal layers; `shared` (kernel), `platform` (technical base), `demo` — see [Backend](backend.md) |
| `ticket/application/LegacyImportService.java`, `ticket/adapter/in/web/LegacyImportController.java`, `context/adapter/out/maplenou/HttpLegacyTicketSource.java` | one-time import of the former Maplenou support tickets from Bakend `/internal/v1/support-tickets` |
| `src/main/resources/application.yml` | configuration, `maplenou.*` settings and the `demo` profile |
| `src/main/resources/db/migration/` | Flyway migrations `V1__support_schema.sql`, `V2__notification_i18n.sql`, `V3__maplenou_integration.sql` (legacy columns, `support_staff_assignments`), `V4__configurable_vocabulary.sql` (statuses, transitions, priorities, SLA policies, permissions, context references), `V5__outbox_feedback_ai.sql` (outbox, templates per language, satisfaction, canned replies, AI suggestions, external actions) |
| `src/test/java/.../` | `ArchitectureTest` (ArchUnit layer and module rules), unit tests (`*Test`: `ticket/domain/WorkflowTest`, `TicketAccessPolicyTest`, `sla/domain/*`, adapters `CloudinaryStorageTest`, `ClamAvScannerTest`, `BakendContractTest`, `CsvWriterTest`) and integration tests (`it/*IT`, base class `IntegrationTest`; `SupportFeaturesIT`, `OperationsIT`, `MaplenouIntegrationIT` with the fake Bakend `it/FakeMaplenouCore`) |
| `http/support-api.http` | manual request collection (VS Code REST Client, JetBrains HTTP Client) — see [Backend processing](backend-processing.md) |
| `http/postman/` | Postman collection `Maplenou-Support-API.postman_collection.json` (40 requests, 8 folders, 59 checks) and environment `Maplenou-local.postman_environment.json` — see [Test the API](api-testing.md) |
| `README.md` | backend run instructions and environment variables |

## Frontend — `frontend/`

| Path | Content |
| --- | --- |
| `src/app/` | App Router routes: `login`, `(requester)/aide…`, `support/…`, `api/` (BFF route handlers, `api/push-config` for the public Firebase values, `api/auth-config` for the sign-in mode `AUTH_MODE`) |
| `src/components/` | shared components (`ui.tsx`, shells, `TicketQueue`, `Conversation`, `TicketTools` — AI suggestion, Maplenou actions, satisfaction, canned replies —, `HelpSearch`, `Preferences`…) |
| `src/lib/` | API client (`api.ts`, `useApi.ts`, `apiCache.ts`), configurable vocabulary (`vocabulary.tsx`), session, types, labels, attachments, browser push (`push.ts`) |
| `public/push-sw.js` | service worker that shows push notifications and opens the ticket on click (no caching) — see [Firebase](firebase.md) |
| `src/lib/i18n/` | FR/EN dictionaries and helpers (`messages/<namespace>.ts`) |
| `src/app/globals.css` | design tokens (light and dark) and component styles |
| `e2e/` | Playwright scenario (`scenario.spec.ts`) and fixtures |
| `I18N.md` | languages and themes rules |
| `Dockerfile`, `.dockerignore`, `next.config.ts` | multi-stage `node:22-alpine` image with the standalone server, security headers |
| `package.json`, `package-lock.json` | pinned Next 16.4.3 / React 19.3.0, scripts `dev`, `build`, `lint`, `typecheck`, `test`, `e2e` |

## Assistant — `assistant/`

| Path | Content |
| --- | --- |
| `app/main.py` | HTTP API (`/api/assistant/health`, `/api/assistant/ask`), rate limiter, validation, start-up tasks (indexes, model pull, embeddings) |
| `app/service.py` | retrieval + generation, references, streaming, fallbacks |
| `app/index.py`, `app/text.py` | loading `search.json`, BM25, cosine similarity, rank fusion, tokenizer |
| `app/ollama.py` | minimal Ollama HTTP client (including model pull) |
| `app/cache.py` | `TtlLru`, the bounded answer cache |
| `app/prompts.py` | system prompts and fixed texts in French and English |
| `app/config.py` | settings from environment variables |
| `tests/` | pytest suite with a fake Ollama (`test_cloud.py`: Ollama cloud key, `think`, no pull, no embeddings) |
| `pyproject.toml`, `uv.lock`, `.python-version` | uv project: pinned dependencies, `dev` group (pytest), pytest settings, Python 3.12 |
| `Dockerfile`, `.dockerignore` | stages `builder`, `test` (runs pytest) and `runtime` |

## Documentation — `documentation/` and `docs/`

| Path | Content |
| --- | --- |
| `documentation/build.py` | prepares and builds the sites, writes the portal page |
| `documentation/Dockerfile`, `documentation/nginx.conf.template` | docs image: builds the sites (strict) and serves them with nginx |
| `documentation/serve.py` | local preview of the built sites without Docker (plays the role of nginx) |
| `documentation/shared/` | CSS, language switch and assistant widget copied into every site |
| `documentation/user/{fr,en}/` | public user guide (`zensical.toml` + `docs/`) |
| `documentation/staff/{fr,en}/` | internal guide for support staff |
| `documentation/developer/fr/` | French developer guide; links to the English records |
| `docs/` (with root `zensical.toml`) | English developer guide (`docs/guide/`) and project records |
| `docs/project/`, `docs/requirements/`, `docs/ux-ui/`, `docs/architecture/`, `docs/diagrams/`, `docs/validation/`, `docs/governance/` | project records, maintained in English only |

## Scripts and secrets — `scripts/`, `secrets/`

| Path | Content |
| --- | --- |
| `scripts/test.*` | every test suite in containers |
| `scripts/smoke-test.*` | end-to-end check of a running stack (also prints the push channel) |
| `scripts/doctor.ps1`, `scripts/doctor.sh` | read-only diagnosis: Docker, `.env` vs `.env.example`, `docker compose config`, host ports, the computer's Ollama, containers and their logs, assistant health |
| `scripts/clean.*`, `scripts/backup.*` | free disk space; back up and restore the database and attachments |
| `secrets/` | secret files mounted read-only at `/run/secrets/maplenou` in the backend (`firebase-service-account.json`); everything except `README.md` is git-ignored — see [Firebase](firebase.md) |

## Generated and ignored files

These are produced by builds and listed in `.gitignore`; never commit or edit them by hand:
`site/`, `.cache/`, `docs/assets/maplenou/`, `documentation/*/*/docs/assets/maplenou/`, `documentation/*/*/site/`,
`frontend/node_modules/`, `frontend/.next/`, `backend/target/`, `.venv/`, `assistant/.venv/`, `.env`,
`docker-compose.override.yml`, `backups/`, `secrets/*` (except `secrets/README.md`). `scripts/clean.sh` (`scripts\clean.ps1`) deletes the build outputs among
them.

## Where to put new things

| You are adding… | Put it in… |
| --- | --- |
| A backend business capability | a package under `backend/src/main/java/com/maplenou/support/` (web → service → domain → repository) |
| Framework-free ticket rules | `ticket/domain/` (with a plain unit test) |
| A database change | a new `backend/src/main/resources/db/migration/V4__<description>.sql` — never edit V1, V2 or V3 |
| An external system | a port interface plus adapters, like `context/EcosystemGateway` or `notification/PushSender` |
| A web screen | `frontend/src/app/…/page.tsx`, its strings in `frontend/src/lib/i18n/messages/<namespace>.ts` |
| A reusable UI element | `frontend/src/components/`, colours as tokens in `globals.css` |
| User-facing help | `documentation/user/fr/docs/` **and** `documentation/user/en/docs/` (same file names) |
| Support staff procedures | `documentation/staff/fr/docs/` and `documentation/staff/en/docs/` |
| Developer guide pages | `docs/guide/` (English) and `documentation/developer/fr/docs/guide/` (French) |
| Requirements, decisions, validation reports | the matching folder under `docs/` (English only) |
| An improvement idea | `docs/governance/UPGRADE-PLAN.md` first — see [Contributing](contributing.md) |
| A CI job | `.github/workflows/service-client-*.yml` at the root of the Maplenou repository (GitHub only reads workflows there) |
| A configuration value of the Docker stack | a variable in `.env.example` used as `${VAR:?…}` in the Compose file — never a hard-coded value |
| A cache | a bounded cache (size and lifetime) — see [Caching and disk space](caching.md) |

## Branches and checks

`main` is the production branch (Render deploys it). Work on a branch whose name starts with `ci/`
(for example `ci/service-client-cs12`) so that the GitHub Actions run before anything reaches `main` (DEC-CS12-01):

1. `git switch -c ci/<topic>` → commit → `git push -u origin ci/<topic>`: every workflow whose folder changed runs
   on the push (`push: branches: [main, "ci/**"]`).
2. Open a pull request `ci/<topic>` → `main`: the same workflows run on the merge result.
3. Read the results: GitHub → **Actions**, or `gh run list --branch ci/<topic>` then `gh run view <id> --log-failed`.
   Any workflow can also be started by hand (**Run workflow**, `workflow_dispatch`).
4. Merge only when every check is green.

| Workflow (`.github/workflows/`) | Runs when … changes | Same check on your computer |
| --- | --- | --- |
| `service-client-backend.yml` | `ServiceClient/backend/**` | `scripts/check.sh backend` |
| `service-client-frontend.yml` | `ServiceClient/frontend/**` | `scripts/check.sh frontend` |
| `service-client-e2e.yml` | backend or frontend | `scripts/check.sh e2e` |
| `service-client-assistant.yml` | `ServiceClient/assistant/**` | `scripts/check.sh assistant` |
| `service-client-docs.yml` | `ServiceClient/docs/**`, `documentation/**` | `scripts/check.sh docs` |
| `site-vitrine-ci.yml` | `Site_vitrine/**` | `cd ../Site_vitrine && npm ci && npm run lint && npm run typecheck && npm test && npm run build` |
| `backend-ci.yml` (Bakend) | `Bakend/**` | `cd ../Bakend && ./mvnw test` (PostgreSQL and Redis running) |
| `frontend-ci.yml` (Flutter) | `frontend/**` | `cd ../frontend && flutter analyze && flutter test` |

`scripts/check.sh` (Windows: `scripts\check.ps1`) runs the checks either natively or in Docker containers, see
[Testing](testing.md#check-with-or-without-docker).
