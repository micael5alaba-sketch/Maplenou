#!/usr/bin/env sh
# Runs the same checks as the GitHub Actions, with or without Docker, and prints one line per suite (DEC-CS12-05).
#
# Usage: ./scripts/check.sh [--docker|--native|--auto] [suite ...]
#   suites: backend frontend assistant docs e2e      (ServiceClient; "all" = these five, the default)
#           site bakend flutter                       (the other folders of the Maplenou repository)
#           ci                                        (everything a pull request to main runs)
#   --native  Java 21 / Node 22 / uv / Flutter on this computer (same commands as .github/workflows)
#   --docker  containers only (docker-compose.test.yml; nothing else installed)
#   --auto    native when the toolchain of a suite is installed, Docker otherwise (default)
# Unlike scripts/test.sh, every requested suite runs even when an earlier one fails; the exit code is 1 if any failed.
cd "$(dirname "$0")/.." || exit 2
ROOT=$(pwd)
REPO=$(cd .. && pwd)
MODE=auto
SUITES=""
for a in "$@"; do
  case "$a" in
    --docker) MODE=docker ;; --native) MODE=native ;; --auto) MODE=auto ;;
    -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
    all) SUITES="$SUITES backend frontend assistant docs e2e" ;;
    ci) SUITES="$SUITES backend frontend assistant docs e2e site bakend flutter" ;;
    backend|frontend|assistant|docs|e2e|site|bakend|flutter) SUITES="$SUITES $a" ;;
    *) echo "Unknown argument: $a (see --help)"; exit 2 ;;
  esac
done
[ -n "$SUITES" ] || SUITES="backend frontend assistant docs e2e"
[ -f .env ] || { cp .env.example .env; echo "Created .env from .env.example"; }

have() { command -v "$1" >/dev/null 2>&1; }
docker_ok() { have docker && docker info >/dev/null 2>&1; }
E2E_URL="${E2E_BASE_URL:-http://localhost:3001}"
RESULTS=""
FAILED=0

# native toolchain of a suite present?
native_ready() {
  case "$1" in
    backend) have java ;;
    frontend|e2e|site) have node && have npm ;;
    assistant|docs) have uv ;;
    bakend) have java ;;
    flutter) have flutter ;;
  esac
}

native() {
  case "$1" in
    backend)   (cd backend && chmod +x mvnw && ./mvnw -B verify) ;;
    frontend)  (cd frontend && npm ci --legacy-peer-deps && npm run lint && npm run typecheck && npm test && npm run build) ;;
    assistant) (cd assistant && uv sync --locked && uv run pytest -q) ;;
    docs)      uv sync --locked && uv run python documentation/build.py --strict ;;
    e2e)
      curl -fsS "$E2E_URL/login" -o /dev/null || { echo "No app on $E2E_URL: start it first (docker compose up -d --wait)"; return 1; }
      (cd frontend && npx playwright install chromium && E2E_BASE_URL="$E2E_URL" npx playwright test) ;;
    site)      [ -d "$REPO/Site_vitrine" ] || { echo "no ../Site_vitrine"; return 3; }
               (cd "$REPO/Site_vitrine" && npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build) ;;
    bakend)    [ -d "$REPO/Bakend" ] || { echo "no ../Bakend"; return 3; }
               # like backend-ci.yml: PostgreSQL (maplenou_db) on 5432 and Redis on 6379 must be running
               (cd "$REPO/Bakend" && chmod +x mvnw && ./mvnw -B --no-transfer-progress test) ;;
    flutter)   [ -d "$REPO/frontend" ] || { echo "no ../frontend"; return 3; }
               (cd "$REPO/frontend" && flutter pub get && flutter analyze && flutter test) ;;
  esac
}

in_docker() {
  T=docker-compose.test.yml
  case "$1" in
    backend)   docker compose -f "$T" run --rm backend-tests ;;
    frontend)  docker compose -f "$T" run --rm frontend-tests ;;
    assistant) docker compose -f "$T" run --rm assistant-tests ;;
    docs)      docker compose --profile docs build docs ;;
    e2e)       docker compose up -d --build --wait && docker compose -f docker-compose.yml -f "$T" run --rm e2e ;;
    site)      [ -d "$REPO/Site_vitrine" ] || { echo "no ../Site_vitrine"; return 3; }
               # node_modules in a named volume: nothing written into your folder but the build output
               docker run --rm -e NEXT_TELEMETRY_DISABLED=1 -v "$REPO/Site_vitrine:/app" \
                 -v maplenou-site-node-modules:/app/node_modules -w /app node:22-alpine \
                 sh -c "npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build" ;;
    bakend)    echo "Bakend needs its PostgreSQL and Redis: run it natively (--native) or with Bakend/start-all.ps1"; return 3 ;;
    flutter)   echo "No Flutter container here (image > 2 GB): install Flutter and use --native"; return 3 ;;
  esac
}

for s in $SUITES; do
  how=$MODE
  if [ "$how" = auto ]; then
    if native_ready "$s"; then how=native; elif docker_ok; then how=docker; else how=none; fi
  fi
  printf '\n=== %s (%s) ===\n' "$s" "$how"
  start=$(date +%s)
  if [ "$how" = native ]; then
    if native_ready "$s"; then native "$s"; rc=$?; else echo "toolchain missing for $s"; rc=3; fi
  elif [ "$how" = docker ]; then
    if docker_ok; then in_docker "$s"; rc=$?; else echo "Docker is not running"; rc=3; fi
  else
    echo "neither the toolchain nor Docker is available for $s"; rc=3
  fi
  took=$(( $(date +%s) - start ))
  case $rc in
    0) st=PASS ;; 3) st=SKIP ;; *) st=FAIL; FAILED=1 ;;
  esac
  RESULTS="$RESULTS$(printf '%-10s %-6s %-5s %ss' "$s" "$how" "$st" "$took")\n"
done
docker compose -f docker-compose.test.yml down --remove-orphans >/dev/null 2>&1 || true

printf '\n%-10s %-6s %-5s %s\n' SUITE MODE RESULT TIME
printf "$RESULTS"
[ $FAILED -eq 0 ] && printf '\nNo failure.\n' || printf '\nAt least one suite failed (see its output above).\n'
exit $FAILED
