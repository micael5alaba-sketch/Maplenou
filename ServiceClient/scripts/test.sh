#!/usr/bin/env sh
# Runs the test suites in Docker — no Java, Node or Python needed.
# Usage: ./scripts/test.sh [backend|frontend|assistant|docs|e2e|all]
set -e
cd "$(dirname "$0")/.."
SUITE="${1:-all}"
T=docker-compose.test.yml
[ -f .env ] || { cp .env.example .env; echo "Created .env from .env.example"; }
run() { printf '\n=== %s ===\n' "$1"; docker compose -f "$T" run --rm "$1"; }
case "$SUITE" in backend|all) run backend-tests;; esac
case "$SUITE" in frontend|all) run frontend-tests;; esac
case "$SUITE" in assistant|all) run assistant-tests;; esac
case "$SUITE" in docs|all)
  printf '\n=== docs (strict build of the six sites) ===\n'
  docker compose --profile docs build docs;;
esac
case "$SUITE" in e2e|all)
  printf '\n=== e2e (starts the full stack) ===\n'
  docker compose up -d --build --wait
  docker compose -f docker-compose.yml -f "$T" run --rm e2e;;
esac
docker compose -f "$T" down --remove-orphans >/dev/null 2>&1 || true
printf '\nAll requested suites passed.\n'
