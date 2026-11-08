# syntax=docker/dockerfile:1.7
# Maplenou documentation assistant (Starlette + Ollama).
#   docker build -t maplenou-assistant assistant              # runtime image (≈ 70 MB of Python packages)
#   docker build --target test -t maplenou-assistant-test assistant && docker run --rm maplenou-assistant-test
# The indexes come from the documentation server (ASSISTANT_INDEX_URL) or a mounted folder (ASSISTANT_INDEX_ROOT).

# =============================================================================
# Stage 1 - Builder: resolve the locked dependencies into a virtual environment
# =============================================================================

# Pin the Debian release (bookworm): the plain `python:3.12-slim` tag moves to new Debian releases.
FROM python:3.12-slim-bookworm AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    UV_PYTHON_DOWNLOADS=never

WORKDIR /app

# Same uv version as the one that wrote uv.lock.
COPY --from=ghcr.io/astral-sh/uv:0.13.0 /uv /uvx /bin/

# Dependency files first: this layer is reused until pyproject.toml or uv.lock change.
COPY pyproject.toml uv.lock ./

# Runtime dependencies only. --locked fails the build if uv.lock is out of date.
# The uv cache is a BuildKit cache mount: it speeds up rebuilds and never ends up in an image layer.
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --locked --no-dev

COPY app ./app

# =============================================================================
# Stage 2 - Test: same environment + dev group (pytest), runs the unit tests
# =============================================================================

FROM builder AS test

RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --locked

COPY tests ./tests

CMD ["/app/.venv/bin/python", "-m", "pytest", "-q"]

# =============================================================================
# Stage 3 - Runtime: only the virtual environment and the code, non-root
# =============================================================================

FROM python:3.12-slim-bookworm AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/app/.venv/bin:$PATH" \
    ASSISTANT_CACHE_DIR=/var/cache/assistant

WORKDIR /app

RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin assistant \
    && mkdir -p /var/cache/assistant /srv/site \
    && chown assistant:assistant /var/cache/assistant

COPY --from=builder --chown=assistant:assistant /app/.venv ./.venv
COPY --from=builder --chown=assistant:assistant /app/app ./app

USER 10001

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["python", "-c", "import sys, urllib.request; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8000/api/assistant/health', timeout=4).status == 200 else 1)"]

CMD ["uvicorn", "--factory", "app.main:create_app", "--host", "0.0.0.0", "--port", "8000", "--proxy-headers", "--forwarded-allow-ips", "*"]
