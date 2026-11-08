# Changelog

All notable changes to this project are documented in this file.

The format follows Keep a Changelog and versions follow Semantic Versioning.

## [Unreleased]

### Added / Changed — CS-12 (owner-requested, 2026-10-31)

- **Render (DEC-CS12-03):** `render.yaml` Blueprint (PostgreSQL, API, web app, public help centre, assistant);
  `DATABASE_URL` in `postgresql://` form; `PublicDeploymentGuard`; `BACKEND_HOSTPORT`; docs nginx as a template
  (`ASSISTANT_UPSTREAM`, `DOCS_RESOLVER`); guide `docs/guide/render.md` (FR/EN). Firebase: public web values of the
  `maplenou-c21c2` project in `.env.example`, Render Secret File for the service account.
- **Former support module (DEC-CS12-02):** scheduled import (`LEGACY_SYNC_ENABLED`, `LEGACY_SYNC_INTERVAL`), status
  changes noted once (`LEGACY_UPDATED`, migration V6 `legacy_ticket_sync`), Bakend `SUPPORT_LEGACY_READ_ONLY`;
  import response gains `updated`.
- **CI (DEC-CS12-01):** workflows run on `ci/**` pushes and by hand; `scripts/check.sh` / `check.ps1` (native or
  Docker, one line per suite).
- **Guides (DEC-CS12-04):** 32 annotated screenshots with numbered legends; `frontend/scripts/guide-screenshots.mjs`.
- **Mockups (DEC-CS12-05):** 13 screens in the design canvas (163 artboards).
- **Performance (DEC-CS12-06):** see `docs/architecture/complexity-review.md`.
- **i18n:** English category descriptions (V6 `description_en`, admin form); translated action names on « Droits ».

### Fixed — CS-12

- A complete old ticket number (`TKT-2026-000017`) was not found: same prefix as new numbers, exact search only on
  the new number. Docs said `SUP-…`; the real format is `TKT-YYYY-NNNNNN`.
- The docs nginx refused to start: a regular expression with `{8}` must be quoted.
- `Bakend/mvnw` was not executable in Git: the Bakend workflow failed with "Permission denied".
- The « Statuts et priorités » transitions matrix showed French labels in English.
- `Site_vitrine/package-lock.json` out of sync: `npm ci` (and so the site workflow) failed.


### Added / Changed — CS-11 (owner-requested, 2026-10-30/31)

- **Hexagonal modules (DEC-CS11-01):** every backend module split into `domain` (plain Java, the `Ticket`
  aggregate restored from a snapshot), `application` (use cases + ports) and `adapter.in/out`; `shared` kernel and
  `platform` base; `ArchitectureTest` (ArchUnit 1.4.1) checks layers, private adapters and the absence of cycles.
  Transactional outbox `outbox_events` with `OutboxRelay` (after commit, retries, gauges).
- **Configurable vocabulary (DEC-CS11-02):** statuses with a kind, transitions, priorities (rank, requester
  selectable, default), SLA policies, permission matrix as data, generic `ticket_context_refs` (migration V4);
  screens « Statuts et priorités », « Droits », SLA targets in « Calendrier SLA »; `GET /vocabulary`; the web app
  takes labels, filters and badges from it.
- **Rules (DEC-CS11-03 … 05):** resolution clock paused while waiting for the requester (state PAUSED, due date
  moved); resolved tickets closed after `SUPPORT_AUTO_CLOSE_AFTER` (7 days); satisfaction rating 1–5 with dashboard
  and report score.
- **Back-office tools (DEC-CS11-06):** categories added by administrators (BR-006 amended, English names), canned
  replies, CSV export of the filtered queue, help-centre search on the help home.
- **Actions on Maplenou (DEC-CS11-07):** return request, delivery release, message to the courier, through Bakend's
  new `SupportBridgeActionService` endpoints (audited `SUPPORT_*`); outcome recorded on the ticket. Bakend's order
  summary lists its sub-orders.
- **AI help (DEC-CS11-08):** category/priority suggestion and reply drafts from the assistant
  (`/api/assistant/internal/*`, `ASSISTANT_INTERNAL_TOKEN`, never public), always decided by an agent; public
  assistant through the `public` profile; `GET /api/assistant/search`.
- **Languages (DEC-CS11-09):** notification templates per language (V5), each person's language
  (`PUT /me/preferences`), push data `source`/`url`; requesters' pushes relayed to the Maplenou app by Bakend
  (`MAPLENOU_PUSH_RELAY`); the Flutter app opens the ticket.
