param(
  [string]$JavaHome = "",
  [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'

function Resolve-JavaHome {
  param([string]$Explicit)

  if ($Explicit) {
    $candidate = [System.IO.Path]::GetFullPath($Explicit)
    if (Test-Path (Join-Path $candidate 'bin\java.exe')) { return $candidate }
    throw "JAVA_HOME invalide: '$Explicit' (java.exe introuvable)"
  }

  if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    return $env:JAVA_HOME
  }

  $fallback = 'C:\jdk-17'
  if (Test-Path (Join-Path $fallback 'bin\java.exe')) {
    return $fallback
  }

  return ""
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path

$resolvedJavaHome = Resolve-JavaHome -Explicit $JavaHome
if ($resolvedJavaHome) {
  $env:JAVA_HOME = $resolvedJavaHome
  $env:PATH = "$resolvedJavaHome\bin;" + $env:PATH
  Write-Host "JAVA_HOME=$resolvedJavaHome" -ForegroundColor DarkGray
} else {
  Write-Host "Warning: JAVA_HOME not set. Make sure you are using Java 17+ (Spring Boot 3.5.x)." -ForegroundColor Yellow
}

$modules = @(
  'discovery',
  'gateway',
  'auth-service',
  'order-service',
  'payment'
)

$extraArgs = @()
if ($SkipTests) { $extraArgs += '-DskipTests' }

foreach ($m in $modules) {
  $moduleDir = Join-Path $root $m
  $wrapper = Join-Path $moduleDir 'mvnw.cmd'
  if (-not (Test-Path $wrapper)) {
    throw "Wrapper Maven introuvable: $wrapper"
  }

  Write-Host "Build $m" -ForegroundColor Cyan
  Push-Location $moduleDir
  try {
    & $wrapper -q -B clean package @extraArgs
  } finally {
    Pop-Location
  }
}

Write-Host "Build complete" -ForegroundColor Green
