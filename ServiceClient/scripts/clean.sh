#!/usr/bin/env sh
# Frees disk space used by this project (macOS, Linux, Git Bash).
#   ./scripts/clean.sh           build outputs + stopped containers + dangling images + old build cache
#   ./scripts/clean.sh --deps    also node_modules and Python virtual environments (re-created on next build)
#   ./scripts/clean.sh --all     --deps + the Docker volumes: database, attachments, Ollama models (~2 GB) — data is LOST
set -u
cd "$(dirname "$0")/.."
DEPS=0; ALL=0
for a in "$@"; do case "$a" in --deps) DEPS=1;; --all) ALL=1; DEPS=1;; esac; done

echo "== Disk used by Docker before cleaning"; docker system df || true
rm -rf backend/target frontend/.next frontend/playwright-report frontend/test-results frontend/tsconfig.tsbuildinfo \
       site docs/assets/maplenou documentation/*/*/site documentation/*/*/docs/assets/maplenou \
       assistant/.pytest_cache .cache
[ "$DEPS" = 1 ] && rm -rf frontend/node_modules .venv assistant/.venv
find . -name __pycache__ -type d -not -path '*/node_modules/*' -prune -exec rm -rf {} + 2>/dev/null

echo "== Docker: containers, dangling images, old build cache"
if [ "$ALL" = 1 ]; then V=--volumes; else V=; fi
docker compose --profile docs down $V --remove-orphans || true
docker compose -f docker-compose.test.yml down $V --remove-orphans || true
docker image prune --force || true
docker builder prune --force --filter "until=168h" || true
echo "== Disk used by Docker after cleaning"; docker system df || true
