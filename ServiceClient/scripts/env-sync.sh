#!/usr/bin/env bash
# Adds to .env the variables of .env.example it does not have yet, with their example value.
# Existing lines (passwords, keys, your ports) are never changed. Run it after each update of the project.
#   scripts/env-sync.sh            (Windows: scripts\env-sync.ps1)
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || { cp .env.example .env; echo "created .env from .env.example"; exit 0; }
added=0
while IFS= read -r line; do
  [[ "$line" =~ ^([A-Z0-9_]+)= ]] || continue
  key="${BASH_REMATCH[1]}"
  if ! grep -qE "^${key}=" .env; then
    [ "$added" -eq 0 ] && printf '\n# ---- added by scripts/env-sync.sh on %s (values from .env.example)\n' "$(date +%F)" >> .env
    printf '%s\n' "$line" >> .env
    echo "added $key"
    added=$((added + 1))
  fi
done < .env.example
echo "$added variable(s) added to .env"
