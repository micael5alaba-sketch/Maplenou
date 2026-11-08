# Testing

Every suite runs in containers, so Java, Node.js and Python are not required. The same suites run natively when the
toolchains are installed, and in GitHub Actions.

## Test pyramid

| Level | Tool | Location | What it checks |
| --- | --- | --- | --- |
| Backend unit (96) | JUnit 5 (Surefire, `*Test`), ArchUnit | `backend/src/test/java/.../` | `ArchitectureTest` (hexagonal layers and module boundaries), workflow and permission matrix rules (`WorkflowTest`, `TicketAccessPolicyTest`), business and 24/7 calendars, SLA policies, pause and states, attachment rules, CSV safety (`CsvWriterTest`), adapters against local fakes: `CloudinaryStorageTest`, `ClamAvScannerTest`, `BakendContractTest` |
| Backend integration (87) | Spring Boot test + MockMvc (Failsafe, `*IT`) | `backend/src/test/java/.../it/` | HTTP → security with real JWTs → services → PostgreSQL 16 with the Flyway schema; `CachingIT`: category ETag/`304` and cache cleared after an administrator's change; `SlaModeIT`: 24/7 deadlines and `admin/sla-settings`; `MaplenouIntegrationIT`: Maplenou sign-in (with 2FA), support roles by phone, legacy import, against the fake core `FakeMaplenouCore`; `SupportFeaturesIT`: SLA pause, automatic closure, satisfaction, canned replies, CSV export, configurable vocabulary, actions on Maplenou, outbox, templates per language; `OperationsIT`: AI suggestion and draft (fake assistant), 429 rate limit, metrics token, request id |
| Frontend unit/component (42) | Vitest 4 + jsdom + Testing Library | `frontend/src/**/*.test.{ts,tsx}` | API client and its cache (`apiCache.test.ts`), attachments, labels, i18n helpers and key parity, configurable vocabulary (`vocabulary.test.ts`), components (satisfaction, custom statuses) |
| Frontend static checks | ESLint, `tsc --noEmit`, `next build` | `frontend/` | lint rules, types (including FR/EN key parity), production build |
| Assistant (33) | pytest | `assistant/tests/` | retrieval, citations, fallbacks, streaming, scope, validation, embedding and answer caches, rate limit, Ollama cloud (`test_cloud.py`), help search and AI help for agents (`test_support.py`: token, lists, untrusted text, user guide only) |
| End-to-end | Playwright 1.56 | `frontend/e2e/scenario.spec.ts` | the complete ticket journey in a real browser, desktop and mobile; canned replies, CSV export and the administration screens |
| Documentation | `build.py --strict` (also run by the docs image build) | `documentation/`, `docs/` | every page builds, no missing page or anchor |
| Running stack | `scripts/smoke-test.*` | `scripts/` | a started stack answers end to end (see below) |
| API scenario | Postman collection / newman (40 requests, 59 checks) | `backend/http/postman/` | the whole ticket journey through the REST API, with checks — see [Test the API](api-testing.md) |

The backend integration tests extend `it.IntegrationTest`, which issues real tokens with `AccessTokenIssuer`, creates its
own users for each test (tests never share data) and disables the push dispatcher. It uses `TEST_DATABASE_URL`
(with `TEST_DATABASE_USERNAME`, `TEST_DATABASE_PASSWORD`, default `maplenou`) when set, otherwise starts a
Testcontainers `postgres:16-alpine`.

## Commands to verify the project

There are two ways to verify the project: **with Docker** (nothing else to install) or **without Docker** (with the
native toolchains). Both run the same tests. Commands are given for Windows PowerShell and for Bash (macOS, Linux,
Git Bash); run them from the repository root.

### With Docker

