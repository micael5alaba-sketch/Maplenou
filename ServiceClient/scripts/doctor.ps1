# Diagnoses why the Docker stack (or its documentation assistant / Ollama) does not start — Windows PowerShell.
# Usage: .\scripts\doctor.ps1            Read-only: changes nothing, prints what is wrong and how to fix it.
# Paste its output when you ask for help.
$ErrorActionPreference = "Continue"
Set-Location (Join-Path $PSScriptRoot "..")
function Ok($m)   { Write-Host "  OK    $m" -ForegroundColor Green }
function Bad($m)  { Write-Host "  FAIL  $m" -ForegroundColor Red; $script:failures++ }
function Info($m) { Write-Host "  ..    $m" }
$failures = 0

Write-Host "== Docker" -ForegroundColor Cyan
docker info --format "{{.ServerVersion}}" 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) { Bad "Docker is not running: start Docker Desktop and wait until it says 'Engine running'."; exit 1 }
Ok ("Docker engine " + (docker info --format "{{.ServerVersion}}") + ", " + (docker compose version --short))
$mem = [math]::Round((docker info --format "{{.MemTotal}}") / 1GB, 1)
if ($mem -lt 6) { Info "Docker has $mem GB of memory: the language model needs about 4 GB (Docker Desktop > Settings > Resources)." } else { Ok "Docker memory $mem GB" }

Write-Host "== .env" -ForegroundColor Cyan
if (-not (Test-Path .env)) { Bad ".env is missing: run  Copy-Item .env.example .env"; exit 1 }
$envVars = @{}
Get-Content .env | ForEach-Object { if ($_ -match '^\s*([A-Z0-9_]+)=(.*)$') { $envVars[$Matches[1]] = $Matches[2].Trim() } }
$expected = Get-Content .env.example | Where-Object { $_ -match '^([A-Z0-9_]+)=' } | ForEach-Object { ($_ -split '=')[0] }
$missing = $expected | Where-Object { -not $envVars.ContainsKey($_) }
if ($missing) { Bad ("variables missing from .env: " + ($missing -join ", ") + " - run scripts\env-sync.ps1 (adds them with their example value)") } else { Ok ".env has every variable of .env.example" }
docker compose --profile docs --profile ollama config -q 2>&1 | ForEach-Object { Bad "$_" }
if ($LASTEXITCODE -eq 0) { Ok "docker compose configuration is valid" }

Write-Host "== Settings that go together" -ForegroundColor Cyan
function V($n) { if ($envVars.ContainsKey($n)) { $envVars[$n] } else { "" } }
if ((V "ANTIVIRUS") -eq "clamav") { Info "ANTIVIRUS=clamav: start with --profile antivirus (uploads are refused while ClamAV loads its signatures)" }
if ((V "ATTACHMENT_STORAGE") -eq "cloudinary" -and (V "CLOUDINARY_URL") -notmatch '^cloudinary://[^:]+:[^@]+@.+') { Bad "ATTACHMENT_STORAGE=cloudinary needs CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>" }
if ((V "AI_ASSIST_ENABLED") -eq "true") {
  if ((V "ASSISTANT_INTERNAL_TOKEN").Length -ge 24) { Ok "AI help in tickets: token set (start with --profile docs)" } else { Bad "AI_ASSIST_ENABLED=true needs ASSISTANT_INTERNAL_TOKEN (24+ random characters)" }
}
if ((V "MAPLENOU_PUSH_RELAY") -eq "true" -and -not (V "SUPPORT_SERVICE_TOKEN")) { Bad "MAPLENOU_PUSH_RELAY=true needs SUPPORT_SERVICE_TOKEN (same value as in Bakend)" }
if ((V "ECOSYSTEM_MODE") -eq "http" -and -not (V "SUPPORT_SERVICE_TOKEN")) { Bad "ECOSYSTEM_MODE=http needs SUPPORT_SERVICE_TOKEN (same value as in Bakend)" }
$mt = V "METRICS_TOKEN"; if ($mt -and $mt.Length -lt 24) { Bad "METRICS_TOKEN is too short (24+ random characters)" }
if ((",$(V 'COMPOSE_PROFILES'),") -match ',public,' -or ((V "SUPPORT_DOMAIN") -and (V "SUPPORT_DOMAIN") -ne "support.example.com")) {
  Info "public deployment checks (SUPPORT_DOMAIN=$(V 'SUPPORT_DOMAIN'))"
  if ((V "DOCS_SITES") -eq "user" -and (V "ASSISTANT_SITES") -eq "user") { Ok "only the user guide is published" } else { Bad "public deployment: set DOCS_SITES=user and ASSISTANT_SITES=user" }
  if ((V "SPRING_PROFILES_ACTIVE") -match "demo") { Bad "public deployment: SPRING_PROFILES_ACTIVE=demo must never be public" }
  if ((V "JWT_SECRET") -like "demo-only*") { Bad "public deployment: change JWT_SECRET" }
  if ((V "POSTGRES_PASSWORD") -eq "maplenou") { Bad "public deployment: change POSTGRES_PASSWORD" }
}

