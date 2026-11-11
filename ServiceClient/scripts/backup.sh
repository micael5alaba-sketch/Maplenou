#!/usr/bin/env sh
# Backs up the Docker stack's data into ./backups/<date>/ : PostgreSQL dump + attachments archive.
#   ./scripts/backup.sh                 create a backup (keeps the 7 most recent, deletes older ones)
#   ./scripts/backup.sh restore <dir>   restore a backup into the running stack (REPLACES current data)
set -eu
cd "$(dirname "$0")/.."
. ./.env 2>/dev/null || { echo "No .env file (copy .env.example)"; exit 1; }
KEEP=7
if [ "${1:-}" = restore ]; then
  DIR="${2:?usage: backup.sh restore backups/<date>}"
  docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
  gunzip -c "$DIR/database.sql.gz" | docker compose exec -T postgres psql -q -U "$POSTGRES_USER" -d "$POSTGRES_DB"
  docker compose cp "$DIR/attachments.tar.gz" backend:/tmp/attachments.tar.gz
  docker compose exec -T backend sh -c 'cd /data/attachments && tar -xzf /tmp/attachments.tar.gz && rm /tmp/attachments.tar.gz'
  docker compose restart backend
  echo "Restored $DIR"; exit 0
fi
DIR="backups/$(date +%Y-%m-%d_%H%M%S)"; mkdir -p "$DIR"
docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner | gzip -9 > "$DIR/database.sql.gz"
docker compose exec -T backend sh -c 'cd /data/attachments && tar -czf - .' > "$DIR/attachments.tar.gz"
echo "Backup written to $DIR ($(du -sh "$DIR" | cut -f1))"
# retention: only the most recent $KEEP backups are kept, so backups cannot fill the disk
ls -1d backups/*/ 2>/dev/null | sort | head -n -"$KEEP" | xargs -r rm -rf
