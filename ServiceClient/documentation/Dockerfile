# syntax=docker/dockerfile:1.7
# Documentation sites (Zensical) served by nginx. Build context: the repository root (see /.dockerignore).
#   docker build -f documentation/Dockerfile -t maplenou-docs .                              # all six sites
#   docker build -f documentation/Dockerfile --build-arg DOCS_SITES=user -t maplenou-docs .  # public build
# The assistant reads the sites' search indexes from this server (ASSISTANT_INDEX_URL=http://docs).

# =============================================================================
# Stage 1 - Builder: build the static sites with the locked Zensical version
# =============================================================================

FROM python:3.12-slim-bookworm AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    UV_PYTHON_DOWNLOADS=never

WORKDIR /src

# Same uv version as the one that wrote uv.lock.
COPY --from=ghcr.io/astral-sh/uv:0.13.0 /uv /uvx /bin/

COPY pyproject.toml uv.lock ./

RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --locked --no-dev

COPY zensical.toml ./
COPY docs ./docs
COPY documentation ./documentation

ARG DOCS_SITES=user,staff,developer
ARG ASSISTANT_ENDPOINT=/api/assistant

RUN .venv/bin/python documentation/build.py \
        --sites "$DOCS_SITES" --endpoint "$ASSISTANT_ENDPOINT" --out /out --strict \
    # pre-compress text files once so nginx serves them without compressing on every request
    && find /out -type f \( -name '*.html' -o -name '*.css' -o -name '*.js' -o -name '*.json' -o -name '*.svg' -o -name '*.xml' \) \
        -size +1k -exec gzip -k -9 {} +

# =============================================================================
# Stage 2 - Runtime: nginx with only the generated files
# =============================================================================

FROM nginx:1.29-alpine AS runtime

# nginx.conf.template -> /etc/nginx/conf.d/default.conf at start-up, with only these two variables substituted
ENV NGINX_ENVSUBST_FILTER="^(ASSISTANT_UPSTREAM|DOCS_RESOLVER)$"
RUN rm -f /etc/nginx/conf.d/default.conf
COPY documentation/nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --chmod=0755 documentation/docker-entrypoint.d/15-docs-upstream.envsh /docker-entrypoint.d/15-docs-upstream.envsh
COPY --from=builder /out /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD ["wget", "-q", "-O", "/dev/null", "http://127.0.0.1/healthz"]
