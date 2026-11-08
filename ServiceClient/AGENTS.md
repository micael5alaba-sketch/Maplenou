# AGENTS.md — Maplenou Customer Service

Instructions for AI coding agents (Claude, Codex, Copilot, Cursor, Gemini…) and for humans who work like them.
This file is the canonical source; `CLAUDE.md` files only import it. Folder-specific rules live in
`backend/AGENTS.md`, `frontend/AGENTS.md`, `assistant/AGENTS.md` and `documentation/AGENTS.md`.

## 1. What this project is

A **customer-service and ticketing module** for the Maplenou marketplace ecosystem (Togo — currency FCFA (XOF),
time zone `Africa/Lome`). It lives in the Maplenou repository as `ServiceClient/`, next to `Bakend/` (core API,
port 8080), `frontend/` (Flutter app), `Admin_frontend/` (back-office, port 3000) and `Site_vitrine/` (public
website, port 3002). Integration with Bakend: `docs/guide/maplenou-integration.md`. Customers, sellers and delivery drivers ("requesters") open tickets; support agents,
support managers and administrators handle them with SLAs, internal notes, escalation and reopening.

| Part | Folder | Stack (pinned) |
| --- | --- | --- |
| REST API | `backend/` | Spring Boot 4.1.1, Java 21, PostgreSQL 16, Flyway, Maven wrapper |
| Web app (requester + back-office) | `frontend/` | Next.js 16.4.3, React 19.3.0, TypeScript 5.9, Node 22 |
| Documentation assistant | `assistant/` | Python 3.12, Starlette 0.48, Ollama cloud (`gpt-oss:20b`) or local (`qwen2.5:3b`, `nomic-embed-text`) |
| Documentation sites (user / staff / developer × FR / EN) | `documentation/`, `docs/`, `zensical.toml` | Zensical 0.0.69 |
| Project records (requirements, UX, architecture, validation, governance) | `docs/` | Markdown, English |

Architecture: **modular monolith of hexagonal modules** (domain / application + ports / adapters, checked by
`ArchitectureTest`), a backend-for-frontend (Next.js proxy with an httpOnly session cookie), domain events through a
transactional outbox, and a **configurable vocabulary** (statuses, transitions, priorities, SLA policies,
permissions as data). Read `docs/architecture/reference-architecture.md` before changing structure.

## 2. Read before you change anything

1. `docs/project/PROJECT-CONTINUITY.md` — where the project stands and what comes next.
2. `docs/project/DEVELOPMENT-MATRIX.md` — approved work items (CS-xx) and their status.
3. `docs/requirements/business-rules.md`, `business-decisions.md`, `authorization-matrix.md` — the rules (BR-xxx, DEC-xxx).
4. `docs/architecture/README.md`, `decisions.md`, `reference-architecture.md`.
5. The `AGENTS.md` of the folder you will touch.

Do not start a new plan when an existing one covers the task. **Only implement approved scope**; new ideas go to
`docs/governance/UPGRADE-PLAN.md` as proposals.

## 3. Run and test — Docker only (no Java, Node or Python needed)

```bash
cp .env.example .env                                # every Compose value lives in .env (never commit .env)
docker compose up -d --build --wait                 # app http://localhost:3001 (demo login), API http://localhost:8081
docker compose --profile docs up -d --build --wait  # + docs http://localhost:8006 and the assistant (Ollama cloud: OLLAMA_API_KEY in .env)
docker compose --profile docs --profile ollama up -d --build --wait   # local Ollama container instead (OLLAMA_URL=http://ollama:11434)
docker compose --profile antivirus up -d --wait     # + ClamAV (ANTIVIRUS=clamav)  ·  --profile monitoring: Prometheus + Grafana
docker compose --profile docs --profile public up -d --build --wait   # server: Caddy + HTTPS on SUPPORT_/HELP_/SITE_DOMAIN
scripts/env-sync.sh                                 # add to .env the variables a new version introduced (Windows: scripts\env-sync.ps1)
scripts/doctor.sh                                   # read-only diagnosis when something does not start (Windows: scripts\doctor.ps1)
scripts/smoke-test.sh                               # end-to-end check of a running stack (Windows: scripts\smoke-test.ps1)
scripts/test.sh all                                 # every test suite in containers (Windows: scripts\test.ps1 all)
scripts/check.sh [--native|--docker] [ci]           # same commands as the GitHub Actions, one line per suite (Windows: scripts\check.ps1)
scripts/clean.sh                                    # free disk space (Windows: scripts\clean.ps1)
```

PostgreSQL is not published to the host (avoids "port 5432 is already allocated"); use
`docker-compose.override.example.yml` (`POSTGRES_HOST_PORT`, default 5433) for a SQL client.

