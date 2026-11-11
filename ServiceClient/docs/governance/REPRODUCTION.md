# Project Reproduction Guide

This guide allows another developer to reproduce the project environment and continue development with or without the project owner.

## Required reading

Before coding:

1. Root `README.md`
2. `docs/project/PROJECT-CONTINUITY.md`
3. `docs/project/DEVELOPMENT-MATRIX.md`
4. `docs/governance/REFERENCES.md`
5. `backend/README.md`
6. `frontend/README.md`
7. `docs/architecture/` (decisions, database, API) and the UML documents

## Reproduction principle

The repository must contain enough documented information to reproduce development without private knowledge.

## Versions

| Tool | Version |
| --- | --- |
| Java (JDK) | 21 |
| Maven | provided by the wrapper (`backend/mvnw`, `mvnw.cmd` on Windows) |
| Spring Boot | 4.1.1 |
| PostgreSQL | 16 |
| Node.js | 22 (npm 10) |
| Next.js / React | 16.4 / 19.3 |
| Python (docs + assistant) | 3.12 (`zensical==0.0.69`; assistant: `assistant/requirements*.txt`) |
| Ollama models | `qwen2.5:3b`, `nomic-embed-text` (pulled by the `docs` profile). *Amended 2026-10-29 by DEC-CS10-18: default is Ollama cloud with `gpt-oss:20b`, no embedding model, nothing pulled; the local models remain the alternative.* |
| Docker | Compose v2 — the only requirement for the Docker-only path |

## Local setup

```bash
git clone <repository-url>
cd <repository-directory>
```

### Option A — everything in Docker

```bash
cp .env.example .env                       # optional: host ports and secrets
docker compose up --build                  # http://localhost:3001 (demo login) and http://localhost:8081
docker compose --profile docs up --build   # + documentation http://localhost:8006 and the Ollama assistant
scripts/test.sh all                        # every test suite in containers (Windows: scripts\test.ps1 all)
docker compose down -v                     # stop and delete the demo data
```

### Option B — run each part

```bash
# 1. Database: PostgreSQL 16 running locally, or the Compose one published on port 5433:
#    cp docker-compose.override.example.yml docker-compose.override.yml && docker compose up -d postgres
#    (then use DATABASE_URL=jdbc:postgresql://localhost:5433/maplenou_support for the backend)
psql -U postgres -c "CREATE USER maplenou WITH PASSWORD 'maplenou'" -c "CREATE DATABASE maplenou_support OWNER maplenou"

# 2. Backend (demo profile)
cd backend
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run        # Windows PowerShell: $env:SPRING_PROFILES_ACTIVE="demo"; .\mvnw.cmd spring-boot:run

# 3. Frontend (second terminal)
cd frontend
npm ci --legacy-peer-deps
echo BACKEND_URL=http://localhost:8081 > .env.local
npm run dev
```

Do not commit secrets. Use environment variables (`.env` files are git-ignored) or the deployment platform's secret manager.

## Before implementation

Identify the current matrix item and its dependency status.

Do not start a new feature because it appears useful if it is not part of the active scope.

## During implementation

For every development slice:

1. Implement the approved behavior.
2. Add tests.
3. Update documentation.
4. Update Mermaid/UML diagrams if behavior or architecture changed.
5. Update the development matrix.
6. Update the continuity checkpoint.
7. Commit using Conventional Commits.

Database changes are new Flyway migrations (`V2__…sql`); never edit a migration that has been applied anywhere.

## Verification

At minimum, a contributor must be able to:

- build the backend and run its tests: `cd backend && ./mvnw verify` (needs Docker for Testcontainers, or set `TEST_DATABASE_URL`),
- build/test the frontend: `cd frontend && npm ci --legacy-peer-deps && npm run lint && npm run typecheck && npm test && npm run build`,
- run the browser scenario: start the stack, then `cd frontend && npx playwright install chromium && npx playwright test`,
- run the assistant tests: `cd assistant && pip install -r requirements-dev.txt && python -m pytest`,
- build the six documentation sites: `pip install zensical==0.0.69 && python documentation/build.py --strict`,
- or, without Java/Node/Python, all of the above in containers: `scripts/test.sh all` (Windows: `scripts\test.ps1 all`),
- inspect the current project status from the continuity and matrix documents.

## Handoff

Before handing work to another developer:

- ensure the working tree is clean,
- update the matrix,
- update the continuity checkpoint,
- document blockers,
- document unfinished work,
- record important decisions,
- make the next development item unambiguous.
