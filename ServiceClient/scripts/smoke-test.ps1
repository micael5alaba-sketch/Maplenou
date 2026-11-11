# End-to-end smoke test of a RUNNING stack (Docker or native) — Windows PowerShell, no extra tools.
# Usage: .\scripts\smoke-test.ps1 [-Api http://localhost:8081] [-App http://localhost:3001] [-Docs http://localhost:8006]
# Requires the demo profile (demo login). Stops at the first failure.
param([string]$Api = "http://localhost:8081", [string]$App = "http://localhost:3001", [string]$Docs = "http://localhost:8006")
$ErrorActionPreference = "Stop"
$jean = "11111111-1111-4111-8111-111111111111"; $sophie = "44444444-4444-4444-8444-444444444444"
function Ok($m) { Write-Host "  OK   $m" -ForegroundColor Green }
function Token($id) { (Invoke-RestMethod -Method Post "$Api/api/v1/dev/token" -ContentType "application/json" -Body (@{ userId = $id } | ConvertTo-Json)).accessToken }
function Status($uri, $headers) { try { (Invoke-WebRequest $uri -Headers $headers -UseBasicParsing).StatusCode } catch { [int]$_.Exception.Response.StatusCode } }

Write-Host "== Backend $Api" -ForegroundColor Cyan
if ((Invoke-RestMethod "$Api/actuator/health").status -ne "UP") { throw "backend not healthy" }; Ok "health UP"
$tj = Token $jean; $ts = Token $sophie; Ok "demo tokens (is SPRING_PROFILES_ACTIVE=demo?)"
$hj = @{ Authorization = "Bearer $tj" }; $hs = @{ Authorization = "Bearer $ts" }
$r = Invoke-WebRequest "$Api/api/v1/support/categories" -Headers $hj -UseBasicParsing
$etag = $r.Headers["ETag"]; Ok "categories with ETag $etag"
if ((Status "$Api/api/v1/support/categories" ($hj + @{ "If-None-Match" = "$etag" })) -ne 304) { throw "expected 304" }; Ok "cached categories revalidated (304)"
$body = @{ subject = "Smoke test"; description = "Created by scripts/smoke-test.ps1"; categoryCode = "OTHER"; priority = "LOW" } | ConvertTo-Json
$t = Invoke-RestMethod -Method Post "$Api/api/v1/support/tickets" -Headers $hj -ContentType "application/json" -Body $body; Ok "ticket created ($($t.ticketNumber))"
Invoke-RestMethod -Method Put "$Api/api/v1/support/tickets/$($t.id)/assign" -Headers $hs -ContentType "application/json" -Body "{}" | Out-Null; Ok "agent assigned"
Invoke-RestMethod -Method Post "$Api/api/v1/support/tickets/$($t.id)/messages" -Headers $hs -ContentType "application/json; charset=utf-8" -Body '{"body":"Reponse du test","clientMessageId":"smoke-1"}' | Out-Null; Ok "agent replied"
$s = Invoke-RestMethod -Method Put "$Api/api/v1/support/tickets/$($t.id)/status" -Headers $hs -ContentType "application/json" -Body '{"status":"RESOLVED","comment":"smoke"}'
if ($s.status -ne "RESOLVED") { throw "resolve failed" }; Ok "ticket resolved"
if ((Status "$Api/api/v1/support/dashboard" $hj) -ne 403) { throw "expected 403" }; Ok "customer denied the back-office (403)"
$p = Invoke-RestMethod "$Api/api/v1/support/notifications/push-status" -Headers $hj; Ok "push channel: $($p.mode) (firebase = FIREBASE_CREDENTIALS_FILE loaded)"
$v = Invoke-RestMethod "$Api/api/v1/support/vocabulary" -Headers $hj
if (-not ($v.statuses | Where-Object { $_.kind -eq "WAITING_REQUESTER" })) { throw "vocabulary" }; Ok "configurable vocabulary"
Invoke-RestMethod -Method Post "$Api/api/v1/support/tickets/$($t.id)/satisfaction" -Headers $hj -ContentType "application/json" -Body '{"rating":5}' | Out-Null; Ok "satisfaction rating"

Write-Host "== Web app $App" -ForegroundColor Cyan
Invoke-WebRequest "$App/login" -UseBasicParsing | Out-Null; Ok "login page"
$en = Invoke-WebRequest "$App/login" -Headers @{ Cookie = "maplenou_locale=en" } -UseBasicParsing
if ($en.Content -notmatch 'lang="en"') { throw "language switch" }; Ok "English rendering (cookie)"

try { Invoke-WebRequest "$Docs/healthz" -UseBasicParsing | Out-Null; $docsUp = $true } catch { $docsUp = $false }
if ($docsUp) {
  Write-Host "== Documentation $Docs" -ForegroundColor Cyan
  Invoke-WebRequest "$Docs/user/fr/search.json" -UseBasicParsing | Out-Null; Ok "user/fr"
  foreach ($site in "user/en", "staff/fr", "staff/en", "developer/fr", "developer/en") {  # internal sites only if DOCS_SITES has them
    try { Invoke-WebRequest "$Docs/$site/search.json" -UseBasicParsing | Out-Null; Ok $site } catch { Write-Host "  --   $site not built (DOCS_SITES)" }
  }
  try { Invoke-RestMethod "$Docs/api/assistant/health" | ConvertTo-Json -Compress; Ok "assistant health" } catch { Write-Host "  (assistant still starting or not running)" }
  try { Invoke-WebRequest -Method Post "$Docs/api/assistant/internal/triage" -ContentType "application/json" -Body "{}" -UseBasicParsing | Out-Null; throw "internal AI endpoints are exposed" }
  catch { if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw } ; Ok "internal AI endpoints not exposed by the docs server" }
} else { Write-Host "== Documentation not running (start it with: docker compose --profile docs up -d)" }
Write-Host "Smoke test passed." -ForegroundColor Green