- **Production (DEC-CS11-10):** rate limits (429), `X-Request-Id`, JSON logs (`LOG_FORMAT=ecs`), Prometheus metrics
  (`METRICS_TOKEN`), profiles `antivirus` (ClamAV), `monitoring` (Prometheus + alert rules + Grafana) and `public`
  (Caddy, HTTPS on `SUPPORT_DOMAIN`, `HELP_DOMAIN`, `SITE_DOMAIN`, guard against internal docs and demo data);
  Cloudinary authenticated storage; ports on `HOST_BIND_ADDRESS`; `scripts/env-sync.*`; doctor checks for settings
  that go together; new guide `deployment.md` (FR/EN).
- **Fixed:** unknown status/priority filter → 400; rate limiter no longer trusts a client `X-Forwarded-For`;
  `SESSION_COOKIE_SECURE` passed by Compose; product conditions aligned with Bakend.
- **Removed:** `docs/governance/REFERENCES-CS-02.md` (merged into `REFERENCES.md`); the Spring Cache manager
  (reference data are snapshots dropped after commit).

### Added / Changed — CS-10 iteration 5 (owner-requested, 2026-10-29)

- **SLA clock 24/7 (DEC-CS10-16):** new setting `SLA_MODE` (`maplenou.sla.mode`). `always` (default) counts
  deadlines 24 hours a day, 7 days a week, weekends and public holidays included, one "day" = 24 hours
  (`BusinessCalendar.continuous`); `business-hours` keeps the former Monday–Friday 08:00–18:00 rule with the
  holidays of « Calendrier SLA ». Targets and states unchanged. New `GET /api/v1/support/admin/sla-settings`; the
  « Calendrier SLA » screen says that holidays have no effect in 24/7 mode. Tests `ContinuousCalendarTest`,
  `it/SlaModeIT`. BR-032, DEC-001 and DEC-CS03-05 carry an amendment note.
- **Integration into the Maplenou repository as `ServiceClient/` (DEC-CS10-17):** sign-in with a Maplenou account
  (phone number + password, 6-digit code with 2FA) checked by Bakend — `POST /api/v1/auth/maplenou/login` and
  `/mfa`, `MAPLENOU_LOGIN_ENABLED`, `AUTH_MODE=maplenou`, `/api/auth-config`; the service issues its own 8-hour
  token and closes the core session; 10 attempts per phone number and minute. Roles mapped from the Maplenou
  account; support roles given by phone number on the « Agents » page (« Rôles support », `admin/staff-roles`,
  table `support_staff_assignments`). Real orders and deliveries from Bakend's read-only internal API
  `/internal/v1/**` (`ECOSYSTEM_MODE=http`, header `X-Maplenou-Service-Token` = `SUPPORT_SERVICE_TOKEN`,
  `MAPLENOU_CORE_URL`). One-time, idempotent import of the former core tickets (« Importer les anciens tickets »,
  `POST /admin/legacy-import`); the old number (`TKT-YYYY-NNNNNN`, 6 digits) is shown next to the new one (`legacyNumber`) and found by
  the search. Migration `V3__maplenou_integration.sql`. Tests `it/MaplenouIntegrationIT` with `it/FakeMaplenouCore`;
  `scripts/doctor.*` section "Maplenou platform". New guide `docs/guide/maplenou-integration.md` (FR/EN).
- **Ollama cloud by default (DEC-CS10-18):** `OLLAMA_URL=https://ollama.com`, secret `OLLAMA_API_KEY` (server
  side only, `Authorization: Bearer`), `ASSISTANT_CHAT_MODEL=gpt-oss:20b`, `ASSISTANT_EMBED_MODEL=` (BM25 only),
  new `ASSISTANT_THINK=low`, `ASSISTANT_PULL_MODELS=false`; no model pull in cloud mode; health reports
  `ollamaCloud`. A local Ollama (computer or `--profile ollama`) stays possible. Tests `assistant/tests/test_cloud.py`;
  the doctor scripts check the key and the model on ollama.com.
- **Ports:** web app on **3001** and API on **8081** (`FRONTEND_HOST_PORT`, `BACKEND_HOST_PORT`; native backend
  `server.port: ${SERVER_PORT:8081}`, `npm run dev`/`start` on 3001), because the Maplenou admin back-office uses 3000
  and Bakend 8080; inside Docker the backend still listens on 8080. Documentation stays on 8006.
