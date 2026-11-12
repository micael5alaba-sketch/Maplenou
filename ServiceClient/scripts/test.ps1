# Runs the test suites in Docker (Windows PowerShell) — no Java, Node or Python needed.
# Usage: .\scripts\test.ps1 [backend|frontend|assistant|docs|e2e|all]
param([string]$Suite = "all")
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
$t = "docker-compose.test.yml"
if (-not (Test-Path .env)) { Copy-Item .env.example .env; Write-Host "Created .env from .env.example" }

function Run($service) {
    Write-Host "`n=== $service ===" -ForegroundColor Cyan
    docker compose -f $t run --rm $service
    if ($LASTEXITCODE -ne 0) { throw "$service failed" }
}

if ($Suite -in @("backend", "all"))   { Run "backend-tests" }
if ($Suite -in @("frontend", "all"))  { Run "frontend-tests" }
if ($Suite -in @("assistant", "all")) { Run "assistant-tests" }
if ($Suite -in @("docs", "all")) {
    Write-Host "`n=== docs (strict build of the six sites) ===" -ForegroundColor Cyan
    docker compose --profile docs build docs
    if ($LASTEXITCODE -ne 0) { throw "docs build failed" }
}
if ($Suite -in @("e2e", "all")) {
    Write-Host "`n=== e2e (starts the full stack) ===" -ForegroundColor Cyan
    docker compose up -d --build --wait
    if ($LASTEXITCODE -ne 0) { throw "stack did not start" }
    docker compose -f docker-compose.yml -f $t run --rm e2e
    if ($LASTEXITCODE -ne 0) { throw "e2e failed" }
}
docker compose -f $t down --remove-orphans 2>$null | Out-Null
Write-Host "`nAll requested suites passed." -ForegroundColor Green
