# Getting started

This page takes you from a fresh clone to a running application, documentation and assistant. You only need
Docker; Java, Node.js and Python are optional and only make some development loops faster.

## Prerequisites: Docker only

| Tool | Version | Needed for |
| --- | --- | --- |
| Docker Engine or Docker Desktop | with Compose v2 (`docker compose`) | everything: app, documentation, assistant, tests |
| Git | any recent version | cloning the repository |

Nothing else has to be installed: the backend, the frontend, the documentation build, the assistant and every test
suite run in containers (`docker-compose.yml`, `docker-compose.test.yml`).

```bash
git clone <repository-url>
cd <repository-directory>
```

## First step: create the .env file

Both Compose files read **every** value (ports, credentials, models, limits) from `.env`; nothing is hard-coded in
them. A missing variable stops Compose with a message such as `Set POSTGRES_DB in .env (copy .env.example)`. Create
the file once from the documented template (`.env` is git-ignored):

=== "Bash"

    ```bash
    cp .env.example .env
    ```

=== "PowerShell"

    ```powershell
    copy .env.example .env
    ```

The defaults of `.env.example` are meant for local demos. The test scripts (`scripts/test.sh`, `scripts/test.ps1`)
create `.env` from `.env.example` automatically when it is missing.

## Run the application with demo data

```bash
docker compose up -d --build --wait
```

`--wait` returns once every container is running and healthy. Compose starts three services:

| Service | Image / build | Host port | Role |
| --- | --- | --- | --- |
| `postgres` | `postgres:16-alpine` | not published | database `POSTGRES_DB` (`maplenou_support`, volume `pgdata`) |
| `backend` | `./backend` (Spring Boot, Java 21) | `8081` (`BACKEND_HOST_PORT`; 8080 inside the container) | REST API with the profile `SPRING_PROFILES_ACTIVE` (`demo`); attachments in volume `attachments` |
| `frontend` | `./frontend` (Next.js standalone) | `3001` (`FRONTEND_HOST_PORT`) | web app; calls the backend at `http://backend:8080` |

The host ports 3001 and 8081 leave 3000 and 8080 free for the Maplenou admin back-office and Bakend, which run on
the same computer once this project is the `ServiceClient/` folder of the Maplenou repository (see
[Maplenou platform integration](maplenou-integration.md)). Every service has a health check and `restart: unless-stopped`; the backend waits for a healthy database and the
frontend for a healthy backend. Container logs are rotated (`LOG_MAX_SIZE` × `LOG_MAX_FILES`, see
[Operations](operations.md)).

Then open:

| URL | What |
| --- | --- |
| <http://localhost:3001> | web app — the login page lists the demo personas |
| <http://localhost:8081/swagger-ui.html> | interactive API documentation (Swagger UI) — see [Test the API](api-testing.md) |
| <http://localhost:8081/api/v1/support> | API base path (needs a bearer token) |
| <http://localhost:8081/actuator/health> | backend health |

The `demo` profile enables a demo login and seeds sample orders, deliveries and tickets. The personas come from
`backend/src/main/java/com/maplenou/support/demo/DemoUsers.java`:

| Persona | Role | Lands on |
| --- | --- | --- |
| Jean K. | `CUSTOMER` | `/aide` |
| Marie K. | `SELLER` | `/aide` |
| Kossi D. | `DELIVERY_DRIVER` | `/aide` |
| Sophie N., Paul D. | `SUPPORT_AGENT` | `/support` |
| Amina K. | `SUPPORT_MANAGER` | `/support` |
| Admin Maplenou | `ADMINISTRATOR` | `/support` |