- **Public marketplace website** `Site_vitrine/` (separate Next.js app, port 3002) recorded as DEC-CS10-19.
- **Security:** Next.js 16.4.0 → **16.4.3** in `frontend/` (and in `Site_vitrine/`): fixes the critical advisories
  GHSA-2464-fhr4-w9xw (image optimisation with SVG), GHSA-p3m5-59mp-jv5r (server actions DoS) and
  GHSA-673q-mjm6-r5xr (`use cache`). `npm audit` still lists `@grpc/grpc-js` through `firebase` (Node-side
  Firestore, not used by the browser push code); its fix is a breaking `firebase` change, kept as a proposal.
- Documentation: user guide (sign-in with the Maplenou account, deadlines every day), staff guide (« Rôles support »,
  « Anciens tickets Maplenou », SLA 24/7, reports), developer guide (configuration tables, assistant modes,
  integration page) and records (decisions, API specification, database model, continuity).

### Added — CS-10 iteration 4 (owner-requested, 2026-10-14)

- **Firebase push notifications connected end to end** (roadmap *Semaine 4*, ticket spec §15): service-account
  key mounted read-only from `secrets/` (`FIREBASE_CREDENTIALS_FILE`), clear start-up error when the file is
  missing; `GET /notifications/push-status` and `POST /notifications/devices/test`; browser push in the web app
  ("Notifications push / Push notifications" card on the Notifications screens, `push-sw.js`, run-time
  `/api/push-config` from `FIREBASE_WEB_*`), click opens the ticket, logout unsubscribes the browser; setup guide
  `docs/guide/firebase.md` (FR/EN) with the Flutter integration.
- **API testing:** Postman collection and environment (`backend/http/postman/`, 28 requests, 36 checks, run with
  newman), guide `docs/guide/api-testing.md` (Swagger UI, Postman, REST Client); `.http` file extended.
- **`scripts/doctor.ps1` / `doctor.sh`:** read-only diagnosis of Docker, `.env`, host ports, Ollama and containers.

### Changed — CS-10 iteration 4

- Documentation served on port **8006** (`DOCS_HOST_PORT`, `DOCS_PUBLIC_URL`); `zensical serve` examples on 8007/8008.
- **Ollama selectable in `.env`** (`OLLAMA_URL`): the Ollama installed on the computer (default, no image) or the
  container, now in its own `ollama` profile; the assistant no longer depends on the container, waits for Ollama
  as long as needed and pulls missing models when it appears (`ASSISTANT_PULL_MODELS`).
- Smoke tests print the push channel; OpenAPI description regenerated.

### Changed — CS-10 iteration 3 (owner-requested, 2026-10-09)

- **Mockups in Claude Design** (replaces the Figma file): one canvas with an overview page and one page per
  language × theme (English · Light, English · Dark, Français · Clair, Français · Sombre); each page shows the help
  home, new ticket, support dashboard and ticket workspace at desktop, tablet and mobile sizes (48 screens built
  from four components). Link in `docs/ux-ui/screens.md` and on `/design`.
- **Smaller Docker images and less disk:** multi-stage Dockerfiles with BuildKit cache mounts, layered Spring Boot
  jar on `eclipse-temurin:21-jre-alpine`, Next.js standalone on `node:22-alpine`, assistant on
  `python:3.12-slim-bookworm` with `uv sync --locked --no-dev`, docs pre-gzipped on `nginx:alpine`; non-root users,
  health checks, `.dockerignore` everywhere, log rotation; the separate `ollama-models` container and the docs
  `assistant` build target were removed (the assistant pulls its models through the Ollama API).
- **`.env` only:** every value of `docker-compose.yml`, `docker-compose.test.yml` and the override example comes
  from `.env` (`.env.example` documents each one); Compose fails fast when a value is missing.
- **Python with uv:** `assistant/pyproject.toml` + `uv.lock` replace `requirements*.txt`; the root project only
  locks Zensical.
- **Dark-theme contrast:** `--text-on-brand/accent/danger` tokens replace hard-coded white text on coloured fills.

### Added — CS-10 iteration 3

- **Bounded caching:** Caffeine cache for active categories (evicted after commit), `ETag`/`Cache-Control` and 304
  on `GET /api/v1/categories`, JSON gzip; browser stale-while-revalidate cache (100 entries, memory only, cleared on
  writes and logout); assistant answer cache (LRU + TTL); nginx cache rules. Guide: `docs/guide/caching.md`.
