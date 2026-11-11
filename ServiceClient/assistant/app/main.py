"""HTTP API of the documentation assistant.

    GET  /api/assistant/health           -> {"status", "ollama", "embeddings", "sites": {"user/fr": 42, ...}}
    POST /api/assistant/ask              {"question", "site", "lang"} -> {"answer", "references", "mode", ...}
    POST /api/assistant/ask?stream=1     same, as NDJSON events (references, token…, done)
    GET  /api/assistant/search?q=&site=&lang=&limit=  -> {"items": [{"title", "breadcrumb", "url", "excerpt"}]}
                                         (help-centre search, no language model)
    POST /api/assistant/internal/triage | /draft-reply   support backend only (X-Assistant-Token), see app/support.py

Run: uvicorn --factory app.main:create_app. Served behind the documentation nginx (same origin), so CORS is only needed for other origins.
"""

from __future__ import annotations

import asyncio
import hmac
import json
import logging
import time
from collections import defaultdict, deque
from contextlib import asynccontextmanager

from starlette.applications import Starlette
from starlette.middleware import Middleware
from starlette.middleware.cors import CORSMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse, StreamingResponse
from starlette.routing import Route

from .config import KNOWN_LANGS, Settings
from .ollama import Ollama
from . import support
from .service import Assistant

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("assistant")


class RateLimiter:
    """Sliding window per client address (in memory: one instance per container)."""

    def __init__(self, per_minute: int) -> None:
        self.per_minute = per_minute
        self.hits: dict[str, deque[float]] = defaultdict(deque)

    def allow(self, key: str, now: float | None = None) -> bool:
        now = time.monotonic() if now is None else now
        window = self.hits[key]
        while window and now - window[0] > 60:
            window.popleft()
        if len(window) >= self.per_minute:
            return False
        window.append(now)
        return True


def problem(status: int, code: str, detail: str) -> JSONResponse:
    return JSONResponse({"status": status, "code": code, "detail": detail}, status_code=status,
                        media_type="application/problem+json")


