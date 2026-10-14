# Demarre tout ce qu'il faut pour le backend Maplenou en local : Redis (Memurai) puis le serveur.
# Usage : ./start-all.ps1

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# 0. JDK 21+ requis (le projet est compile en class file version 65 = Java 21).
#    Sans ceci, mvnw prend le JDK par defaut du PATH, qui peut etre plus ancien.
$env:JAVA_HOME = "C:\Program Files\Java\jdk-24"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 1. Redis (Memurai) - indispensable, sinon login/register renvoient 403/500.
$redisUp = Get-NetTCPConnection -LocalPort 6379 -State Listen -ErrorAction SilentlyContinue
if (-not $redisUp) {
    Write-Host "Redis absent sur 6379, demarrage de Memurai..."
    Start-Process -FilePath "C:\Program Files\Memurai\memurai.exe" -ArgumentList "--port 6379" -WindowStyle Hidden
    Start-Sleep -Seconds 2
}

# 2. Secrets locaux (FedaPay, Cloudinary) - fichier non commite, a cote de ce script.
$envFile = Join-Path $root ".env.local.ps1"
if (Test-Path $envFile) {
    . $envFile
} else {
    Write-Host "Pas de .env.local.ps1 : FedaPay/Cloudinary tourneront en mode desactive/placeholder."
}

# 3. Backend Spring Boot.
Set-Location $root
& .\mvnw.cmd spring-boot:run
