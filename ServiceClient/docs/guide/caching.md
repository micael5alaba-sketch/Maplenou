# Caching and disk space

Two goals guide every cache in the project: **answer the end user faster with less network and less work**, and
**never fill a disk or the memory** — every cache has a size limit and a lifetime, and never mixes users' data.

## Overview

| Layer | What is cached | Bound | Invalidation | Where |
| --- | --- | --- | --- | --- |
| Browser (web app) | GET responses of the API, in memory, per tab | 100 responses (LRU) | after any change (POST/PUT/DELETE), on sign-out, page reload | `frontend/src/lib/apiCache.ts`, `useApi.ts` |
| Browser (static files) | Next.js `/_next/static/*` (hashed names) | browser-managed | new file name on each build | Next.js default `immutable` headers |
| Browser (push) | nothing: `push-sw.js` only shows notifications (no cache, no network request); once push is turned on, the Firebase SDK keeps its token in IndexedDB — the documented exception to "no IndexedDB", required by Firebase, removed when push is turned off | one registration per browser | turning push off, sign-out unsubscribes the browser | `frontend/public/push-sw.js`, `src/lib/push.ts` — see [Firebase](firebase.md) |
| Browser (docs) | theme assets with a hash: 1 year; images/fonts: 1 day; pages, search indexes and our widget: revalidated (`no-cache` + ETag → `304`) | browser-managed | new build | `documentation/nginx.conf.template` |
| Network | gzip: API JSON > 1 KB (Spring), docs pre-compressed at build time (`gzip_static`), Next.js responses | — | — | `application.yml`, `documentation/Dockerfile` |
| Backend | reference data: categories, vocabulary (statuses, transitions, priorities, permissions), SLA calendar and policies, notification templates — one snapshot per service | one small object each, reloaded after `REFERENCE_CACHE_TTL` (10 min) | dropped **after commit** of an administrator's change | `ticket/application/CategoryService`, `VocabularyService`, `sla/application/SlaService`, `notification/application/TemplateService` |
| Backend | rate-limit counters (per user or address, one minute) | 50 000 windows, 2 min (Caffeine) | expire by themselves | `platform/observability/RateLimitFilter` |
| HTTP (API) | `GET /categories` and `GET /vocabulary`: `Cache-Control: private, max-age=300` + `ETag` → `304 Not Modified` | per client | content hash changes | `ticket/adapter/in/web/CategoryController`, `VocabularyController` |
| Assistant | help-centre search results (`GET /api/assistant/search`): `Cache-Control: public, max-age=60` (public user guide only) | browser-managed, 1 min | — | `assistant/app/main.py` |
| Assistant | answers to the same question (same site and language) | 256 answers, 1 h (`ASSISTANT_ANSWER_CACHE_*`) | index reload | `assistant/app/cache.py` |
| Assistant | section embeddings, on disk (only with an embedding model, so not with the default Ollama cloud setup) | one file per site/language/model/content hash | new documentation or model | `assistant-cache` volume |
| Ollama (local only) | loaded model in RAM | 1 model (`OLLAMA_MAX_LOADED_MODELS`), unloaded after `OLLAMA_KEEP_ALIVE` | idle time | `docker-compose.yml` |
| Docker build | Maven, npm, uv and Next.js caches as BuildKit **cache mounts** | Docker build cache | `docker builder prune` | the four Dockerfiles |

What is **never** cached: ticket data on the server (it must always be current), authenticated API responses in
the browser's HTTP cache (`no-store`), anything shared between users.

## Disk space

| Source | How it is kept small |
| --- | --- |
| Docker images | multi-stage builds; runtime images contain only what runs: JRE (not JDK) on Alpine for the backend, the Next.js standalone server, a Python virtual environment without build tools, nginx Alpine for the docs; bases are shared between services and tests (`postgres:16-alpine`, `node:22-alpine`, `eclipse-temurin:21-jdk-noble`, `python:3.12-slim-bookworm`) |
| Build context | `.dockerignore` in each folder (allow-list at the repository root) |
| Image layers | Spring Boot layered jar: a code change re-ships ~1 MB, not the 127 MB of libraries |
| Container logs | rotated: `LOG_MAX_SIZE` × `LOG_MAX_FILES` per container (10 MB × 3 by default) |
| Documentation sites | built once, in the docs image; the assistant reads their indexes over HTTP (no second build) |
| Ollama | default: Ollama cloud (`OLLAMA_URL=https://ollama.com`) — no Ollama image, **no model on the disk**, no embedding cache (`ASSISTANT_EMBED_MODEL` empty); with the Ollama installed on the computer, models already downloaded are reused; the container mode (`--profile ollama`) costs several GB of disk (the `ollama/ollama` image plus about 2.3 GB of models in the `ollama` volume) — see [Ollama: cloud, on the computer or in Docker](assistant.md#ollama-cloud-on-the-computer-or-in-docker) |
| Test database | in memory (`tmpfs`) |
| Backups | `scripts/backup.*` keeps the 7 most recent |

### Clean up

```powershell
.\scripts\clean.ps1          # build outputs, stopped containers, dangling images, build cache older than 7 days
.\scripts\clean.ps1 -Deps    # + node_modules and Python virtual environments
.\scripts\clean.ps1 -All     # + Docker volumes: database, attachments, Ollama models (data is lost)
docker system df             # what Docker uses
```

On macOS/Linux use `./scripts/clean.sh [--deps|--all]`.

## Rules for new code

1. Cache only data that is read often and changes rarely; never per-user data on the server.
2. Every cache has a maximum size and a lifetime; invalidate it after the change is **committed**.
3. Prefer HTTP validation (`ETag`, `Cache-Control`) for data that clients can keep.
4. Measure: the assistant exposes cache hits in `/api/assistant/health`; the backend's metrics are on `/actuator/prometheus`.