def create_app(settings: Settings | None = None, ollama: Ollama | None = None) -> Starlette:
    settings = settings or Settings()
    ollama = ollama or Ollama(settings.ollama_url, settings.chat_model, settings.embed_model,
                              settings.chat_timeout_seconds, api_key=settings.ollama_api_key, think=settings.think)
    assistant = Assistant(settings, ollama)
    limiter = RateLimiter(settings.rate_limit_per_minute)

    async def load_indexes() -> None:
        if not settings.index_url:
            return  # already loaded from ASSISTANT_INDEX_ROOT
        # The documentation server may start after the assistant: retry until every index is read.
        import httpx
        async with httpx.AsyncClient(timeout=10) as client:
            for attempt in range(180):
                if await assistant.load_from_url(client):
                    return
                await asyncio.sleep(10 if attempt < 30 else 60)

    async def pull_missing_models() -> None:
        for model in (m for m in (settings.embed_model, settings.chat_model) if m):
            if not ollama.has(await ollama.models(), model):
                log.info("Pulling %s into Ollama (first start only, can take several minutes)…", model)
                try:
                    await ollama.pull(model)
                    log.info("Model %s ready", model)
                except Exception as e:  # noqa: BLE001
                    log.warning("Could not pull %s: %s", model, e)

    async def warm_up() -> None:
        """Loads the indexes, then waits for Ollama as long as needed (it may be started later, e.g. the Ollama
        app on the computer): pulls the missing models once, then computes the embeddings. Meanwhile the
        assistant answers with search results only."""
        await load_indexes()
        cloud = bool(settings.ollama_api_key)
        log.info("Ollama expected at %s%s", settings.ollama_url, " (cloud, API key set)" if cloud else "")
        # models cannot be pulled into Ollama cloud: they are already there
        pulled = not settings.pull_models or cloud
        warned = False
        while True:
            if not await ollama.reachable():
                if not warned:
                    log.warning("Ollama is not reachable at %s: answers use search results only until it is "
                                "(see docs/guide/assistant.md, 'Troubleshooting')", settings.ollama_url)
                    warned = True
                await asyncio.sleep(30)
                continue
            if warned:
                log.info("Ollama is reachable")
                warned = False
            if not pulled:
                await pull_missing_models()
                pulled = True
            if not settings.embed_model:
                log.info("Embeddings disabled (ASSISTANT_EMBED_MODEL empty): lexical retrieval")
                return
            try:
                if await assistant.embed_all():
                    log.info("Embeddings ready")
                    return
            except Exception as e:  # noqa: BLE001 — keep serving lexical results whatever happens
                log.warning("embedding attempt failed: %s", e)
            await asyncio.sleep(20)

    @asynccontextmanager
    async def lifespan(app: Starlette):
        if not settings.index_url:
            assistant.load()
        task = asyncio.create_task(warm_up())
        yield
        task.cancel()
        await ollama.aclose()

    async def health(request: Request) -> JSONResponse:
        models = await ollama.models()
        return JSONResponse({
            "status": "ok",
            "ollama": bool(models),
            "ollamaCloud": bool(settings.ollama_api_key),
            "chatModel": settings.chat_model if ollama.has(models, settings.chat_model) else None,
            "embeddings": assistant.embeddings_ready,
            "indexReady": assistant.ready,
            "answerCache": {"entries": len(assistant.answers), "hits": assistant.answers.hits,
                            "misses": assistant.answers.misses},
            "sites": {f"{s}/{l}": len(i.sections) for (s, l), i in assistant.indexes.items()},
        })

    async def ask(request: Request):
        if not limiter.allow(client_of(request)):
            return problem(429, "RATE_LIMITED", "Too many questions; wait a minute.")
        try:
            body = await request.json()
        except (json.JSONDecodeError, UnicodeDecodeError):
            return problem(400, "MALFORMED_REQUEST", "JSON body expected.")
        if not isinstance(body, dict):
            return problem(400, "MALFORMED_REQUEST", "JSON object expected.")
        question = str(body.get("question", "")).strip()
        site = body.get("site")
        lang = body.get("lang", "fr")
        if not question:
            return problem(400, "QUESTION_REQUIRED", "question is required.")
        if len(question) > settings.max_question_chars:
            return problem(400, "QUESTION_TOO_LONG", f"question is limited to {settings.max_question_chars} characters.")
        if site not in settings.sites:
            # never reveal which internal sites exist on this instance
            return problem(404, "UNKNOWN_SITE", "Unknown documentation site.")
        if lang not in KNOWN_LANGS:
            return problem(400, "UNKNOWN_LANGUAGE", f"lang must be one of {KNOWN_LANGS}.")
        if not assistant.ready:
            return problem(503, "INDEX_LOADING", "The documentation index is loading; try again in a moment.")
        if request.query_params.get("stream") in ("1", "true"):
            async def events():
                async for event in assistant.ask_stream(question, site, lang):
                    yield json.dumps(event, ensure_ascii=False) + "\n"
            return StreamingResponse(events(), media_type="application/x-ndjson",
                                     headers={"Cache-Control": "no-store", "X-Accel-Buffering": "no"})
        answer = await assistant.ask(question, site, lang)
        return JSONResponse(answer.as_dict(), headers={"Cache-Control": "no-store"})

    def client_of(request: Request) -> str:
        return request.headers.get("x-real-ip") or (request.client.host if request.client else "unknown")

    async def search(request: Request) -> JSONResponse:
        if not limiter.allow(client_of(request)):
            return problem(429, "RATE_LIMITED", "Too many requests; wait a minute.")
        q = request.query_params.get("q", "").strip()
        site = request.query_params.get("site", "user")
        lang = request.query_params.get("lang", "fr")
        try:
            limit = max(1, min(20, int(request.query_params.get("limit", "8"))))
        except ValueError:
            return problem(400, "MALFORMED_REQUEST", "limit must be a number.")
        if not q:
            return problem(400, "QUESTION_REQUIRED", "q is required.")
        if len(q) > settings.max_question_chars:
            return problem(400, "QUESTION_TOO_LONG", f"q is limited to {settings.max_question_chars} characters.")
        if site not in settings.sites:
            return problem(404, "UNKNOWN_SITE", "Unknown documentation site.")
        if lang not in KNOWN_LANGS:
            return problem(400, "UNKNOWN_LANGUAGE", f"lang must be one of {KNOWN_LANGS}.")
        if not assistant.ready:
            return problem(503, "INDEX_LOADING", "The documentation index is loading; try again in a moment.")
        sections = (await assistant.search(q, site, lang, limit))
        refs = assistant.references(sections)
        return JSONResponse({"items": [{"title": r.title, "breadcrumb": r.breadcrumb, "url": r.url,
                                        "excerpt": r.excerpt} for r in refs]},
                            headers={"Cache-Control": "public, max-age=60"})

    def internal(handler):
        """Support-backend endpoints: absent without ASSISTANT_INTERNAL_TOKEN, 401 with a wrong token."""
        async def endpoint(request: Request) -> JSONResponse:
            if not settings.internal_token:
                return problem(404, "NOT_FOUND", "Not found.")
            given = request.headers.get("x-assistant-token", "")
            if not hmac.compare_digest(given.encode(), settings.internal_token.encode()):
                return problem(401, "UNAUTHENTICATED", "Service token required.")
            try:
                body = await request.json()
            except (json.JSONDecodeError, UnicodeDecodeError):
                return problem(400, "MALFORMED_REQUEST", "JSON body expected.")
            if not isinstance(body, dict):
                return problem(400, "MALFORMED_REQUEST", "JSON object expected.")
            try:
                return JSONResponse(await handler(assistant, body), headers={"Cache-Control": "no-store"})
            except support.Invalid as e:
                return problem(400, "MALFORMED_REQUEST", str(e))
            except support.Unavailable as e:
                log.info("AI help unavailable: %s", e)
                return problem(503, "AI_UNAVAILABLE", "The language model is not available.")
        return endpoint

    app = Starlette(
        routes=[
            Route("/api/assistant/health", health, methods=["GET"]),
            Route("/api/assistant/ask", ask, methods=["POST"]),
            Route("/api/assistant/search", search, methods=["GET"]),
            Route("/api/assistant/internal/triage", internal(support.triage), methods=["POST"]),
            Route("/api/assistant/internal/draft-reply", internal(support.draft_reply), methods=["POST"]),
        ],
        middleware=[Middleware(CORSMiddleware, allow_origins=list(settings.allowed_origins),
                               allow_methods=["GET", "POST"], allow_headers=["Content-Type"])],
        lifespan=lifespan,
    )
    app.state.assistant = assistant
    return app