Native loops (optional): backend `./mvnw verify` with `TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/maplenou_test`
(otherwise Testcontainers); frontend `npm ci --legacy-peer-deps && npm run lint && npm run typecheck && npm test`;
assistant `cd assistant && uv sync --locked && uv run pytest`; docs `uv sync --locked && uv run python
documentation/build.py --strict`; e2e `npx playwright test` against a running stack; manual API calls with
`backend/http/support-api.http`, Postman (`backend/http/postman/`, `npx newman run …`) or Swagger UI
(`http://localhost:8081/swagger-ui.html`, see `docs/guide/api-testing.md`). Full command list: `docs/guide/testing.md`
("Commands to verify the project").

## 4. Rules that apply everywhere

- **Business rules live in the backend.** The UI only reflects them (`allowedActions` in ticket responses).
- **Two languages, always.** Every user-visible string exists in French and English (frontend namespaces,
  documentation pages, assistant texts). French is the reference vocabulary (CS-02 design system) — do not reword it.
- **Same file names in both documentation languages**, so the language switch keeps the reader on the same page.
- **Themes:** light, dark and system in the app and the docs. Use design tokens (CSS variables), never raw colours.
- **Errors:** RFC 9457 problem details with a stable `code`; the UI translates by `code`.
- **Migrations are immutable** once merged (`backend/src/main/resources/db/migration`): add `V<n+1>__*.sql`.
- **Security:** no secrets in the repo; the `demo` Spring profile (demo login, seed data) must never run in production;
  internal docs (staff, developer) are never published publicly; a public assistant runs with `ASSISTANT_SITES=user`.
- **Pinned versions:** keep `package-lock.json`, `uv.lock`, the Maven wrapper and image tags reproducible.
  Upgrades are proposals (UPGRADE-PLAN), not side effects.
- **Accessibility:** WCAG 2.2 AA — labels on inputs, status never shown by colour alone, keyboard reachable;
  text on brand/accent/danger fills uses the `--text-on-*` tokens (contrast 4.5:1 in both themes).
- **Configuration lives in `.env`.** `docker-compose*.yml` read every value as `${VAR:?…}`; never hard-code a port,
  password, model name or URL in a Compose file. Add a new variable to `.env.example` with a comment, and to the
  "Configuration" tables of the developer guide (FR and EN).
- **Small images, little disk.** Dockerfiles are multi-stage with `# syntax=docker/dockerfile:1.7`, BuildKit cache
  mounts (`/root/.m2`, npm cache, `.next/cache`, `/root/.cache/uv`), pinned slim/alpine bases, only runtime files in
  the last stage, a non-root user (uid 10001) and a `HEALTHCHECK`. Every build folder has a `.dockerignore`.
  Python dependencies are installed with `uv sync --locked --no-dev` from `pyproject.toml` + `uv.lock` (no
  `requirements.txt`, no `pip install`). Compose services use the shared `x-logging` rotation. Ollama is Ollama
  cloud (`OLLAMA_URL=https://ollama.com` + `OLLAMA_API_KEY`, default, nothing on disk), the one installed on the
  computer (`http://host.docker.internal:11434`) or the single `ollama` service of the `ollama` profile — never add
  model-pull containers or extra Ollama images, and never make another service `depends_on` ollama (it is optional).
- **SLA clock:** `SLA_MODE=always` (default) counts 24 hours a day, 7 days a week, weekends and public holidays
  included (DEC-CS10-16); `business-hours` keeps the working-day calendar. Durations shown to people use the
  calendar's day length (`/admin/sla-settings`).
- **Maplenou platform:** Bakend is the source of truth for accounts, orders, payments and deliveries. This module
  reads them through Bakend's internal API `/internal/v1/**` (header `X-Maplenou-Service-Token`, shared
  `SUPPORT_SERVICE_TOKEN`), signs people in through Bakend's `/api/auth/*`, and *asks* Bakend to act (return
  request, delivery release, message to the courier, push relay) through the same API — Bakend applies its rules and
  audits; this module never writes to Bakend's database.
- **Vocabulary is data:** statuses, priorities, transitions, SLA targets and permissions come from the database
  (`GET /vocabulary`); code decides by status *kind* and priority *rank*, never by a hard-coded label or code list.
- **AI is advisory:** suggestions and drafts are never applied or sent without an agent; only subject, description
  and public messages reach the assistant; `/api/assistant/internal/*` is never exposed publicly.
- **Caching is bounded and safe.** Every cache has a size limit and an expiry: in-memory reference snapshots in the
  backend services (TTL `REFERENCE_CACHE_TTL`, dropped *after commit*), HTTP `ETag`/`Cache-Control` for reference data, the browser `apiCache` (100 entries, memory only,
  cleared on writes and logout), the assistant answer cache (`ASSISTANT_ANSWER_CACHE_*`). Never cache per-user
  ticket data on shared layers, never write caches to the end user's disk (no localStorage/IndexedDB), and never
  cache personal data in nginx. See `docs/guide/caching.md`. Only exception: once a user turns push on, the
  Firebase SDK keeps its own registration in IndexedDB (required by FCM); `push-sw.js` caches nothing.
