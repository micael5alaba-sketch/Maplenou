#!/usr/bin/env sh
# End-to-end smoke test of a RUNNING stack (Docker or native), with curl only.
# Usage: ./scripts/smoke-test.sh [api-url] [app-url] [docs-url]
#   defaults: http://localhost:8081 http://localhost:3001 http://localhost:8006 (docs checked only if reachable)
# Requires the demo profile (demo login). Exit code 0 = everything answered as expected.
set -eu
API="${1:-http://localhost:8081}"; APP="${2:-http://localhost:3001}"; DOCS="${3:-http://localhost:8006}"
JEAN=11111111-1111-4111-8111-111111111111; SOPHIE=44444444-4444-4444-8444-444444444444
ok() { printf '  \033[32mOK\033[0m  %s\n' "$1"; }
fail() { printf '  \033[31mFAIL\033[0m %s\n' "$1"; exit 1; }
json() { python3 -c "import sys,json; print(json.load(sys.stdin)$1)" 2>/dev/null || sed -n "s/.*\"${2}\":\"\([^\"]*\)\".*/\1/p"; }

echo "== Backend $API"
curl -fsS "$API/actuator/health" | grep -q '"UP"' && ok "health UP" || fail "backend health"
TJ=$(curl -fsS -X POST "$API/api/v1/dev/token" -H 'Content-Type: application/json' -d "{\"userId\":\"$JEAN\"}" | json "['accessToken']" accessToken)
[ -n "$TJ" ] && ok "demo token (customer)" || fail "demo login (is SPRING_PROFILES_ACTIVE=demo?)"
TS=$(curl -fsS -X POST "$API/api/v1/dev/token" -H 'Content-Type: application/json' -d "{\"userId\":\"$SOPHIE\"}" | json "['accessToken']" accessToken)
ETAG=$(curl -fsS -D - -o /dev/null "$API/api/v1/support/categories" -H "Authorization: Bearer $TJ" | tr -d '\r' | sed -n 's/^[Ee][Tt]ag: //p')
[ -n "$ETAG" ] && ok "categories with ETag $ETAG" || fail "categories"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$API/api/v1/support/categories" -H "Authorization: Bearer $TJ" -H "If-None-Match: $ETAG")
[ "$CODE" = 304 ] && ok "cached categories revalidated (304)" || fail "expected 304, got $CODE"
ID=$(curl -fsS -X POST "$API/api/v1/support/tickets" -H "Authorization: Bearer $TJ" -H 'Content-Type: application/json' \
  -d '{"subject":"Smoke test","description":"Created by scripts/smoke-test.sh","categoryCode":"OTHER","priority":"LOW"}' | json "['id']" id)
[ -n "$ID" ] && ok "ticket created ($ID)" || fail "create ticket"
curl -fsS -X PUT "$API/api/v1/support/tickets/$ID/assign" -H "Authorization: Bearer $TS" -H 'Content-Type: application/json' -d '{}' >/dev/null && ok "agent assigned"
curl -fsS -X POST "$API/api/v1/support/tickets/$ID/messages" -H "Authorization: Bearer $TS" -H 'Content-Type: application/json' \
  -d '{"body":"Réponse du test","clientMessageId":"smoke-1"}' >/dev/null && ok "agent replied"
curl -fsS -X PUT "$API/api/v1/support/tickets/$ID/status" -H "Authorization: Bearer $TS" -H 'Content-Type: application/json' \
  -d '{"status":"RESOLVED","comment":"smoke"}' | grep -q '"RESOLVED"' && ok "ticket resolved" || fail "resolve"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$API/api/v1/support/dashboard" -H "Authorization: Bearer $TJ")
[ "$CODE" = 403 ] && ok "customer denied the back-office (403)" || fail "expected 403, got $CODE"
MODE=$(curl -fsS "$API/api/v1/support/notifications/push-status" -H "Authorization: Bearer $TJ" | json "['mode']" mode)
[ -n "$MODE" ] && ok "push channel: $MODE (firebase = FIREBASE_CREDENTIALS_FILE loaded)" || fail "push-status"
curl -fsS "$API/api/v1/support/vocabulary" -H "Authorization: Bearer $TJ" | grep -q '"WAITING_REQUESTER"' && ok "configurable vocabulary" || fail "vocabulary"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/api/v1/support/tickets/$ID/satisfaction" -H "Authorization: Bearer $TJ" \
  -H 'Content-Type: application/json' -d '{"rating":5}')
[ "$CODE" = 201 ] && ok "satisfaction rating" || fail "rating, got $CODE"

echo "== Web app $APP"
curl -fsS "$APP/login" -o /dev/null && ok "login page" || fail "frontend"
curl -fsS -H 'Cookie: maplenou_locale=en' "$APP/login" | grep -q 'lang="en"' && ok "English rendering (cookie)" || fail "language switch"

if curl -fsS "$DOCS/healthz" -o /dev/null 2>/dev/null; then
  echo "== Documentation $DOCS"
  curl -fsS "$DOCS/user/fr/search.json" -o /dev/null && ok "user/fr" || fail "user/fr (the user guide is always built)"
  for s in user/en staff/fr staff/en developer/fr developer/en; do  # internal sites exist only if DOCS_SITES has them
    curl -fsS "$DOCS/$s/search.json" -o /dev/null 2>/dev/null && ok "$s" || echo "  --   $s not built (DOCS_SITES)"
  done
  curl -fsS "$DOCS/api/assistant/health" && echo && ok "assistant health" || echo "  (assistant still starting or not running)"
  CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$DOCS/api/assistant/internal/triage" -H 'Content-Type: application/json' -d '{}')
  [ "$CODE" = 404 ] && ok "internal AI endpoints not exposed by the docs server" || fail "internal endpoints answered $CODE"
else
  echo "== Documentation not running (start it with: docker compose --profile docs up -d)"
fi
echo "Smoke test passed."