Follow the logs with `docker compose logs -f backend`. Stop the stack with `docker compose down`.
`docker compose down -v` also deletes the volumes (database, attachments, downloaded Ollama models and the
assistant's embedding cache), so the next start re-seeds the demo data. To check a running stack end to end, run
`scripts/smoke-test.sh` (or `scripts\smoke-test.ps1`) — see [Testing](testing.md). To call the API by hand, use
Swagger UI or the Postman collection of `backend/http/postman/` — see [Test the API (Swagger, Postman)](api-testing.md).

!!! warning "Demo only"
    The `demo` profile and the default `JWT_SECRET` of `.env.example` are for local demonstrations. Never run them in
    production — see [Operations](operations.md). To sign in with real Maplenou accounts and read real orders from
    Bakend instead, follow [Maplenou platform integration](maplenou-integration.md).

## Run the documentation and the assistant

```bash
docker compose --profile docs up -d --build --wait
```

The `docs` profile adds two services to the three above (three with the `ollama` profile):

| Service | Role |
| --- | --- |
| `docs` | nginx serving the documentation sites listed in `DOCS_SITES`, built once in the image (`documentation/Dockerfile`), on host port `8006` (`DOCS_HOST_PORT`) |
| `assistant` | the documentation assistant API (`./assistant`), reached by the sites at `/api/assistant`; reads the search indexes from the `docs` server (`ASSISTANT_INDEX_URL=http://docs`) and calls Ollama at `OLLAMA_URL` |
| `ollama` | only with `--profile ollama`: `OLLAMA_IMAGE` (`ollama/ollama:latest`), models stored in the `ollama` volume, at most one model in memory |

Open <http://localhost:8006> for the portal page, which links to every site and language.

The assistant needs an Ollama server; `OLLAMA_URL` in `.env` chooses it:

- **Ollama cloud** (default, `OLLAMA_URL=https://ollama.com`): nothing to install and no model on the disk. Create a
  free account on <https://ollama.com>, then a key at <https://ollama.com/settings/keys>, and paste it in `.env` as
  `OLLAMA_API_KEY=…` (a secret: `.env` is never committed and the key never reaches the browser). `.env.example`
  already sets `ASSISTANT_CHAT_MODEL=gpt-oss:20b`, `ASSISTANT_EMBED_MODEL=` (empty: keyword search only),
  `ASSISTANT_THINK=low` and `ASSISTANT_PULL_MODELS=false`.
- **Ollama installed on the computer** (`OLLAMA_URL=http://host.docker.internal:11434`): install and start the
  Ollama app for Windows or macOS, empty `OLLAMA_API_KEY` and `ASSISTANT_THINK`, set
  `ASSISTANT_CHAT_MODEL=qwen2.5:3b`, `ASSISTANT_EMBED_MODEL=nomic-embed-text` and `ASSISTANT_PULL_MODELS=true`, then
  use the command above. No Ollama image is downloaded.
- **Ollama in a container** (`OLLAMA_URL=http://ollama:11434`, other values as for the computer): add the `ollama`
  profile — `docker compose --profile docs --profile ollama up -d --build --wait`.

The documentation starts even when Ollama is not reachable yet (or the key is missing); details and troubleshooting
in [Ollama: cloud, on the computer or in Docker](assistant.md#ollama-cloud-on-the-computer-or-in-docker).

!!! info "Local Ollama: the first start downloads the models"
    With Ollama cloud nothing is downloaded. With a local Ollama and `ASSISTANT_PULL_MODELS=true`, the assistant
    downloads on its first start the embedding model `ASSISTANT_EMBED_MODEL` (`nomic-embed-text`) and the chat model
    `ASSISTANT_CHAT_MODEL` (`qwen2.5:3b`) through the Ollama API into the Ollama it uses (about 2.3 GB in total;
    later starts find them and skip the download). Until they are present the assistant still works: it ranks
    sections lexically and, without the chat model, answers with the closest sections only. In every mode, while the
    search indexes are not yet loaded from the `docs` server, questions get `503 INDEX_LOADING`. Follow progress
    with `docker compose --profile docs logs -f assistant` and `curl http://localhost:8006/api/assistant/health`.

## Configure ports and secrets with .env

Change a value in `.env`, then run `docker compose up -d --build --wait` again. The values you are most likely to
change:

| Variable | Default | Used by |
| --- | --- | --- |
| `FRONTEND_HOST_PORT` | `3001` | host port of the web app (3000 is the Maplenou admin back-office) |
| `BACKEND_HOST_PORT` | `8081` | host port of the API (8080 is Bakend) |
| `DOCS_HOST_PORT` | `8006` | host port of the documentation (`docs` profile) |
| `POSTGRES_HOST_PORT` | `5433` | only used by `docker-compose.override.yml` |
| `POSTGRES_PASSWORD` | `maplenou` | database password (database and backend) |
| `JWT_SECRET` | `demo-only-secret-change-me-0123456789abcdef` | HS256 secret of the support service's own tokens (demo and Maplenou sign-in) |
| `SLA_MODE` | `always` | SLA clock: `always` = 24/7 (weekends and public holidays count), `business-hours` = Monday–Friday 08:00–18:00 minus holidays |
| `MAPLENOU_CORE_URL` | `http://host.docker.internal:8080` | Bakend (Maplenou core API) as seen from the containers |
| `MAPLENOU_LOGIN_ENABLED` | `false` | `true` = sign-in with a Maplenou account (with `AUTH_MODE=maplenou` and `SPRING_PROFILES_ACTIVE=default`) |
| `AUTH_MODE` | `demo` | sign-in page of the web app: `demo` (personas) or `maplenou` |
| `ECOSYSTEM_MODE` | `demo` | `demo` (sample orders) or `http` (real orders and deliveries from Bakend) |
| `SUPPORT_SERVICE_TOKEN` | empty | secret shared with Bakend, needed by `ECOSYSTEM_MODE=http` and the import of the former tickets |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3001` | browser origins allowed to call the API |
| `DOCS_PUBLIC_URL` | `http://localhost:8006` | documentation links from the app, assistant reference URLs and allowed origin |
| `OLLAMA_URL` | `https://ollama.com` | Ollama used by the assistant: Ollama cloud (default), `http://host.docker.internal:11434` (computer) or `http://ollama:11434` (container, `--profile ollama`) |
| `OLLAMA_API_KEY` | empty | secret Ollama cloud key (<https://ollama.com/settings/keys>); empty for a local Ollama |
| `ASSISTANT_CHAT_MODEL` | `gpt-oss:20b` | chat model (local Ollama: `qwen2.5:3b`) |
| `ASSISTANT_EMBED_MODEL` | empty | embedding model; empty = keyword search only (local Ollama: `nomic-embed-text`) |
| `ASSISTANT_THINK` | `low` | reasoning effort of `gpt-oss`; empty for models without thinking |
| `ASSISTANT_PULL_MODELS` | `false` | `true` = the assistant downloads missing models into a local Ollama |
| `COMPOSE_PROFILES` | commented out | optional: profiles started by a plain `docker compose up` (for example `docs`, or `docs,ollama`) |
| `FIREBASE_CREDENTIALS_FILE` | empty | backend: Firebase service-account key, `/run/secrets/maplenou/firebase-service-account.json` (file in `secrets/`); empty = pushes only written to the log — see [Firebase](firebase.md) |
| `FIREBASE_WEB_API_KEY`, `FIREBASE_WEB_AUTH_DOMAIN`, `FIREBASE_WEB_PROJECT_ID`, `FIREBASE_WEB_MESSAGING_SENDER_ID`, `FIREBASE_WEB_APP_ID`, `FIREBASE_WEB_VAPID_KEY` | empty | web app: public Firebase values for browser push, read at run time by `/api/push-config`; all empty = no browser push |

If you change `FRONTEND_HOST_PORT`, update `CORS_ALLOWED_ORIGINS`; if you change `DOCS_HOST_PORT`, update
`DOCS_PUBLIC_URL` to the same port, so links and CORS stay correct. Every variable is listed in
[Operations](operations.md).

## When something does not start: the doctor script

Run the read-only diagnostic script:

=== "PowerShell"

    ```powershell
    scripts\doctor.ps1
    ```

=== "Bash"

    ```bash
    scripts/doctor.sh
    ```

It checks that Docker runs and has enough memory, lists the variables of `.env.example` missing from `.env`,
validates `docker compose config`, checks that the host ports (`FRONTEND_HOST_PORT`, `BACKEND_HOST_PORT`,
`DOCS_HOST_PORT`) are free, that Ollama works (the computer's Ollama and its models, or with Ollama cloud the key
and the model on ollama.com), that Bakend answers when the Maplenou integration is on, and shows the state and
health of every container — with its last 25 log lines when it fails — and the assistant's health. It changes
nothing. More symptoms: [Troubleshooting](assistant.md#troubleshooting).

## "Port 5432 is already allocated"

PostgreSQL is **not published** to your machine by default: the backend reaches it inside the Compose network as
`postgres:5432`. This avoids the classic `Bind for 0.0.0.0:5432 failed: port is already allocated` error when a
PostgreSQL is already installed locally.

If you need the database from your machine (SQL client, natively running backend), publish it on another port
with the override example — Compose loads `docker-compose.override.yml` automatically and the file is git-ignored:

=== "Bash"

    ```bash
    cp docker-compose.override.example.yml docker-compose.override.yml
    docker compose up -d postgres
    psql -h localhost -p 5433 -U maplenou maplenou_support
    ```

=== "PowerShell"

    ```powershell
    Copy-Item docker-compose.override.example.yml docker-compose.override.yml
    docker compose up -d postgres
    psql -h localhost -p 5433 -U maplenou maplenou_support
    ```

The host port is `POSTGRES_HOST_PORT` (default `5433`). If another port is taken (`3001`, `8081`, `8006`), set
the matching `*_HOST_PORT` in `.env` (keep away from 3000 and 8080, used by the Maplenou admin back-office and
Bakend). To see which program holds a port:

=== "Bash"

    ```bash
    docker ps --format '{{.Names}}\t{{.Ports}}'
    lsof -i :5432          # macOS / Linux
    ```

=== "PowerShell"

    ```powershell
    docker ps --format '{{.Names}}\t{{.Ports}}'
    Get-NetTCPConnection -LocalPort 5432 | Select-Object OwningProcess
    ```

## Optional native toolchains

Native toolchains give faster edit–run loops. Versions are pinned in the repository (see [AI agents](ai-agents.md)
for the full list).

| Part | Toolchain | Notes |
| --- | --- | --- |
| Backend | Java 21 (JDK) | Maven comes from the wrapper `backend/mvnw` (`mvnw.cmd` on Windows) |
| Frontend | Node.js 22 (npm 10) | install with `npm ci --legacy-peer-deps` |
| Documentation | Python 3.11+ and `uv` | `pyproject.toml` pins `zensical==0.0.69`; `uv.lock` locks it |
| Assistant | Python 3.12+ and `uv` (0.13.0 in the images) | `assistant/pyproject.toml` and `assistant/uv.lock` (dev group: pytest) |

### Backend against the Compose database

The native backend needs the database published on the host (override above), so point it at port `5433`:

=== "Bash"

    ```bash
    cp docker-compose.override.example.yml docker-compose.override.yml
    docker compose up -d postgres
    cd backend
    DATABASE_URL=jdbc:postgresql://localhost:5433/maplenou_support SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run
    ```

=== "PowerShell"

    ```powershell
    Copy-Item docker-compose.override.example.yml docker-compose.override.yml
    docker compose up -d postgres
    cd backend
    $env:DATABASE_URL="jdbc:postgresql://localhost:5433/maplenou_support"; $env:SPRING_PROFILES_ACTIVE="demo"; .\mvnw.cmd spring-boot:run
    ```

The native backend listens on `http://localhost:8081` (`server.port: ${SERVER_PORT:8081}`), so it can run next to
Bakend on 8080.

### Frontend against a running backend

```bash
cd frontend
npm ci --legacy-peer-deps
cp .env.example .env.local     # then set BACKEND_URL=http://localhost:8081 (the native backend's port) in it
npm run dev                    # http://localhost:3001 (npm run dev/start use port 3001)
```

### Assistant tests natively

```bash
cd assistant
uv sync          # creates assistant/.venv from uv.lock, including the dev group (pytest)
uv run pytest
```

Running the assistant itself against locally built sites is described in [Documentation sites](documentation.md).

### Documentation preview

```bash
uv sync                                          # installs zensical 0.0.69 from uv.lock
uv run python documentation/build.py --prepare-only
uv run zensical serve -f zensical.toml -a localhost:8007   # English developer site
```

Details, including the other sites, are in [Documentation sites](documentation.md).
