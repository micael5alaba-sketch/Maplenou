# Runs the same checks as the GitHub Actions, with or without Docker, and prints one line per suite (DEC-CS12-05).
#
# Usage: .\scripts\check.ps1 [-Mode auto|native|docker] [suite ...]
#   suites: backend frontend assistant docs e2e      (ServiceClient; "all" = these five, the default)
#           site bakend flutter                       (the other folders of the Maplenou repository)
#           ci                                        (everything a pull request to main runs)
#   -Mode native  Java 21 / Node 22 / uv / Flutter on this computer (same commands as .github/workflows)
#   -Mode docker  containers only (docker-compose.test.yml; nothing else installed)
#   -Mode auto    native when the toolchain of a suite is installed, Docker otherwise (default)
# Every requested suite runs even when an earlier one fails; the exit code is 1 if any failed.
param(
    [ValidateSet("auto", "native", "docker")] [string] $Mode = "auto",
    [Parameter(ValueFromRemainingArguments = $true)] [string[]] $Suite
)
$ErrorActionPreference = "Continue"
Set-Location (Join-Path $PSScriptRoot "..")
$Root = (Get-Location).Path
$Repo = Split-Path $Root -Parent
$E2eUrl = if ($env:E2E_BASE_URL) { $env:E2E_BASE_URL } else { "http://localhost:3001" }

$all = @("backend", "frontend", "assistant", "docs", "e2e")
$list = @()
if (-not $Suite -or $Suite.Count -eq 0) { $list = $all }
foreach ($s in $Suite) {
    switch ($s) {
        "all" { $list += $all }
        "ci" { $list += $all + @("site", "bakend", "flutter") }
        { $_ -in @("backend", "frontend", "assistant", "docs", "e2e", "site", "bakend", "flutter") } { $list += $s }
        default { Write-Host "Unknown suite: $s"; exit 2 }
    }
}
if (-not (Test-Path .env)) { Copy-Item .env.example .env; Write-Host "Created .env from .env.example" }

function Have($cmd) { [bool](Get-Command $cmd -ErrorAction SilentlyContinue) }
function DockerOk { if (-not (Have docker)) { return $false }; docker info *> $null; return ($LASTEXITCODE -eq 0) }
function NativeReady($s) {
    switch ($s) {
        "backend" { Have java }
        "bakend" { Have java }
        { $_ -in @("frontend", "e2e", "site") } { (Have node) -and (Have npm) }
        { $_ -in @("assistant", "docs") } { Have uv }
        "flutter" { Have flutter }
    }
}
# runs the commands one after the other, stops at the first failure; returns 0 / 1
function Steps([string] $dir, [string[]] $cmds) {
    Push-Location $dir
    try {
        foreach ($c in $cmds) {
            Write-Host "> $c"
            Invoke-Expression $c | Out-Host
            if ($LASTEXITCODE -ne 0) { return 1 }
        }
        return 0
    } finally { Pop-Location }
}
function Native($s) {
    switch ($s) {
        "backend" { return Steps "backend" @(".\mvnw.cmd -B verify") }
        "frontend" { return Steps "frontend" @("npm ci --legacy-peer-deps", "npm run lint", "npm run typecheck", "npm test", "npm run build") }
        "assistant" { return Steps "assistant" @("uv sync --locked", "uv run pytest -q") }
        "docs" { return Steps "." @("uv sync --locked", "uv run python documentation/build.py --strict") }
        "e2e" {
            try { Invoke-WebRequest -UseBasicParsing "$E2eUrl/login" -TimeoutSec 10 | Out-Null }
            catch { Write-Host "No app on ${E2eUrl}: start it first (docker compose up -d --wait)"; return 1 }
            $env:E2E_BASE_URL = $E2eUrl
            return Steps "frontend" @("npx playwright install chromium", "npx playwright test")
        }
        "site" {
            if (-not (Test-Path "$Repo\Site_vitrine")) { Write-Host "no ..\Site_vitrine"; return 3 }
            return Steps "$Repo\Site_vitrine" @("npm ci --no-audit --no-fund", "npm run lint", "npm run typecheck", "npm test", "npm run build")
        }
        "bakend" {
            if (-not (Test-Path "$Repo\Bakend")) { Write-Host "no ..\Bakend"; return 3 }
            # like backend-ci.yml: PostgreSQL (maplenou_db) on 5432 and Redis on 6379 must be running
            return Steps "$Repo\Bakend" @(".\mvnw.cmd -B --no-transfer-progress test")
        }
        "flutter" {
            if (-not (Test-Path "$Repo\frontend")) { Write-Host "no ..\frontend"; return 3 }
            return Steps "$Repo\frontend" @("flutter pub get", "flutter analyze", "flutter test")
        }
    }
}
function InDocker($s) {
    $t = "docker-compose.test.yml"
    switch ($s) {
        "backend" { return Steps "." @("docker compose -f $t run --rm backend-tests") }
        "frontend" { return Steps "." @("docker compose -f $t run --rm frontend-tests") }
        "assistant" { return Steps "." @("docker compose -f $t run --rm assistant-tests") }
        "docs" { return Steps "." @("docker compose --profile docs build docs") }
        "e2e" { return Steps "." @("docker compose up -d --build --wait", "docker compose -f docker-compose.yml -f $t run --rm e2e") }
        "site" {
            if (-not (Test-Path "$Repo\Site_vitrine")) { Write-Host "no ..\Site_vitrine"; return 3 }
            # node_modules in a named volume: nothing written into your folder but the build output
            return Steps "." @("docker run --rm -e NEXT_TELEMETRY_DISABLED=1 -v `"$Repo\Site_vitrine:/app`" -v maplenou-site-node-modules:/app/node_modules -w /app node:22-alpine sh -c `"npm ci --no-audit --no-fund && npm run lint && npm run typecheck && npm test && npm run build`"")
        }
        "bakend" { Write-Host "Bakend needs its PostgreSQL and Redis: use -Mode native or Bakend\start-all.ps1"; return 3 }
        "flutter" { Write-Host "No Flutter container here (image > 2 GB): install Flutter and use -Mode native"; return 3 }
    }
}

$results = @()
$failed = $false
foreach ($s in $list) {
    $how = $Mode
    if ($how -eq "auto") { $how = if (NativeReady $s) { "native" } elseif (DockerOk) { "docker" } else { "none" } }
    Write-Host "`n=== $s ($how) ==="
    $watch = [Diagnostics.Stopwatch]::StartNew()
    $rc = 3
    if ($how -eq "native") { if (NativeReady $s) { $rc = Native $s } else { Write-Host "toolchain missing for $s" } }
    elseif ($how -eq "docker") { if (DockerOk) { $rc = InDocker $s } else { Write-Host "Docker is not running" } }
    else { Write-Host "neither the toolchain nor Docker is available for $s" }
    $rc = @($rc)[-1]   # last value written by the function
    $state = switch ($rc) { 0 { "PASS" } 3 { "SKIP" } default { "FAIL" } }
    if ($state -eq "FAIL") { $failed = $true }
    $results += [pscustomobject]@{ Suite = $s; Mode = $how; Result = $state; Seconds = [int]$watch.Elapsed.TotalSeconds }
}
if (Have docker) { docker compose -f docker-compose.test.yml down --remove-orphans *> $null }

$results | Format-Table -AutoSize
if ($failed) { Write-Host "At least one suite failed (see its output above)."; exit 1 }
Write-Host "No failure."
exit 0