| Goal | PowerShell (Windows) | Bash (macOS, Linux) |
| --- | --- | --- |
| Create the configuration (once; the test scripts do it when `.env` is missing) | `copy .env.example .env` | `cp .env.example .env` |
| Every suite: backend, frontend, assistant, docs, e2e | `.\scripts\test.ps1 all` | `./scripts/test.sh all` |
| Backend: 102 unit + 87 integration tests, PostgreSQL in memory | `.\scripts\test.ps1 backend` | `./scripts/test.sh backend` |
| Frontend: lint, types, 45 unit tests, production build | `.\scripts\test.ps1 frontend` | `./scripts/test.sh frontend` |
| Assistant: 35 tests, fake Ollama | `.\scripts\test.ps1 assistant` | `./scripts/test.sh assistant` |
| Documentation: strict build of the six sites (docs image) | `.\scripts\test.ps1 docs` | `./scripts/test.sh docs` |
| End to end: starts the stack, then Playwright | `.\scripts\test.ps1 e2e` | `./scripts/test.sh e2e` |
| Start the application and wait until it is healthy | `docker compose up -d --build --wait` | `docker compose up -d --build --wait` |
| … with the documentation and the assistant (Ollama cloud by default) | `docker compose --profile docs up -d --build --wait` | `docker compose --profile docs up -d --build --wait` |
| … same, with the Ollama container (`OLLAMA_URL=http://ollama:11434`) | `docker compose --profile docs --profile ollama up -d --build --wait` | `docker compose --profile docs --profile ollama up -d --build --wait` |
| Diagnose a stack that does not start (read-only) | `.\scripts\doctor.ps1` | `./scripts/doctor.sh` |
| Smoke test of the running stack | `.\scripts\smoke-test.ps1` | `./scripts/smoke-test.sh` |
| API scenario with checks (Postman collection, needs Node.js for `npx`) | `npx newman run backend/http/postman/Maplenou-Support-API.postman_collection.json -e backend/http/postman/Maplenou-local.postman_environment.json` | same command |
| Try one endpoint in the browser (Swagger UI) | <http://localhost:8081/swagger-ui.html> | <http://localhost:8081/swagger-ui.html> |
| State and health of the containers | `docker compose ps` | `docker compose ps` |
| Logs of a service | `docker compose logs -f backend` | `docker compose logs -f backend` |
| Disk used by Docker | `docker system df` | `docker system df` |
| Free disk space | `.\scripts\clean.ps1` | `./scripts/clean.sh` |

`scripts/test.sh` (macOS, Linux) and `scripts/test.ps1` (Windows) take one argument,
`backend|frontend|assistant|docs|e2e|all` (default `all`), stop at the first failing suite, and finally remove the
test containers. The cache volumes (`maven-cache`, `npm-cache`, `frontend-node-modules`, `e2e-node-modules`) are
kept, so later runs do not download the dependencies again. The services are defined in `docker-compose.test.yml`,
which reads its values from `.env` (`TEST_POSTGRES_*`, log rotation) and reuses the base images of the application:

| Suite | Compose service | Image | Command |
| --- | --- | --- | --- |
| `backend` | `backend-tests` + `test-db` | `eclipse-temurin:21-jdk-noble`, `postgres:16-alpine` (database in `tmpfs`) | `./mvnw -B verify` with `TEST_DATABASE_URL=jdbc:postgresql://test-db:5432/<TEST_POSTGRES_DB>` |
| `frontend` | `frontend-tests` | `node:22-alpine` | `npm ci --legacy-peer-deps` → `lint` → `typecheck` → `test` → `build` |
| `assistant` | `assistant-tests` | `./assistant`, build target `test` | `python -m pytest -q` with the tests' own small indexes |
| `docs` | `docs` of `docker-compose.yml` (`docs` profile) | `documentation/Dockerfile` | `docker compose --profile docs build docs`: `build.py --strict` runs inside the image build |
| `e2e` | `e2e` | `mcr.microsoft.com/playwright:v1.56.0-noble` | `npx playwright test` against `http://frontend:3000` |

The scripts are thin wrappers; you can call Compose directly:

```bash
docker compose -f docker-compose.test.yml run --rm backend-tests
docker compose -f docker-compose.test.yml run --rm frontend-tests
docker compose -f docker-compose.test.yml run --rm assistant-tests
docker compose --profile docs build docs
docker compose -f docker-compose.test.yml down -v      # also deletes the cache volumes

# end-to-end: start the full stack first, then run Playwright in the same Compose project
docker compose up -d --build --wait
docker compose -f docker-compose.yml -f docker-compose.test.yml run --rm e2e
```

The e2e suite leaves the application stack running; stop it with `docker compose down` (add `-v` to reset the demo
data).

### Without Docker

Install the toolchains first (see [Getting started](getting-started.md)): Java 21, Node.js 22, Python 3.12 and uv.

