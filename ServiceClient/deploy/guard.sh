#!/bin/sh
# Started instead of Caddy's own entrypoint: a public deployment must never publish the internal guides, an
# assistant that knows them, or the demo profile (demo login and fake data).
set -eu
fail() { echo "public profile refused: $1 (see docs/guide/deployment.md)" >&2; exit 1; }
[ "${DOCS_SITES:-}" = "user" ] || fail "DOCS_SITES must be 'user' (staff and developer guides are internal)"
[ "${ASSISTANT_SITES:-}" = "user" ] || fail "ASSISTANT_SITES must be 'user'"
case ",${SPRING_PROFILES_ACTIVE:-}," in *,demo,*) fail "SPRING_PROFILES_ACTIVE=demo must never be public" ;; esac
[ "${AUTH_MODE:-}" = "maplenou" ] || fail "AUTH_MODE must be 'maplenou' (sign-in with Maplenou accounts)"
[ "${SESSION_COOKIE_SECURE:-}" = "true" ] || fail "SESSION_COOKIE_SECURE must be 'true' behind HTTPS"
exec caddy run --config /etc/caddy/Caddyfile --adapter caddyfile
