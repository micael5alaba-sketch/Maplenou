#!/usr/bin/env bash
# Diagnoses why the Docker stack (or its documentation assistant / Ollama) does not start — Linux, macOS, Git Bash.
# Usage: scripts/doctor.sh       Read-only: changes nothing, prints what is wrong and how to fix it.
cd "$(dirname "$0")/.."
G=$'\e[32m'; R=$'\e[31m'; C=$'\e[36m'; N=$'\e[0m'; failures=0
ok()   { echo "  ${G}OK${N}    $*"; }
bad()  { echo "  ${R}FAIL${N}  $*"; failures=$((failures + 1)); }
info() { echo "  ..    $*"; }
compose() { docker compose --profile docs --profile ollama "$@"; }

echo "${C}== Docker${N}"
docker info >/dev/null 2>&1 || { bad "Docker is not running: start Docker (Desktop) first."; exit 1; }
ok "Docker engine $(docker info --format '{{.ServerVersion}}'), compose $(docker compose version --short)"

echo "${C}== .env${N}"
[ -f .env ] || { bad ".env is missing: run  cp .env.example .env"; exit 1; }
missing=$(comm -23 <(grep -oE '^[A-Z0-9_]+=' .env.example | sort -u) <(grep -oE '^[A-Z0-9_]+=' .env | sort -u) | tr -d '=' | xargs)
[ -z "$missing" ] && ok ".env has every variable of .env.example" || bad "variables missing from .env: $missing — run scripts/env-sync.sh (adds them with their example value)"
if out=$(compose config -q 2>&1); then ok "docker compose configuration is valid"; else bad "$out"; fi
val() { grep -E "^$1=" .env | tail -1 | cut -d= -f2- | tr -d '\r'; }