| Goal | PowerShell (Windows) | Bash (macOS, Linux) |
| --- | --- | --- |
| Backend, database from Testcontainers (needs Docker) | `cd backend; .\mvnw.cmd verify` | `cd backend && ./mvnw verify` |
| Backend against a local PostgreSQL (no Docker) | `cd backend; $env:TEST_DATABASE_URL="jdbc:postgresql://localhost:5432/maplenou_test"; .\mvnw.cmd verify` | `cd backend && TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/maplenou_test ./mvnw verify` |
| One backend test class | `.\mvnw.cmd "-Dtest=BusinessCalendarTest" test` | `./mvnw -Dtest=BusinessCalendarTest test` |
| One integration test class | `.\mvnw.cmd "-Dit.test=CachingIT" verify` | `./mvnw -Dit.test=CachingIT verify` |
| Frontend: lint, types, unit tests, build | `cd frontend; npm ci --legacy-peer-deps; npm run lint; npm run typecheck; npm test; npm run build` | `cd frontend && npm ci --legacy-peer-deps && npm run lint && npm run typecheck && npm test && npm run build` |
| Assistant | `cd assistant; uv sync; uv run pytest` | `cd assistant && uv sync && uv run pytest` |
| Documentation, every site (strict) | `uv sync; uv run python documentation/build.py --strict` | `uv sync && uv run python documentation/build.py --strict` |
| Documentation, one site with Zensical | `uv run python documentation/build.py --prepare-only; uv run zensical build --strict -f zensical.toml` | `uv run python documentation/build.py --prepare-only && uv run zensical build --strict -f zensical.toml` |
| End to end (application running on port 3001) | `cd frontend; npx playwright install chromium; npx playwright test` | `cd frontend && npx playwright install chromium && npx playwright test` |
| Smoke test against a native stack | `.\scripts\smoke-test.ps1 -Api http://localhost:8081 -App http://localhost:3001` | `./scripts/smoke-test.sh http://localhost:8081 http://localhost:3001` |

- The test database for `TEST_DATABASE_URL` must exist (`maplenou_test`); user and password come from
  `TEST_DATABASE_USERNAME` and `TEST_DATABASE_PASSWORD` (default `maplenou`).
- In Windows PowerShell 5.1, `;` runs the next command even when the previous one failed: check each result
  (PowerShell 7 also accepts `&&`).
- Playwright reads `E2E_BASE_URL` (default `http://localhost:3001`); its HTML report is written to
  `frontend/playwright-report/`.
- The smoke test needs a backend with the `demo` profile (demo login); its third argument (`-Docs`) is the
  documentation URL, checked only when it answers.

## Check with or without Docker

`scripts/check.sh` (Windows: `scripts\check.ps1`) runs **the same commands as the GitHub Actions** and chooses, for
each suite, the native toolchain when it is installed or Docker otherwise. Unlike `scripts/test.sh` it does not stop
at the first failure: it ends with one line per suite (`PASS`, `FAIL` or `SKIP` with the reason) — the quickest way
to know what the Actions will answer before you push.

| Goal | PowerShell (Windows) | Bash (macOS, Linux) |
| --- | --- | --- |
| ServiceClient suites, each one natively if possible, otherwise in Docker | `.\scripts\check.ps1` | `./scripts/check.sh` |
| Force Docker (nothing installed but Docker) | `.\scripts\check.ps1 -Mode docker` | `./scripts/check.sh --docker` |
| Force the native toolchains (no Docker) | `.\scripts\check.ps1 -Mode native` | `./scripts/check.sh --native` |
| Everything a pull request to `main` checks (adds `Site_vitrine`, Bakend, Flutter) | `.\scripts\check.ps1 ci` | `./scripts/check.sh ci` |
| Only some suites | `.\scripts\check.ps1 -Mode native backend docs` | `./scripts/check.sh --native backend docs` |

| Suite | Native (what the workflow runs) | Docker |
| --- | --- | --- |
| `backend` | `./mvnw -B verify` (Testcontainers, or `TEST_DATABASE_URL`) | `backend-tests` + `test-db` |
| `frontend` | `npm ci --legacy-peer-deps`, `lint`, `typecheck`, `test`, `build` | `frontend-tests` |
| `assistant` | `uv sync --locked`, `uv run pytest -q` | `assistant-tests` |
| `docs` | `uv sync --locked`, `documentation/build.py --strict` | docs image build |
| `e2e` | `npx playwright test` against `E2E_BASE_URL` (app already running) | starts the stack, then the `e2e` service |
| `site` | `../Site_vitrine`: `npm ci`, `lint`, `typecheck`, `test`, `build` | `node:22-alpine`, `node_modules` in a named volume |
| `bakend` | `../Bakend`: `./mvnw test` (its PostgreSQL and Redis running) | `SKIP` — use native or `Bakend/start-all.ps1` |
| `flutter` | `../frontend`: `flutter pub get`, `flutter analyze`, `flutter test` | `SKIP` — install Flutter |

