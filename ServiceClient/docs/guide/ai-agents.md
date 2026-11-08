# AI agents

AI coding agents (Claude Code, GitHub Copilot, Cursor, Codex and similar) are expected to work in this repository
the same way a new developer does: read the written context first, stay within approved scope, and leave the
records up to date. This page explains the instruction files and how to reproduce the project identically.

## Instruction files

| File | Role |
| --- | --- |
| `AGENTS.md` (repository root) | **canonical instructions** for every agent: project context, read order, rules, commands |
| `backend/AGENTS.md`, `frontend/AGENTS.md`, `assistant/AGENTS.md`, `documentation/AGENTS.md` | rules specific to each part, read when working in that folder |
| `CLAUDE.md` files | contain `@AGENTS.md`, so Claude Code imports the same instructions instead of a diverging copy |
| `.github/copilot-instructions.md`, `.github/instructions/mermaid.instructions.md` | GitHub Copilot instructions (Mermaid diagram conventions) |

Edit `AGENTS.md`, never a copy: tools that read another file name import or point to it.

## Read order

1. `AGENTS.md` (root, then the one of the folder you work in).
2. [Project continuity](../project/PROJECT-CONTINUITY.md) — current phase, decisions awaiting approval, next step.
3. [Development matrix](../project/DEVELOPMENT-MATRIX.md) — which item is active and its dependencies.
4. [Reference architecture](../architecture/reference-architecture.md) — styles and principles the code follows.
5. Then the records relevant to the task: [architecture decisions](../architecture/decisions.md), requirements,
   UX/UI, and this developer guide.

## Rules for agents

- **Stay within approved scope.** Implement only the active matrix item. Propose new ideas in the
  [upgrade plan](../governance/UPGRADE-PLAN.md) instead of implementing them. Never present planned work as done.
- **Respect the architecture.** Business rules on the server (`TicketAccessPolicy`, `ticket.domain`); external
  systems behind ports; side effects after commit; database changes as new Flyway migrations.
- **Keep French/English parity.** Every UI string in both dictionaries; every user, staff and developer guide page
  in both languages with the same file name; the project records stay in English.
- **Run the tests with Docker** — no local toolchain is assumed: `./scripts/test.sh` (or `.\scripts\test.ps1`) and
  `python3 documentation/build.py --strict` for documentation. Report the commands you ran and their result.
- **Update the records.** Matrix, continuity checkpoint, changelog, traceability, architecture/API/database
  documents and diagrams, as listed in the [definition of done](contributing.md).
- **Configure through `.env`.** A new setting of the Docker stack is a variable documented in `.env.example` and
  used as `${VAR:?…}` in the Compose files; never hard-code a port, credential, model or limit in
  `docker-compose*.yml`.
- **Bound every cache and keep images small.** A cache has a maximum size and a lifetime, is cleared after commit and
  never holds ticket data on the server or mixes users' data ([Caching and disk space](caching.md)). Images are
  multi-stage on a pinned slim base already used by the project, with BuildKit cache mounts and a `.dockerignore`.
- **Use the lock files.** Python dependencies are managed with uv: edit `pyproject.toml`, run `uv lock`, commit
  `uv.lock` (root for the documentation, `assistant/` for the assistant); there is no `requirements.txt`.
- **Verify with the documented commands** — [Testing](testing.md) lists them with and without Docker, in PowerShell
  and Bash, including the smoke test of a running stack.
- **Never** commit secrets or `.env` files, enable the `demo` profile outside local use, edit an applied migration,
  or expose internal documentation sites publicly.

## Reproduce the project identically

Every version that matters is pinned in the repository:

| Component | Version | Pinned in |
| --- | --- | --- |
| Java | 21 | `backend/pom.xml` (`java.version`), `backend/Dockerfile` (`eclipse-temurin:21-jdk-noble` → `21-jre-alpine`), `docker-compose.test.yml` |
| Spring Boot | 4.1.1 | `backend/pom.xml` (parent) |
| Maven | wrapper | `backend/mvnw`, `backend/.mvn/wrapper/maven-wrapper.properties` |
| springdoc / Firebase Admin / Testcontainers | 3.1.1 / 9.11.0 / 2.0.5 | `backend/pom.xml` |
| PostgreSQL | 16 | `docker-compose.yml`, `docker-compose.test.yml`, `IntegrationTest` (`postgres:16-alpine`) |
| Node.js | 22 | `frontend/Dockerfile` and `docker-compose.test.yml` (`node:22-alpine`), CI |
| Next.js / React | 16.4.3 / 19.3.0 | `frontend/package.json`, `frontend/package-lock.json` |
| Playwright | 1.56.0 | `frontend/package.json`, `docker-compose.test.yml` image |
| Python | 3.12 | `.python-version`, `assistant/.python-version`, `assistant/Dockerfile` and `documentation/Dockerfile` (`python:3.12-slim-bookworm`) |
| uv | 0.13.0 | `assistant/Dockerfile`, `documentation/Dockerfile` (`ghcr.io/astral-sh/uv:0.13.0`) |
| Zensical | 0.0.69 | `pyproject.toml`, `uv.lock` |
| Assistant dependencies | starlette 0.48.0, uvicorn 0.37.0, httpx 0.28.1, pytest 8.4.2 | `assistant/pyproject.toml`, `assistant/uv.lock` |
| nginx | 1.29 | `documentation/Dockerfile` (`nginx:1.29-alpine`) |
| Ollama models | Ollama cloud: `gpt-oss:20b` (`.env.example`); local Ollama: `qwen2.5:3b`, `nomic-embed-text` (defaults of `assistant/app/config.py`) | `.env.example`, `assistant/app/config.py` |

Install with the lock files: `npm ci --legacy-peer-deps` (never `npm install`), `./mvnw` (never a system Maven
version), `uv sync` at the root and in `assistant/` (uses `uv.lock` and `assistant/uv.lock`; the images run
`uv sync --locked`, which fails when a lock file is out of date). The only unpinned image is the Ollama image
(`OLLAMA_IMAGE=ollama/ollama:latest` in `.env.example`); pin it in `.env` for identical deployments.

From a clean clone, these commands reproduce the stack, the tests and the documentation:

```bash
cp .env.example .env                                 # Windows: copy .env.example .env
docker compose up -d --build --wait                  # app + API with demo data
docker compose --profile docs up -d --build --wait   # + documentation and assistant
./scripts/test.sh all                                # backend, frontend, assistant, docs, e2e
./scripts/smoke-test.sh                              # the running stack answers end to end
python3 documentation/build.py --strict              # all documentation sites
```

## Handing work back

End a session the way the [Reproduction guide](../governance/REPRODUCTION.md) asks of any developer: tests run and
reported, documentation in both languages, matrix and continuity checkpoint updated, open questions written down,
and the next step unambiguous.