echo "${C}== Settings that go together${N}"
[ "$(val ANTIVIRUS)" = clamav ] && info "ANTIVIRUS=clamav: start with --profile antivirus (uploads are refused while ClamAV loads its signatures)"
if [ "$(val ATTACHMENT_STORAGE)" = cloudinary ]; then
  [[ "$(val CLOUDINARY_URL)" == cloudinary://*:*@* ]] && ok "Cloudinary storage configured" || bad "ATTACHMENT_STORAGE=cloudinary needs CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>"
fi
if [ "$(val AI_ASSIST_ENABLED)" = true ]; then
  t=$(val ASSISTANT_INTERNAL_TOKEN)
  [ "${#t}" -ge 24 ] && ok "AI help in tickets: token set (start with --profile docs)" || bad "AI_ASSIST_ENABLED=true needs ASSISTANT_INTERNAL_TOKEN (24+ random characters)"
fi
[ "$(val MAPLENOU_PUSH_RELAY)" = true ] && [ -z "$(val SUPPORT_SERVICE_TOKEN)" ] && bad "MAPLENOU_PUSH_RELAY=true needs SUPPORT_SERVICE_TOKEN (same value as in Bakend)"
[ "$(val ECOSYSTEM_MODE)" = http ] && [ -z "$(val SUPPORT_SERVICE_TOKEN)" ] && bad "ECOSYSTEM_MODE=http needs SUPPORT_SERVICE_TOKEN (same value as in Bakend)"
m=$(val METRICS_TOKEN); [ -n "$m" ] && [ "${#m}" -lt 24 ] && bad "METRICS_TOKEN is too short (24+ random characters)"
if [[ ",$(val COMPOSE_PROFILES)," == *,public,* ]] || [ "$(val SUPPORT_DOMAIN)" != support.example.com ]; then
  info "public deployment checks (SUPPORT_DOMAIN=$(val SUPPORT_DOMAIN))"
  [ "$(val DOCS_SITES)" = user ] && [ "$(val ASSISTANT_SITES)" = user ] && ok "only the user guide is published" || bad "public deployment: set DOCS_SITES=user and ASSISTANT_SITES=user"
  [[ "$(val SPRING_PROFILES_ACTIVE)" == *demo* ]] && bad "public deployment: SPRING_PROFILES_ACTIVE=demo must never be public"
  [[ "$(val JWT_SECRET)" == demo-only* ]] && bad "public deployment: change JWT_SECRET"
  [ "$(val POSTGRES_PASSWORD)" = maplenou ] && bad "public deployment: change POSTGRES_PASSWORD"
  [ "$(val FORWARD_HEADERS_STRATEGY)" = native ] || info "behind Caddy, FORWARD_HEADERS_STRATEGY=native gives the real client address to the API"
fi

echo "${C}== Host ports${N}"
ours=$(compose ps --format '{{.Ports}}' 2>/dev/null | tr '\n' ' ')
for name in FRONTEND_HOST_PORT BACKEND_HOST_PORT DOCS_HOST_PORT; do
  port=$(val "$name"); [ -n "$port" ] || continue
  if ! (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then ok "$name=$port is free"
  elif [[ "$ours" == *":$port->"* ]]; then ok "$name=$port is used by this project's container"
  else bad "$name=$port is taken by another program: choose another port in .env"; fi
done

echo "${C}== Ollama${N}"
url=$(val OLLAMA_URL); info "OLLAMA_URL=$url"
if [[ "$url" == *host.docker.internal* ]]; then
  if tags=$(curl -fsS --max-time 5 http://localhost:11434/api/tags 2>/dev/null); then
    ok "Ollama on this computer answers: $(echo "$tags" | grep -oE '"name":"[^"]+"' | cut -d'"' -f4 | xargs)"
  else
    bad "Ollama is not running on this computer: start it (ollama serve), or use the container (OLLAMA_URL=http://ollama:11434 and --profile ollama)."
  fi
  [[ "$(uname -s)" == Linux ]] && info "Linux: Ollama must listen on the Docker bridge (OLLAMA_HOST=0.0.0.0) to be reached from a container."
elif [[ "$url" == *ollama.com* ]]; then
  key=$(val OLLAMA_API_KEY); model=$(val ASSISTANT_CHAT_MODEL)
  if [ -z "$key" ]; then bad "OLLAMA_API_KEY is empty: create a key on https://ollama.com/settings/keys and paste it in .env"
  elif tags=$(curl -fsS --max-time 10 -H "Authorization: Bearer $key" https://ollama.com/api/tags 2>/dev/null); then
    if echo "$tags" | grep -q "\"name\":\"$model\""; then ok "Ollama cloud answers and offers $model"
    else bad "Ollama cloud has no model '$model': choose one listed on https://ollama.com/api/tags"; fi
  else bad "Ollama cloud is not reachable from this computer (internet connection?)"; fi
elif [[ "$url" == *//ollama:* ]]; then info "container mode: docker compose --profile docs --profile ollama up -d --build --wait"; fi

echo "${C}== Maplenou platform (Bakend)${N}"
if [ "$(val MAPLENOU_LOGIN_ENABLED)" = true ] || [ "$(val ECOSYSTEM_MODE)" = http ]; then
  if curl -fsS --max-time 5 http://localhost:8080/actuator/health >/dev/null 2>&1; then ok "Bakend answers on http://localhost:8080"
  else bad "Bakend is not running on port 8080: start it (cd Bakend && ./mvnw spring-boot:run)"; fi
  if [ "$(val ECOSYSTEM_MODE)" = http ] && [ -z "$(val SUPPORT_SERVICE_TOKEN)" ]; then
    bad "ECOSYSTEM_MODE=http needs SUPPORT_SERVICE_TOKEN (same value in Bakend's environment)"; fi
else info "demo mode: not connected to Bakend"; fi

echo "${C}== Containers${N}"
rows=$(compose ps -a --format '{{.Service}}|{{.State}}|{{.Health}}' 2>/dev/null)
[ -z "$rows" ] && info "no container yet: run  docker compose --profile docs up -d --build --wait"
while IFS='|' read -r service state health; do
  [ -n "$service" ] || continue
  if [ "$state" = running ] && { [ -z "$health" ] || [ "$health" = healthy ]; }; then ok "$service $state $health"
  else bad "$service is $state $health — last log lines:"; compose logs --tail 25 "$service" 2>&1 | sed 's/^/        /'; fi
done <<< "$rows"
port=$(val DOCS_HOST_PORT)
if h=$(curl -fsS --max-time 5 "http://localhost:$port/api/assistant/health" 2>/dev/null); then ok "assistant: $h"
else info "assistant not reachable on http://localhost:$port (docs profile not started?)"; fi

echo
if [ "$failures" -eq 0 ]; then echo "${G}No problem found.${N}"; else echo "$failures problem(s) found: fix them from the top."; exit 1; fi