## Smoke test of a running stack

`scripts/smoke-test.sh` (curl) and `scripts/smoke-test.ps1` (PowerShell only) check a stack that is already running,
started with Docker or natively, and stop at the first failure:

1. backend `/actuator/health` is `UP` and the demo login returns tokens for Jean K. and Sophie N.;
2. `GET /categories` returns an `ETag`, and the same request with `If-None-Match` returns `304`;
3. Jean creates a ticket, Sophie takes it, replies and resolves it;
4. a customer gets `403` on the support dashboard;
5. `GET /notifications/push-status` prints the push channel of the backend: `firebase` (the key of
   `FIREBASE_CREDENTIALS_FILE` is loaded) or `log-only` — see [Firebase](firebase.md);
6. the web app serves `/login`, in English with the cookie `maplenou_locale=en`;
7. when the documentation answers on its URL: the `search.json` of the six sites and `/api/assistant/health`.

The test creates a real ticket ("Smoke test") in the database. Arguments: API, web app and documentation URLs
(defaults `http://localhost:8081`, `http://localhost:3001`, `http://localhost:8006`).

## Manual API tests

`backend/http/support-api.http` is a request collection for the VS Code REST Client and the JetBrains HTTP Client:
health, demo tokens, then a full ticket journey, sent from top to bottom. See
[How the backend processes a request](backend-processing.md).

The same journey exists as a Postman collection with automatic checks (`backend/http/postman/`, also runnable with
`newman`), and Swagger UI (<http://localhost:8081/swagger-ui.html>) lets you try any endpoint with a demo token. See
[Test the API with Swagger UI and Postman](api-testing.md).

## What the end-to-end scenario covers

`frontend/e2e/scenario.spec.ts` drives the real UI with the demo personas (French locale), against the backend
`demo` profile:

1. **Complete lifecycle.** Jean K. (customer) creates a ticket in the delivery category, linked to order
   `CMD-2026-0456`, with high priority and a PNG attachment, and gets a number `TKT-YYYY-NNNNN…`. Sophie N. (agent)
   finds it in the queue by number, sees the order and delivery context, takes it, adds an internal note, replies
   and asks for information. Jean sees "Action requise", never the internal note, and replies (the ticket returns
   to "En cours"). Sophie resolves it after a confirmation dialog. Jean requests a reopening; Sophie approves it,
   resolves again and closes the ticket. Jean sees the closed state, no reopening button, and the in-app
   notifications; Sophie sees the full audited history.
2. **Access denied.** Marie K. (seller) opening `/support` is redirected to the access-denied page.
3. **Read-only colleague ticket.** Sophie can open a ticket assigned to another agent but sees "Lecture seule" and no
   send button.
4. **Mobile** (`@mobile`, Pixel 7 profile). Kossi D. (driver) reports a delivery incident from a phone.
5. **Language and theme.** Switching to English and dark mode changes `<html lang>` and `data-theme`, survives a
   reload (cookies), and switching back to French works.

The `desktop` project (Desktop Chrome) runs every test except the `@mobile` one, which is skipped there; the `mobile`
project runs only `@mobile`.

## Strict documentation build

Treat documentation like code: build it in strict mode before every commit that touches `docs/` or
`documentation/`. With Docker, `scripts/test.sh docs` does the same through the docs image.

```bash
python3 documentation/build.py --sites developer --strict   # must end with "Built 2 site(s)"
python3 documentation/build.py --strict                     # all six sites
```

Strict mode stops on any warning, for example a link to a page that does not exist or to a missing `#anchor`.

## Continuous integration

| Workflow | Trigger | Runs |
| --- | --- | --- |
| `.github/workflows/service-client-backend.yml` | changes in `backend/` | `./mvnw -B verify` (Testcontainers), uploads Surefire/Failsafe reports |
| `.github/workflows/service-client-frontend.yml` | changes in `frontend/` | `npm ci`, lint, typecheck, test, build |
| `.github/workflows/service-client-e2e.yml` | every push to `main` and pull request | PostgreSQL service, backend jar with `demo`, `next start`, Playwright; uploads report and logs |
| `.github/workflows/service-client-assistant.yml` | changes in `assistant/` | Python 3.12, installs the dependencies, `python -m pytest -q` |
| `.github/workflows/service-client-docs.yml` | push and pull request on `main` | `python documentation/build.py --strict` (six sites); on `main`, builds the **user guide only** and deploys it to GitHub Pages |