- **Secrets** (Firebase service-account key…) live only in `secrets/` (git-ignored, mounted read-only at
  `/run/secrets/maplenou`), referenced from `.env` by path. Never bake them into an image, a Compose file or the
  repository. Firebase *web* values (`FIREBASE_WEB_*`) are public and read at run time by `/api/push-config`.
- **Push notifications** go through `PushSender` (Firebase or log-only) and, for requesters, the Bakend relay to the
  Maplenou app; new notification types need FR and EN templates (migration) and keep `type`/`ticketId`/`source`/`url`
  in the push data. Setup and tests: `docs/guide/firebase.md`.
- **Branches:** work on `ci/<topic>`, push, read the GitHub Actions, merge to `main` only when green
  (`docs/guide/repository.md`, "Branches and checks"). Render deploys `main` (`render.yaml` in this folder, `docs/guide/render.md`).
- **Former support module (Bakend):** never handle a ticket in both places — `LEGACY_SYNC_ENABLED` here,
  `SUPPORT_LEGACY_READ_ONLY` in Bakend (DEC-CS12-02).
- **Public deployment** only through the `public` profile (Caddy) or the Render Blueprint (`PublicDeploymentGuard`): it refuses internal docs, an assistant that
  knows them, demo data and demo login. Host ports listen on `HOST_BIND_ADDRESS` (127.0.0.1 by default).
- **Design mockups** are in the Claude Design canvas linked from `docs/ux-ui/screens.md` (one page per language ×
  theme, desktop/tablet/mobile). A UI change updates the code first, then the matching canvas component.

## 5. Definition of done

- [ ] Tests added or updated, and green: backend, frontend (lint, typecheck, unit), assistant, e2e when flows change.
- [ ] FR and EN strings/pages both updated; `npm run typecheck` proves the translation keys match.
- [ ] `uv run python documentation/build.py --strict` passes (no broken links).
- [ ] Records updated: affected `docs/` pages, `docs/architecture/openapi.json` when the API changes,
      `DEVELOPMENT-MATRIX.md`, `PROJECT-CONTINUITY.md`, `CHANGELOG.md`.
- [ ] `scripts/smoke-test.sh` passes against `docker compose up -d --build --wait` when runtime behaviour changes.
- [ ] No hard-coded values in Compose files; new variables documented in `.env.example`.
- [ ] Conventional Commit message (`feat(scope): …`, `fix(scope): …`, `docs(scope): …`).

## 6. Reproduce the project identically

1. Clone; copy `.env.example` to `.env` (change the secrets for anything but a local demo).
2. `docker compose up -d --build --wait` → seeded demo (5 tickets, 7 personas).
   `docker compose --profile docs up -d --build --wait` starts the docs and the assistant (Ollama cloud with the
   key in `.env`; with a local Ollama and `ASSISTANT_PULL_MODELS=true` it pulls about 2.3 GB of models once).
3. `scripts/smoke-test.sh` and `scripts/test.sh all` must be green.
4. To rebuild from scratch in another system, follow `docs/architecture/reference-architecture.md` §4
   (adapting the ticketing core) and `docs/governance/REPRODUCTION.md`.

## 7. Map

```text
backend/        Spring Boot API (see backend/AGENTS.md)
frontend/       Next.js web app (see frontend/AGENTS.md, frontend/I18N.md)
assistant/      Ollama RAG documentation assistant (see assistant/AGENTS.md)
documentation/  Zensical sites: user, staff, developer(fr) + shared assets + build.py (see documentation/AGENTS.md)
docs/           English developer site content = project records + developer guide (docs/guide/)
zensical.toml   English developer site (replaces mkdocs.yml)
scripts/        test, check, smoke-test, doctor, env-sync, clean, backup (.sh for Linux/macOS, .ps1 for Windows)
render.yaml     Render Blueprint: API, web app, public help centre, assistant, PostgreSQL (Blueprint Path = ServiceClient/render.yaml)
integration/    patches for files OUTSIDE ServiceClient/ (Bakend supportbridge, Flutter push, root README): the owner applies them; never edit ../Bakend or ../frontend from here
deploy/         Caddyfile + guard of the public profile (HTTPS, domains)
monitoring/     Prometheus configuration, alert rules, Grafana provisioning (monitoring profile)
backend/http/   support-api.http (REST Client / IntelliJ) and postman/ (collection + environment) — manual API tests
secrets/        git-ignored keys (Firebase service account), mounted read-only into the backend
.env.example    every configurable value (copy to .env)
.github/        CI workflows, issue and PR templates, CODEOWNERS
```
