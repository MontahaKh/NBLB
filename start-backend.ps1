param(
  [string]$JavaExe,
  [switch]$Minimized = $true,
  [switch]$SkipWait
)

$ErrorActionPreference = 'Stop'

function Resolve-JavaExe {
  param([string]$Explicit)

  if ($Explicit -and (Test-Path $Explicit)) {
    return (Resolve-Path $Explicit).Path
  }

  if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (Test-Path $candidate) {
      return (Resolve-Path $candidate).Path
    }
  }

  return 'java'
}

function Wait-Port {
  param(
    [string]$HostName,
    [int]$Port,
    [int]$TimeoutSeconds = 30
  )

  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      if (Test-NetConnection $HostName -Port $Port -InformationLevel Quiet) {
        return $true
      }
    } catch {
      # ignore transient errors
    }
    Start-Sleep -Milliseconds 500
  }
  return $false
}

function Start-Jar {
  param(
    [string]$Name,
    [string]$WorkingDirectory,
    [string]$JarRelativePath,
    [int]$Port
  )

  $jarPath = Join-Path $WorkingDirectory $JarRelativePath
  if (-not (Test-Path $jarPath)) {
    throw "[$Name] JAR introuvable: $jarPath (build d'abord avec mvnw package)"
  }

  $windowStyle = if ($Minimized) { 'Minimized' } else { 'Normal' }

  Write-Host "→ Start $Name ($jarPath)" -ForegroundColor Cyan
  Start-Process -FilePath $script:java -WorkingDirectory $WorkingDirectory -ArgumentList @('-jar', $JarRelativePath) -WindowStyle $windowStyle | Out-Null

  if (-not $SkipWait) {
    if (-not (Wait-Port -HostName 'localhost' -Port $Port -TimeoutSeconds 45)) {
      throw "[$Name] n'a pas ouvert le port $Port dans le délai imparti."
    }
    Write-Host "✓ $Name écoute sur $Port" -ForegroundColor Green
  }
}

$java = Resolve-JavaExe -Explicit $JavaExe
Write-Host "Java: $java" -ForegroundColor DarkGray

try {
  $versionText = (& $java -version 2>&1 | Out-String)
  if ($versionText -match 'version\s+"(?<maj>\d+)(\.|\s)') {
    $major = [int]$Matches.maj
    if ($major -lt 17) {
      Write-Host "Warning: detected Java $major. Spring Boot 3.5.x requires Java 17+." -ForegroundColor Yellow
      Write-Host "Set JAVA_HOME to a JDK 17+ (example: C:\\jdk-17) or pass -JavaExe." -ForegroundColor Yellow
    }
  }
} catch {
  # Ignore version check errors
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

# Ordre important: discovery (Eureka) -> gateway -> services
Start-Jar -Name 'discovery'     -WorkingDirectory (Join-Path $root 'discovery')     -JarRelativePath 'target\discovery-0.0.1-SNAPSHOT.jar' -Port 8761
Start-Jar -Name 'gateway'       -WorkingDirectory (Join-Path $root 'gateway')       -JarRelativePath 'target\gateway-0.0.1-SNAPSHOT.jar'   -Port 8222
Start-Jar -Name 'auth-service'  -WorkingDirectory (Join-Path $root 'auth-service')  -JarRelativePath 'target\auth-service-0.0.1-SNAPSHOT.jar' -Port 8090
Start-Jar -Name 'order-service' -WorkingDirectory (Join-Path $root 'order-service') -JarRelativePath 'target\order-service-0.0.1-SNAPSHOT.jar' -Port 8091
Start-Jar -Name 'payment'       -WorkingDirectory (Join-Path $root 'payment')       -JarRelativePath 'target\payment-0.0.1-SNAPSHOT.jar'   -Port 8092

Write-Host ''
Write-Host 'URLs:' -ForegroundColor Yellow
Write-Host '  Eureka : http://localhost:8761'
Write-Host '  Gateway: http://localhost:8222'
Write-Host '  Front  : http://localhost:5500/index.html'
