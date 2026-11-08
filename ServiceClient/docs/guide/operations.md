# Operations

This page covers configuration, secrets, health checks, logs, backups, disk space and publishing. The repository ships a local
Docker Compose stack for demos and development; the production items still open are listed in the
[security review](../architecture/security.md) ("Before production").

## Configuration reference

### Docker stack (.env)

`docker-compose.yml` and `docker-compose.test.yml` take every value from `.env` (template and documentation:
`.env.example`; create it with `cp .env.example .env`, Windows `copy .env.example .env`). A required variable that is
missing stops Compose with `Set <NAME> in .env`. Nothing is hard-coded in the Compose files.

| Variable | `.env.example` | Used by | Purpose |
| --- | --- | --- | --- |
| `FRONTEND_HOST_PORT` | `3001` | `frontend` | host port of the web app (3000 is the Maplenou admin back-office) |
| `BACKEND_HOST_PORT` | `8081` | `backend` | host port of the API (8080 is Bakend, the Maplenou core API); inside the container the backend listens on 8080 |
| `DOCS_HOST_PORT` | `8006` | `docs` | host port of the documentation (`docs` profile) |
| `POSTGRES_HOST_PORT` | `5433` | `docker-compose.override.yml` | host port of PostgreSQL, only when the override is used |
| `POSTGRES_DB` | `maplenou_support` | `postgres`, `backend` | database name (also part of `DATABASE_URL`) |
| `POSTGRES_USER` | `maplenou` | `postgres`, `backend` (`DATABASE_USERNAME`), backup scripts | database user |
| `POSTGRES_PASSWORD` | `maplenou` | `postgres`, `backend` (`DATABASE_PASSWORD`) | database password — change it outside local demos |
| `SPRING_PROFILES_ACTIVE` | `demo` | `backend` | `demo` = demo login, sample data, seeded tickets; never in production |
| `JWT_SECRET` | `demo-only-secret-change-me-0123456789abcdef` | `backend` | HS256 secret of the support service's own tokens (demo and Maplenou sign-in), at least 32 characters; a random value outside local demos |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3001` | `backend` | browser origins allowed to call the API directly |
| `SLA_MODE` | `always` | `backend` | SLA clock: `always` = 24/7, weekends and public holidays count (DEC-CS10-16); `business-hours` = Monday–Friday 08:00–18:00 `Africa/Lome` minus the holidays of « Calendrier SLA » |
| `MAPLENOU_CORE_URL` | `http://host.docker.internal:8080` | `backend` | Bakend (Maplenou core API) as seen from the container — sign-in, real orders, import of the former tickets; see [Maplenou platform integration](maplenou-integration.md) |
| `MAPLENOU_LOGIN_ENABLED` | `false` | `backend` | `true` = sign-in with a Maplenou account (phone number + password, checked by Bakend); set `AUTH_MODE=maplenou` too |
| `AUTH_MODE` | `demo` | `frontend` | sign-in page of the web app: `demo` (personas, backend `demo` profile) or `maplenou` (Maplenou account) |
| `SESSION_COOKIE_SECURE` | `false` | `frontend` | `true` behind HTTPS (required by the `public` profile): the session cookie is sent over HTTPS only |
| `ECOSYSTEM_MODE` | `demo` | `backend` | `demo` (sample orders) or `http` (real orders and deliveries read from Bakend `/internal/v1/**`, needs `SUPPORT_SERVICE_TOKEN`) |
| `SUPPORT_SERVICE_TOKEN` | empty | `backend` | **secret** shared with Bakend (same value in Bakend's environment), 32+ random characters; needed by `ECOSYSTEM_MODE=http` and by the import of the former tickets; empty = not connected (may be empty) |
| `FIREBASE_CREDENTIALS_FILE` | empty | `backend` | Firebase service-account key in the container: `/run/secrets/maplenou/firebase-service-account.json` (file saved in `secrets/`); empty = push is logged only (optional variable) — see [Firebase](firebase.md) |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_PROJECT_ID`, `FIREBASE_WEB_MESSAGING_SENDER_ID`, `FIREBASE_WEB_APP_ID`, `FIREBASE_WEB_VAPID_KEY` | empty | `frontend` | public values of the Firebase web app, read at run time by `/api/push-config`; all empty = no browser push (optional variables) |
| `HOST_BIND_ADDRESS` | `127.0.0.1` | every published port | address the host ports listen on: `127.0.0.1` = this computer only (recommended, also on a server behind Caddy); `0.0.0.0` = every network |
| `MAPLENOU_PUSH_RELAY` | `false` | `backend` | `true` = requesters' notifications are also pushed to the Maplenou mobile app through Bakend (`POST /internal/v1/users/{id}/push`); needs `SUPPORT_SERVICE_TOKEN` |
| `SUPPORT_APP_URL` | `http://localhost:3001` | `backend` | address of this web app as users see it; pushes link to `<SUPPORT_APP_URL>/?ticket=<id>` (public deployment: `https://<SUPPORT_DOMAIN>`) |
| `SUPPORT_AUTO_CLOSE_AFTER` | `P7D` | `backend` | a resolved ticket closes by itself after this ISO-8601 duration if it is not reopened (DEC-CS11-04); `P0D` = never |
| `LEGACY_SYNC_ENABLED`, `LEGACY_SYNC_INTERVAL` | `false`, `PT15M` | `backend` | import the new tickets of Bakend's former support module and note its status changes periodically (DEC-CS12-02, [Maplenou integration](maplenou-integration.md)) |
| `ATTACHMENT_STORAGE` | `local` | `backend` | `local` (volume `attachments`) or `cloudinary` (private files, authenticated delivery, signed 5-minute download links) |
| `CLOUDINARY_URL` | empty | `backend` | **secret** `cloudinary://<api_key>:<api_secret>@<cloud_name>` (Cloudinary console › API keys); needed with `ATTACHMENT_STORAGE=cloudinary` (may be empty) |
| `CLOUDINARY_FOLDER` | `maplenou-support` | `backend` | Cloudinary folder of the attachments |
| `ANTIVIRUS` | `none` | `backend` | `none` or `clamav` (every file is scanned before it is stored; start `--profile antivirus`) |
| `ANTIVIRUS_FAIL_CLOSED` | `true` | `backend` | `true` = refuse uploads while ClamAV cannot be reached; `false` = accept them unscanned |
| `CLAMAV_IMAGE` | `clamav/clamav:1.4.3` | `clamav` (`--profile antivirus`) | ClamAV image (about 1 GB of RAM, signatures ~300 MB in the `clamav` volume) |
| `RATE_LIMIT_ENABLED`, `RATE_LIMIT_PER_MINUTE`, `RATE_LIMIT_WRITES_PER_MINUTE` | `true`, `300`, `60` | `backend` | requests (all / writes) per minute and per signed-in user, or per address; above, `429 Too Many Requests` with `Retry-After` |
| `FORWARD_HEADERS_STRATEGY` | `none` | `backend` | `native` behind a trusted proxy (the `public` profile's Caddy): the client address comes from `X-Forwarded-For` |
| `LOG_FORMAT` | empty | `backend` (`LOGGING_STRUCTURED_FORMAT_CONSOLE`) | empty = readable text logs; `ecs` = one JSON object per line for a log tool (Loki, Elastic, Datadog) (may be empty) |
| `METRICS_TOKEN` | empty | `backend`, `prometheus` | **secret** bearer token of `/actuator/prometheus`, 24+ random characters; empty = endpoint absent; required by `--profile monitoring` (may be empty) |
| `PROMETHEUS_IMAGE`, `PROMETHEUS_RETENTION` | `prom/prometheus:v3.5.0`, `15d` | `prometheus` (`--profile monitoring`) | Prometheus image and how long metrics are kept (also capped at 1 GB) |
| `GRAFANA_IMAGE`, `GRAFANA_HOST_PORT`, `GRAFANA_ADMIN_PASSWORD` | `grafana/grafana:12.1.1`, `3003`, `change-me-grafana` | `grafana` (`--profile monitoring`) | Grafana image, host port and `admin` password (change it) |
| `AI_ASSIST_ENABLED` | `false` | `backend` | `true` = AI help in tickets: category/priority suggestion and reply drafts (DEC-CS11-08); needs `--profile docs` and the token below |
| `ASSISTANT_INTERNAL_TOKEN` | empty | `backend`, `assistant` | **secret** shared by the backend and the assistant for `/api/assistant/internal/*`, 24+ random characters; empty = AI help off (may be empty) |
| `SUPPORT_DOMAIN`, `HELP_DOMAIN`, `SITE_DOMAIN` | `support.example.com`, `aide.example.com`, `www.example.com` | `caddy`, `site` (`--profile public`) | public domain names of the web app, of the help centre (user guide + assistant) and of the public site; their DNS records point to the server — see [Deployment](deployment.md) |
| `ACME_EMAIL` | `admin@example.com` | `caddy` | e-mail of the Let's Encrypt account (certificate expiry warnings) |
| `CADDY_IMAGE` | `caddy:2.10.0-alpine` | `caddy` | Caddy image |
| `MAPLENOU_APP_URL`, `ANDROID_APP_URL`, `IOS_APP_URL` | empty | `site` (`--profile public`) | links of the public site (empty = button hidden) |
| `REFERENCE_CACHE_TTL` | `PT10M` | `backend` | lifetime of the in-memory reference data (categories, vocabulary, SLA calendar and targets, notification texts), ISO-8601 duration |
| `DOCS_PUBLIC_URL` | `http://localhost:8006` | `frontend` (`DOCS_BASE_URL`), `assistant` (`ASSISTANT_PUBLIC_BASE_URL`, `ASSISTANT_ALLOWED_ORIGINS`) | public address of the documentation |
| `DOCS_SITES` | `user,staff,developer` | `docs` (build argument) | sites built into the docs image; `user` for a public deployment |
| `ASSISTANT_SITES` | `user,staff,developer` | `assistant` | sites the assistant answers about; `user` for a public deployment |
| `ASSISTANT_CHAT_MODEL` | `gpt-oss:20b` | `assistant` | chat model: a model of the Ollama cloud plan (list: <https://ollama.com/api/tags>); with a local Ollama `qwen2.5:3b` |
| `ASSISTANT_EMBED_MODEL` | empty | `assistant` | embedding model; empty = keyword (BM25) search only, the usual choice with Ollama cloud; with a local Ollama `nomic-embed-text` (may be empty) |
| `ASSISTANT_THINK` | `low` | `assistant` | reasoning effort of `gpt-oss` models (`low`, `medium`, `high`); empty for models without thinking (may be empty) |
| `ASSISTANT_RATE_LIMIT` | `20` | `assistant` | questions per minute and per visitor |
| `ASSISTANT_ANSWER_CACHE_SIZE` | `256` | `assistant` | answers kept in memory (`0` disables the answer cache) |
| `ASSISTANT_ANSWER_CACHE_TTL` | `3600` | `assistant` | lifetime of a cached answer, in seconds |
| `OLLAMA_URL` | `https://ollama.com` | `assistant` | Ollama used by the assistant: Ollama cloud (default), `http://host.docker.internal:11434` (installed on the computer) or `http://ollama:11434` (container) — see [Ollama: cloud, on the computer or in Docker](assistant.md#ollama-cloud-on-the-computer-or-in-docker) |
| `OLLAMA_API_KEY` | empty | `assistant` | **secret** — Ollama cloud key, created at <https://ollama.com/settings/keys>; server side only, never in the browser; empty for a local Ollama (may be empty) |
| `ASSISTANT_PULL_MODELS` | `false` | `assistant` | `true` = download the missing models into a local Ollama once it is reachable (about 2.3 GB once); ignored with Ollama cloud |
| `COMPOSE_PROFILES` | commented out | `docker compose` | optional: profiles started by a plain `docker compose up` (`docs`, or `docs,ollama`) |
| `OLLAMA_IMAGE` | `ollama/ollama:latest` | `ollama` (`--profile ollama` only) | Ollama image; pin a version (`ollama/ollama:<version>`) for reproducible deployments |
| `OLLAMA_KEEP_ALIVE` | `10m` | `ollama` (`--profile ollama` only) | idle time after which a model is unloaded from memory |
| `LOG_MAX_SIZE` | `10m` | every container | size of a log file before rotation |
| `LOG_MAX_FILES` | `3` | every container | log files kept per container |
| `TEST_POSTGRES_DB` | `maplenou_test` | `test-db`, `backend-tests` | test database name |
| `TEST_POSTGRES_USER` | `maplenou` | `test-db`, `backend-tests` | test database user |
| `TEST_POSTGRES_PASSWORD` | `maplenou` | `test-db`, `backend-tests` | test database password |

A few values are fixed in `docker-compose.yml` because they describe the Compose network, not the deployment:
`BACKEND_URL=http://backend:8080` (frontend), `DATABASE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB}`,
`ASSISTANT_INDEX_URL=http://docs`, `OLLAMA_MAX_LOADED_MODELS=1` and `OLLAMA_NUM_PARALLEL=1`. The `assistant` and
`backend` services also map `host.docker.internal` to the host (`extra_hosts: host-gateway`, needed on Linux) so
that they can reach an Ollama, or Bakend, running on the computer.

### Backend (`backend/src/main/resources/application.yml`)

| Variable | Default | Purpose |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | none (`demo` in Compose) | `demo` enables the demo login and seed data |
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | `jdbc:postgresql://localhost:5432/maplenou_support`, `maplenou`, `maplenou` | PostgreSQL connection |
| `JWT_SECRET` | empty (the `demo` profile sets one) | HS256 secret for local/demo tokens; at least 32 bytes |
| `JWT_ISSUER` | `maplenou-support-dev` | expected token issuer |
| `DEV_LOGIN_ENABLED` | `false` (`true` in `demo`) | demo login endpoints |
| `ATTACHMENTS_DIR` | `./data/attachments` (`/data/attachments` in the image) | private attachment storage |
| `FIREBASE_CREDENTIALS_FILE` | empty | Firebase service-account JSON; empty = push is logged only |
| `SERVER_PORT` | `8081` (`8080` inside the Docker image) | HTTP port of a natively started backend (`server.port`); 8080 is left to Bakend |
| `SLA_MODE` | `always` | `maplenou.sla.mode`: `always` (24/7) or `business-hours` |
| `MAPLENOU_CORE_URL` | `http://localhost:8080` | Bakend base URL (`maplenou.core.base-url` and `maplenou.ecosystem.base-url`) |
| `MAPLENOU_LOGIN_ENABLED` | `false` | `maplenou.core.login-enabled`: exposes `POST /api/v1/auth/maplenou/login` and `/mfa`; needs `JWT_SECRET` |
| `ECOSYSTEM_MODE` | `demo` | `demo` (sample data) or `http` (Bakend internal API `/internal/v1/**`) |
| `SUPPORT_SERVICE_TOKEN` | empty | `maplenou.ecosystem.service-token`, sent as `X-Maplenou-Service-Token` to Bakend |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3001` | browser origins allowed to call the API directly |
| `REFERENCE_CACHE_TTL` | `PT10M` | lifetime of the reference-data cache (categories, business calendar) |

Fixed settings in the same file: SLA zone `Africa/Lome` and opening hours 08:00–18:00 (used only with
`SLA_MODE=business-hours`); Bakend timeouts 5 s (sign-in) and 3 s (orders, deliveries); attachments 10 MB per file, 5 per message,
50 MB per ticket; push dispatcher every 5 s, 5 attempts; multipart limit 10 MB per file / 52 MB per request;
reference cache at most 500 entries; gzip compression of JSON responses larger than 1 KB.

### Frontend

| Variable | Default | Purpose |
| --- | --- | --- |
| `BACKEND_URL` | `http://localhost:8081` (`http://backend:8080` in Compose) | API URL used server-side by the proxy |
| `AUTH_MODE` | `demo` | sign-in page: `demo` (personas) or `maplenou` (Maplenou account); read at run time by `/api/auth-config` |
| `SESSION_COOKIE_SECURE` | `false` | set `true` behind HTTPS so the session cookie is `Secure` |
| `DOCS_BASE_URL` | `http://localhost:8006` (from `DOCS_PUBLIC_URL` in Compose) | links from the app to the documentation |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_PROJECT_ID`, `FIREBASE_WEB_MESSAGING_SENDER_ID`, `FIREBASE_WEB_APP_ID`, `FIREBASE_WEB_VAPID_KEY` | empty | public Firebase web values served at run time by `/api/push-config` (browser push); see [Firebase](firebase.md) |

### Documentation build and assistant

| Variable | Used by | Purpose |
| --- | --- | --- |
| `DOCS_SITES` | `documentation/build.py` | sites to build (`user,staff,developer`) |
| `ASSISTANT_ENDPOINT` | `documentation/build.py` | assistant URL written into each site's `config.js` (`/api/assistant`) |
| `ASSISTANT_*`, `OLLAMA_URL` | `assistant/` | see [Documentation assistant](assistant.md) |

In Docker, these variables are set from `.env` (table above).

## Secrets

| Secret | Where | Rule |
| --- | --- | --- |
| `JWT_SECRET` | backend | signs the support service's own tokens. With Maplenou sign-in (`MAPLENOU_LOGIN_ENABLED=true`) it is **required**: a random value of 32+ characters, never the demo default. Without Maplenou sign-in, production may leave it empty and set `spring.security.oauth2.resourceserver.jwt.issuer-uri` to an identity provider |
| `POSTGRES_PASSWORD` | `postgres` and backend (`DATABASE_PASSWORD`) | change it outside local demos |
| `FIREBASE_CREDENTIALS_FILE` | backend | the key file lives in `secrets/` and is mounted read-only; never commit it |
| `SUPPORT_SERVICE_TOKEN` | backend and Bakend (same value on both sides) | protects Bakend's internal API `/internal/v1/**`; 32+ random characters — see [Maplenou platform integration](maplenou-integration.md#connect-real-orders-and-deliveries) |
| `OLLAMA_API_KEY` | assistant | Ollama cloud key; server side only, never sent to the browser |
| `CLOUDINARY_URL` | backend | API key and secret of the Cloudinary account (attachments); never commit it |
| `METRICS_TOKEN` | backend and Prometheus | protects `/actuator/prometheus`; 24+ random characters |
| `ASSISTANT_INTERNAL_TOKEN` | backend and assistant | protects the AI endpoints of the assistant; 24+ random characters |
| `GRAFANA_ADMIN_PASSWORD` | Grafana | change the example value before starting the `monitoring` profile |

- Locally, put values in `.env` (git-ignored). Elsewhere, use the platform's secret manager — not Compose files.
- **Secret files go in `secrets/`.** Everything in that folder except its `README.md` is ignored by Git and by every
  Docker build; Compose mounts it **read-only** into the backend at `/run/secrets/maplenou`. Today it holds the
  Firebase service-account key `firebase-service-account.json` — see [Firebase push notifications](firebase.md).
  The `FIREBASE_WEB_*` values are public and stay in `.env`.
- `POSTGRES_PASSWORD` is applied by the PostgreSQL image only when the `pgdata` volume is first initialized. To change
  it later, change the database user's password (`ALTER USER`) as well, or recreate the volume.
- Generate a strong secret:

=== "Bash"

    ```bash
    openssl rand -base64 48
    ```

=== "PowerShell"

    ```powershell
    [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
    ```

## The demo profile must never run in production

The `demo` profile turns on `dev-login-enabled`, which exposes `GET /api/v1/dev/users` and `POST /api/v1/dev/token`:
anyone who can reach the backend can obtain a token for any demo persona. It also seeds sample data and sets a
publicly known default `JWT_SECRET`.

!!! danger "Before any pilot or production deployment"
    - Do not set `SPRING_PROFILES_ACTIVE=demo` and keep `DEV_LOGIN_ENABLED=false`.
    - Sign in with Maplenou accounts (`MAPLENOU_LOGIN_ENABLED=true`, `AUTH_MODE=maplenou`, a random `JWT_SECRET`
      that is not the demo default) — see [Maplenou platform integration](maplenou-integration.md) — or leave
      `JWT_SECRET` empty and configure an identity provider's `issuer-uri`.
    - Serve both apps over HTTPS only, set `SESSION_COOKIE_SECURE=true`, restrict `CORS_ALLOWED_ORIGINS`.
    - Never expose Bakend's `/internal/` paths on the Internet (reverse-proxy rule).
    - Store secrets in a secret manager.
    - See the full list in the [security review](../architecture/security.md).

## Health checks

| Service | Health check | Notes |
| --- | --- | --- |
| `postgres` | `pg_isready` | the backend starts only once the database is healthy |
| `backend` | image `HEALTHCHECK`: `wget` on `http://127.0.0.1:8080/actuator/health` | public endpoint; probes are enabled, so `/actuator/health/liveness` and `/actuator/health/readiness` exist too; the frontend starts only once the backend is healthy |
| `frontend` | image `HEALTHCHECK`: `fetch` of `/login` | — |
| `docs` | image `HEALTHCHECK`: `/healthz` (nginx) | — |
| `assistant` | image `HEALTHCHECK`: `/api/assistant/health` | the JSON reports Ollama, chat model, embeddings, `indexReady`, answer cache and sections per site |
| `ollama` | `ollama list` | only with `--profile ollama`; the assistant does not wait for it and keeps checking Ollama every 30 s |

`docker compose up -d --build --wait` returns when every container is healthy; `docker compose ps` shows the state
of each one. When something does not start, run the read-only diagnostic script `scripts\doctor.ps1` (Windows) or
`scripts/doctor.sh`: it checks Docker and its memory, the variables missing from `.env`, `docker compose config`,
the free host ports, Ollama (the computer's Ollama and its models, or the Ollama cloud key and model), the
Maplenou platform (Bakend on port 8080 and `SUPPORT_SERVICE_TOKEN` when the integration is on), every container's state and health (with its last 25
log lines when it fails) and the assistant's health. Every service has `restart: unless-stopped`, so it restarts after a crash or a Docker restart unless you
stopped it.

Only `health` is exposed by Actuator (`management.endpoints.web.exposure.include`), and `SecurityConfig` permits
only `/actuator/health/**` without authentication. Expose other endpoints only behind authentication.

## Protection, monitoring and alerts

| What | How it works | Setting |
| --- | --- | --- |
| Rate limits | per signed-in user (or per address before sign-in), counted per minute; above, `429 Too Many Requests` with `Retry-After`; counter `support_ratelimit_rejected_total` | `RATE_LIMIT_*` |
| Request id | every response carries `X-Request-Id` (kept from the request when it is 8–64 safe characters, else generated); it is in every log line (`requestId`) — ask users for it when they report an error | always on |
| Logs | text by default; one JSON object per line with `LOG_FORMAT=ecs`, ready for Loki, Elastic or Datadog. Logs rotate (`LOG_MAX_SIZE` × `LOG_MAX_FILES`) | `LOG_FORMAT` |
| Metrics | `/actuator/prometheus` (HTTP timings and errors, JVM, database pool, `support_outbox_pending`, `support_outbox_failed`), only with `Authorization: Bearer <METRICS_TOKEN>`; absent without a token | `METRICS_TOKEN` |
| Monitoring stack | `docker compose --profile monitoring up -d`: Prometheus (scrapes the backend every 30 s, keeps `PROMETHEUS_RETENTION`, at most 1 GB) with the alert rules of `monitoring/alerts.yml` (API down, server errors, slow answers, notification backlog, failed events, many 429, database pool exhausted) and Grafana on `http://localhost:GRAFANA_HOST_PORT` (user `admin`, `GRAFANA_ADMIN_PASSWORD`, Prometheus already connected) | `--profile monitoring` |
| Antivirus | with `ANTIVIRUS=clamav` and `--profile antivirus`, every attachment is sent to ClamAV before it is stored; an infected file is refused (`ATTACHMENT_INFECTED`); while ClamAV is unreachable uploads are refused (`ANTIVIRUS_UNAVAILABLE`) unless `ANTIVIRUS_FAIL_CLOSED=false`. The first start downloads the signatures (a few minutes) | `ANTIVIRUS*` |
| Private attachments on Cloudinary | `ATTACHMENT_STORAGE=cloudinary`: files are uploaded as private ("authenticated") raw resources in `CLOUDINARY_FOLDER`; downloads go through the API (permission check) with a signed link valid 5 minutes; the secret never reaches the browser. Existing local files are not moved: keep `local` or copy them before switching | `ATTACHMENT_STORAGE`, `CLOUDINARY_*` |
| Domain events | stored in `outbox_events` with the change and delivered right after commit; a poller retries what failed (crash, restart). `support_outbox_failed` > 0 means an event failed after every retry: read the backend log | always on |
| Automatic closure | every 15 minutes, resolved tickets older than `SUPPORT_AUTO_CLOSE_AFTER` are closed (history `AUTO_CLOSED`) | `SUPPORT_AUTO_CLOSE_AFTER` |

Check the metrics endpoint by hand:

```bash
curl -s -H "Authorization: Bearer $METRICS_TOKEN" http://localhost:8081/actuator/prometheus | grep '^support_'
```

```powershell
$t = (Select-String '^METRICS_TOKEN=' .env).Line.Split('=',2)[1]
(Invoke-WebRequest http://localhost:8081/actuator/prometheus -Headers @{ Authorization = "Bearer $t" }).Content -split "`n" | Select-String '^support_'
```

## Logs and log rotation

Logs go to standard output; read them with Compose:

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose --profile docs logs -f assistant ollama
```

Every container of both Compose files uses the `json-file` driver with rotation (`x-logging` anchor): a log file
grows to `LOG_MAX_SIZE` (`10m`), then rotates, and `LOG_MAX_FILES` (`3`) files are kept — at most about 30 MB per
container with the defaults. New values apply when `docker compose up -d` recreates the containers.

Useful backend messages: `Firebase credentials not configured: push notifications are logged only.`,
`Push delivery failed for notification …`, `Unexpected error` (with the stack trace, never sent to clients). The
assistant logs `Loaded {...}` with the number of sections per site, `Ollama expected at <url>` (followed by
`(cloud, API key set)` with Ollama cloud), `Embeddings disabled (ASSISTANT_EMBED_MODEL empty): lexical retrieval`
when no embedding model is set, `Pulling <model> into Ollama (first start only)…`
and `Model <model> ready` while it downloads the models, and `Embeddings ready` once semantic ranking is on.
Logs are plain text; structured logs, metrics and tracing are upgrade candidates (observability, before a pilot).

## Backups

The state lives in four Compose volumes:

| Volume | Content | Back up? |
| --- | --- | --- |
| `pgdata` | the PostgreSQL database | yes |
| `attachments` | uploaded files (`/data/attachments` in the backend) | yes, together with the database (rows reference the files) |
| `ollama` | downloaded models | no, they can be pulled again |
| `assistant-cache` | embeddings of the documentation | no, they are recomputed |

`scripts/backup.sh` (macOS, Linux) and `scripts/backup.ps1` (Windows) back up the running Docker stack into
`backups/<date>_<time>/` (git-ignored): a `pg_dump` of the database (`database.sql.gz` with Bash,
`database.sql.zip` with PowerShell) and `attachments.tar.gz`. They read `POSTGRES_USER` and `POSTGRES_DB` from
`.env`. Only the **7 most recent** backups are kept; older ones are deleted, so backups cannot fill the disk.

=== "Bash"

    ```bash
    ./scripts/backup.sh                                     # new backup in backups/<date>_<time>/
    ./scripts/backup.sh restore backups/<date>_<time>       # REPLACES the current data
    ```

=== "PowerShell"

    ```powershell
    .\scripts\backup.ps1
    .\scripts\backup.ps1 -Restore backups\<date>_<time>
    ```

A restore drops and re-creates the `public` schema, loads the dump, unpacks the attachments into the backend and
restarts it. Restore a backup with the script of the same family (`.sh` or `.ps1`): the dump formats differ.

!!! warning "Before a pilot"
    The scripts run on demand and write next to the repository. Schedule them (cron, Task Scheduler), copy the
    backups to another machine and test a restore regularly before relying on them.

## Disk space

See what Docker uses with `docker system df`. The clean scripts free space without touching other projects:

| Command | Removes |
| --- | --- |
| `./scripts/clean.sh` · `.\scripts\clean.ps1` | build outputs (`backend/target`, `frontend/.next`, Playwright reports, built sites, generated docs assets, caches), the project's containers (the stack is stopped), dangling images, Docker build cache older than 7 days |
| `./scripts/clean.sh --deps` · `.\scripts\clean.ps1 -Deps` | the above + `frontend/node_modules` and the Python virtual environments (`.venv`, `assistant/.venv`) |
| `./scripts/clean.sh --all` · `.\scripts\clean.ps1 -All` | the above + the Docker volumes of both Compose files: database, attachments, Ollama models, caches — **the data is lost** (with PowerShell, add `-Deps` to also remove the dependencies) |

The other limits (log rotation, multi-stage images, one Ollama image, in-memory test database, backup retention)
are listed in [Caching and disk space](caching.md).

## Publishing the documentation publicly

Only the user guide may be public:

1. Build the user site only: `DOCS_SITES=user` in `.env` (or `python documentation/build.py --sites user`).
2. Run the assistant with `ASSISTANT_SITES=user`, so it neither indexes nor answers about internal sites.
3. Set `DOCS_PUBLIC_URL` (used for `ASSISTANT_PUBLIC_BASE_URL`, `ASSISTANT_ALLOWED_ORIGINS` and the app's
   documentation links) to the public HTTPS address.
4. Keep the staff and developer sites, and any assistant instance that serves them, on an internal network.

See [Documentation sites](documentation.md) for the build and [Documentation assistant](assistant.md) for the
assistant's security model.

## Ollama memory and CPU

The `ollama` service (`--profile ollama`) is configured to stay light on an ordinary machine:

| Setting | Value | Effect |
| --- | --- | --- |
| `OLLAMA_KEEP_ALIVE` | `.env`, `10m` | a model unused for this long is unloaded, which frees its RAM between questions |
| `OLLAMA_MAX_LOADED_MODELS` | `1` (fixed in `docker-compose.yml`) | at most one model in memory: the embedding and chat models take turns |
| `OLLAMA_NUM_PARALLEL` | `1` (fixed in `docker-compose.yml`) | one request at a time, so CPU and memory use stay predictable |

The first question after an idle period is slower because the model is loaded again. To use an NVIDIA GPU,
uncomment the `deploy` block of the `ollama` service (requires the NVIDIA Container Toolkit). With Ollama installed
on the computer, these settings are those of the Ollama app (environment variables of the same names). With Ollama
cloud (the default), nothing runs locally: memory and CPU are those of ollama.com, and the free plan handles one
request at a time with limited usage (see <https://ollama.com/pricing>).

## Upgrading the Ollama models

1. Set `ASSISTANT_CHAT_MODEL` and/or `ASSISTANT_EMBED_MODEL` in `.env`. With Ollama cloud, choose a chat model
   listed by <https://ollama.com/api/tags> and keep `ASSISTANT_EMBED_MODEL` empty; steps 2–4 below then only
   restart the assistant (nothing is pulled, nothing to remove).
2. `docker compose --profile docs up -d --build --wait` — `assistant` restarts with the new values, pulls the
   missing models through the Ollama API and recomputes the embeddings if the embedding model changed (the cache key
   includes the model).
3. Check `curl http://localhost:8006/api/assistant/health` (`chatModel` must be the new name, `embeddings: true`).
4. Remove models you no longer use: `ollama list` then `ollama rm <name>` with the computer's Ollama, or
   `docker compose exec ollama ollama list` then `docker compose exec ollama ollama rm <name>` with the container.

`OLLAMA_IMAGE` defaults to `ollama/ollama:latest`; set a pinned version in `.env` if you need identical
deployments.
