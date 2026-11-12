# syntax=docker/dockerfile:1.7
# Maplenou Customer Service web app (Next.js standalone output).
#   docker build -t maplenou-frontend frontend

# =============================================================================
# Stage 1 - Dependencies: installed once per package-lock.json
# =============================================================================

FROM node:22-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./

# The npm cache is a BuildKit cache mount: faster rebuilds, nothing left in the image.
RUN --mount=type=cache,target=/root/.npm \
    npm ci --legacy-peer-deps --no-audit --no-fund

# =============================================================================
# Stage 2 - Builder: production build
# =============================================================================

FROM node:22-alpine AS builder

WORKDIR /app

ENV NEXT_TELEMETRY_DISABLED=1

COPY --from=deps /app/node_modules ./node_modules
COPY . .

RUN --mount=type=cache,target=/app/.next/cache \
    npm run build

# =============================================================================
# Stage 3 - Runtime: only the standalone server (no node_modules tree, no sources), non-root
# =============================================================================

FROM node:22-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

RUN addgroup -S -g 10001 app && adduser -S -D -H -u 10001 -G app app

COPY --from=builder --chown=app:app /app/.next/standalone ./
COPY --from=builder --chown=app:app /app/.next/static ./.next/static
COPY --from=builder --chown=app:app /app/public ./public

USER 10001

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD ["node", "-e", "fetch('http://127.0.0.1:3000/login').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"]

CMD ["node", "server.js"]
