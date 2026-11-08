# Frees disk space used by this project (Windows PowerShell).
#   .\scripts\clean.ps1            build outputs + stopped containers + dangling images + old build cache
#   .\scripts\clean.ps1 -Deps      also node_modules and Python virtual environments (re-created on next build)
#   .\scripts\clean.ps1 -All       -Deps + the Docker volumes: database, attachments, Ollama models (~2 GB) — data is LOST
# Nothing outside this repository and its Docker project is touched.
param([switch]$Deps, [switch]$All)
if ($All) { $Deps = $true }
$ErrorActionPreference = "Continue"
Set-Location (Join-Path $PSScriptRoot "..")

Write-Host "== Disk used by Docker before cleaning" -ForegroundColor Cyan
docker system df

$paths = @(
  "backend\target", "frontend\.next", "frontend\playwright-report", "frontend\test-results",
  "frontend\tsconfig.tsbuildinfo", "site", "docs\assets\maplenou", "assistant\.pytest_cache", ".cache"
)
$paths += Get-ChildItem documentation -Directory -Recurse -Depth 2 -ErrorAction SilentlyContinue |
  Where-Object { $_.Name -eq "site" -or $_.FullName -like "*\docs\assets\maplenou" } | ForEach-Object { $_.FullName }
if ($Deps) { $paths += @("frontend\node_modules", ".venv", "assistant\.venv") }
foreach ($p in $paths) {
  if (Test-Path $p) { Remove-Item -Recurse -Force $p; Write-Host "removed $p" }
}
Get-ChildItem -Recurse -Directory -Filter __pycache__ -ErrorAction SilentlyContinue |
  Where-Object { $_.FullName -notlike "*node_modules*" } | Remove-Item -Recurse -Force

Write-Host "== Docker: containers, dangling images, old build cache" -ForegroundColor Cyan
if ($All) {
  docker compose --profile docs down --volumes --remove-orphans
  docker compose -f docker-compose.test.yml down --volumes --remove-orphans
} else {
  docker compose --profile docs down --remove-orphans
  docker compose -f docker-compose.test.yml down --remove-orphans
}
docker image prune --force                       # images without a tag (left by rebuilds)
docker builder prune --force --filter "until=168h"   # build cache older than 7 days

Write-Host "== Disk used by Docker after cleaning" -ForegroundColor Cyan
docker system df
