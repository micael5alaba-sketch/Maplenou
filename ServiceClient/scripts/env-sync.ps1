# Adds to .env the variables of .env.example it does not have yet, with their example value.
# Existing lines (passwords, keys, your ports) are never changed. Run it after each update of the project.
#   powershell -ExecutionPolicy Bypass -File scripts\env-sync.ps1
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Test-Path .env)) { Copy-Item .env.example .env; Write-Host "created .env from .env.example"; exit 0 }
$existing = @{}
foreach ($l in Get-Content .env) { if ($l -match "^([A-Z0-9_]+)=") { $existing[$Matches[1]] = $true } }
$added = @()
foreach ($l in Get-Content .env.example) {
  if ($l -match "^([A-Z0-9_]+)=" -and -not $existing.ContainsKey($Matches[1])) { $added += $l; Write-Host "added $($Matches[1])" }
}
if ($added.Count -gt 0) {
  $header = "", "# ---- added by scripts/env-sync.ps1 on $(Get-Date -Format yyyy-MM-dd) (values from .env.example)"
  Add-Content -Path .env -Value ($header + $added) -Encoding utf8
}
Write-Host "$($added.Count) variable(s) added to .env"
