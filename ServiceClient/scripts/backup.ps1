# Backs up the Docker stack's data into .\backups\<date>\ : PostgreSQL dump + attachments archive (Windows).
#   .\scripts\backup.ps1                          create a backup (keeps the 7 most recent, deletes older ones)
#   .\scripts\backup.ps1 -Restore backups\<date>  restore a backup into the running stack (REPLACES current data)
param([string]$Restore = "")
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Test-Path .env)) { throw "No .env file (copy .env.example)" }
$cfg = @{}; Get-Content .env | Where-Object { $_ -match '^\s*([A-Z_]+)=(.*)$' } | ForEach-Object { $cfg[$Matches[1]] = $Matches[2] }
$user = $cfg["POSTGRES_USER"]; $db = $cfg["POSTGRES_DB"]; $keep = 7
if ($Restore) {
  docker compose exec -T postgres psql -U $user -d $db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
  docker compose cp "$Restore\database.sql.gz" postgres:/tmp/database.sql.gz
  docker compose exec -T postgres sh -c "gunzip -c /tmp/database.sql.gz | psql -q -U $user -d $db && rm /tmp/database.sql.gz"
  docker compose cp "$Restore\attachments.tar.gz" backend:/tmp/attachments.tar.gz
  docker compose exec -T backend sh -c 'cd /data/attachments && tar -xzf /tmp/attachments.tar.gz && rm /tmp/attachments.tar.gz'
  docker compose restart backend
  Write-Host "Restored $Restore"; exit 0
}
$dir = "backups\" + (Get-Date -Format "yyyy-MM-dd_HHmmss"); New-Item -ItemType Directory -Force $dir | Out-Null
# dumps are written inside the containers, then copied out (no PowerShell re-encoding of binary data)
docker compose exec -T postgres sh -c "pg_dump -U $user -d $db --no-owner | gzip -9 > /tmp/database.sql.gz"
docker compose cp postgres:/tmp/database.sql.gz "$dir\database.sql.gz"
docker compose exec -T postgres rm /tmp/database.sql.gz
docker compose exec -T backend sh -c 'cd /data/attachments && tar -czf /tmp/attachments.tar.gz .'
docker compose cp backend:/tmp/attachments.tar.gz "$dir\attachments.tar.gz"
docker compose exec -T backend rm /tmp/attachments.tar.gz
Write-Host "Backup written to $dir"
# retention: only the most recent $keep backups are kept, so backups cannot fill the disk
Get-ChildItem backups -Directory | Sort-Object Name -Descending | Select-Object -Skip $keep | Remove-Item -Recurse -Force