- `scripts/smoke-test`, `scripts/clean`, `scripts/backup` (`.sh` and `.ps1`); `backend/http/support-api.http`.
- Developer guide pages "Backend processing and manual tests" and "Caching and disk space" (FR/EN); test command
  list with and without Docker in the testing guide.
- Repository files: `.editorconfig`, `SECURITY.md`, `LICENSE` (to be confirmed), issue and PR templates,
  `CODEOWNERS`.
- Tests: `CachingIT` (ETag/304, eviction after an admin change), `apiCache.test.ts`, assistant cache and URL-index
  tests.

### Added — CS-10 (owner-requested, 2026-10-09)

- Web app in **French and English** (switch in the header, the account page and the login page; cookie
  `maplenou_locale`) and **light / dark / system themes** (cookie `maplenou_theme`); errors translated by API code;
  notifications rendered in English from language-neutral facts (Flyway **V2** adds `ticket_number`, `detail`).
- **Three documentation sites** (user — public, staff — internal, developer — internal) in French and English, built
  with **Zensical** (`documentation/`, root `zensical.toml`), with language switch keeping the page, palette toggle,
  search and a portal page; strict build in CI; GitHub Pages publishes the user guide only.
- **Documentation assistant** (`assistant/`): Ollama (`qwen2.5:3b` + `nomic-embed-text`) answering from the sites'
  search indexes with numbered **references**, streaming, retrieval-only fallback, per-instance site scope, rate
  limit; widget "Poser une question / Ask a question" on every site; `docs` profile in Docker Compose.
- **Design deliverables:** Figma file (superseded in iteration 3; variables, text styles, components, composed screens), in-app design
  catalogue `/design`, screenshots as built (`docs/ux-ui/screens.md`).
- **Reference architecture** reusable for other ticketing systems (`docs/architecture/reference-architecture.md`).
- **AGENTS.md / CLAUDE.md** (root and per folder) and a developer guide (`docs/guide/`, FR translation).
- **Docker-only workflow:** `docker-compose.test.yml`, `scripts/test.sh` / `test.ps1`, `.env.example`,
  `docker-compose.override.example.yml`; CI workflow for the assistant.

### Changed

- PostgreSQL is no longer published on host port 5432 (fixes "port is already allocated"); host ports configurable.
- MkDocs replaced by Zensical (`mkdocs.yml` → `zensical.toml`); `pyproject.toml` pins `zensical==0.0.69`.
- Actuator exposes `health` only.

## [0.1.0] - 2026-10-08 — MVP candidate (awaiting owner approval)

### Added

- **CS-03** Architecture decisions, PostgreSQL schema (Flyway V1), API specification and generated OpenAPI contract,
  FR-014 search/filter specification (AC-026 … AC-029), Spring Boot 4.1 backend project with Maven wrapper and Dockerfile.
- **CS-05** Ticket backend: creation with ticket numbers, categories, priorities, lifecycle, assignment, messages,
  internal notes, attachments (DEC-003), append-only history, SLA on a business calendar, reopening, escalation,
  search and filters, support dashboard and basic report, administration of agents, categories and holidays.
- **CS-04 / CS-06** Next.js web app: requester screens A1–A4, support screens B1–B4, notifications, account,
  access denied, administration; connected to the API through a same-origin proxy with an httpOnly session.
- **CS-07** Ecosystem context (order, payment/commission/payout, delivery/driver, seller, refund) through a
  read-only gateway with a demo adapter and a proposed HTTP contract; reference ownership checks (BR-019).
- **CS-08** In-app notifications and Firebase Cloud Messaging push with retries, isolated from ticket state;
  roadmap scenarios as automated tests.
- **CS-09** Security review, Docker Compose stack, CI workflows (backend, frontend, end-to-end), Playwright
  validation of the complete scenario on desktop and mobile.
- Validation reports CS-03 … CS-09 and updated UML diagrams.

### Changed

- Ticket state diagram now matches the approved lifecycle (`WAITING_FOR_CUSTOMER`, reopening request/decision).
- Traceability matrix links every requirement to its implementation and tests.
- MkDocs navigation covers all documentation and renders Mermaid diagrams; `pyproject.toml` depends on `mkdocs-material`.

### Security

- Deny-by-default API, resource-level authorization, append-only audit trail enforced by the database,
  content-checked attachments served only through authorized downloads, CSRF protection on the web proxy.