Write-Host "== Host ports" -ForegroundColor Cyan
$ours = (docker compose --profile docs --profile ollama ps --format "{{.Ports}}" 2>$null) -join " "
foreach ($name in "FRONTEND_HOST_PORT", "BACKEND_HOST_PORT", "DOCS_HOST_PORT") {
  $port = $envVars[$name]
  if (-not $port) { continue }
  $used = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
  if (-not $used) { Ok "$name=$port is free" }
  elseif ($ours -match ":$port->") { Ok "$name=$port is used by this project's container" }
  else {
    $proc = ($used | Select-Object -First 1 | ForEach-Object { (Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue).ProcessName })
    Bad "$name=$port is taken by '$proc': choose another port in .env (e.g. $name=$([int]$port + 6))"
  }
}

Write-Host "== Ollama" -ForegroundColor Cyan
$ollamaUrl = $envVars["OLLAMA_URL"]
Info "OLLAMA_URL=$ollamaUrl"
if ($ollamaUrl -match "host\.docker\.internal") {
  try {
    $tags = Invoke-RestMethod "http://localhost:11434/api/tags" -TimeoutSec 5
    Ok ("Ollama on this computer answers; models: " + (($tags.models | ForEach-Object { $_.name }) -join ", "))
    foreach ($m in $envVars["ASSISTANT_CHAT_MODEL"], $envVars["ASSISTANT_EMBED_MODEL"]) {
      if (-not ($tags.models.name -contains $m -or $tags.models.name -contains "${m}:latest")) {
        Info "model $m is not installed yet: the assistant downloads it on start, or run  ollama pull $m"
      }
    }
  } catch {
    Bad "Ollama is not running on this computer: start the Ollama app (or run 'ollama serve'), or use the container (OLLAMA_URL=http://ollama:11434 and --profile ollama)."
  }
} elseif ($ollamaUrl -match "ollama\.com") {
  $key = $envVars["OLLAMA_API_KEY"]
  if (-not $key) { Bad "OLLAMA_API_KEY is empty: create a key on https://ollama.com/settings/keys and paste it in .env" }
  else {
    try {
      $tags = Invoke-RestMethod "https://ollama.com/api/tags" -Headers @{ Authorization = "Bearer $key" } -TimeoutSec 10
      $model = $envVars["ASSISTANT_CHAT_MODEL"]
      if ($tags.models.name -contains $model) { Ok "Ollama cloud answers and offers $model" }
      else { Bad "Ollama cloud answers but has no model '$model': choose one listed on https://ollama.com/api/tags" }
    } catch { Bad "Ollama cloud is not reachable from this computer (internet connection?)" }
  }
} elseif ($ollamaUrl -match "//ollama:") {
  Info "container mode: start with  docker compose --profile docs --profile ollama up -d --build --wait"
} else { Info "custom Ollama URL" }

Write-Host "== Maplenou platform (Bakend)" -ForegroundColor Cyan
if ($envVars["MAPLENOU_LOGIN_ENABLED"] -eq "true" -or $envVars["ECOSYSTEM_MODE"] -eq "http") {
  try {
    $h = Invoke-RestMethod "http://localhost:8080/actuator/health" -TimeoutSec 5
    Ok ("Bakend answers on http://localhost:8080 (" + $h.status + ")")
  } catch { Bad "Bakend is not running on port 8080: start it with  cd Bakend; ./start-all.ps1" }
  if ($envVars["ECOSYSTEM_MODE"] -eq "http" -and -not $envVars["SUPPORT_SERVICE_TOKEN"]) {
    Bad "ECOSYSTEM_MODE=http needs SUPPORT_SERVICE_TOKEN (same value in Bakend's environment)"
  }
  if ($envVars["MAPLENOU_LOGIN_ENABLED"] -eq "true" -and $envVars["AUTH_MODE"] -ne "maplenou") {
    Info "MAPLENOU_LOGIN_ENABLED=true but AUTH_MODE is not maplenou: the sign-in page still shows the demo personas"
  }
} else { Info "demo mode: not connected to Bakend (MAPLENOU_LOGIN_ENABLED=false, ECOSYSTEM_MODE=demo)" }

Write-Host "== Containers" -ForegroundColor Cyan
$rows = docker compose --profile docs --profile ollama ps -a --format "{{.Service}}|{{.State}}|{{.Health}}" 2>$null
if (-not $rows) { Info "no container yet: run  docker compose --profile docs up -d --build --wait" }
foreach ($row in $rows) {
  $service, $state, $health = $row -split '\|'
  if ($state -eq "running" -and ($health -eq "" -or $health -eq "healthy")) { Ok "$service $state $health" }
  else {
    Bad "$service is $state $health — last log lines:"
    docker compose --profile docs --profile ollama logs --tail 25 $service 2>&1 | ForEach-Object { Write-Host "        $_" }
  }
}
$docsPort = $envVars["DOCS_HOST_PORT"]
if ($docsPort) {
  try {
    $h = Invoke-RestMethod "http://localhost:$docsPort/api/assistant/health" -TimeoutSec 5
    Ok ("assistant: ollama=" + $h.ollama + ", chatModel=" + $h.chatModel + ", embeddings=" + $h.embeddings + ", indexReady=" + $h.indexReady)
    if (-not $h.ollama) { Info "the assistant cannot reach Ollama: see docs/guide/assistant.md, 'Troubleshooting'" }
  } catch { Info "assistant not reachable on http://localhost:$docsPort (docs profile not started?)" }
}

Write-Host ""
if ($failures -eq 0) { Write-Host "No problem found." -ForegroundColor Green } else { Write-Host "$failures problem(s) found: fix them from the top." -ForegroundColor Yellow; exit 1 }
