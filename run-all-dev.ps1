# NBLB Full Stack Application Launcher
# Run all microservices and frontend in development mode (Maven, not containers)

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Load .env variables
$envFile = Join-Path $projectDir ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment variables from .env..." -ForegroundColor DarkGray
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -notmatch '^#' -and $line -match '=') {
            $name, $value = $line.Split('=', 2)
            $name = $name.Trim()
            $value = $value.Trim()
            # Remove quotes just in case
            $value = $value -replace '^"|"$', ''
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
    
    # Ensure GOOGLE_API_KEY is set for the Google GenAI SDK (it might default to this name)
    $geminiKey = [Environment]::GetEnvironmentVariable("GEMINI_API_KEY", "Process")
    if (-not [string]::IsNullOrEmpty($geminiKey)) {
        [Environment]::SetEnvironmentVariable("GOOGLE_API_KEY", $geminiKey, "Process")
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   NBLB Full Stack Application Launcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to start a service in a new window
function Start-Service {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [int]$Order,
        [int]$Port
    )
    
    Write-Host "[$Order/6] Starting $ServiceName on port $Port..." -ForegroundColor Yellow
    
    $fullPath = Join-Path $projectDir $ServicePath
    
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = "powershell.exe"
    $startInfo.Arguments = "-NoExit -Command `"cd '$fullPath'; .\mvnw.cmd spring-boot:run`""
    $startInfo.UseShellExecute = $true
    
    [System.Diagnostics.Process]::Start($startInfo) | Out-Null
    
    Start-Sleep -Seconds 2
}

# Start all services
Start-Service -ServiceName "Discovery Service" -ServicePath "discovery" -Order 1 -Port 8761
Start-Service -ServiceName "Auth Service" -ServicePath "auth-service" -Order 2 -Port 8090
Start-Service -ServiceName "Order Service" -ServicePath "order-service" -Order 3 -Port 8091
Start-Service -ServiceName "Payment Service" -ServicePath "payment" -Order 4 -Port 8092
Start-Service -ServiceName "Recommendation Service" -ServicePath "recommendation-service" -Order 5 -Port 8093
Start-Service -ServiceName "API Gateway" -ServicePath "gateway" -Order 6 -Port 8222

# Start Frontend
Write-Host "[7/7] Starting Frontend on port 5000..." -ForegroundColor Yellow
$frontendPath = Join-Path $projectDir "Front"

$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = "powershell.exe"
$startInfo.Arguments = "-NoExit -Command `"cd '$frontendPath'; .\serve.ps1`""
$startInfo.UseShellExecute = $true

[System.Diagnostics.Process]::Start($startInfo) | Out-Null

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "   All services are starting..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Services will be available at:" -ForegroundColor Cyan
Write-Host "   - Frontend:    http://localhost:5000" -ForegroundColor White
Write-Host "   - API Gateway: http://localhost:8222" -ForegroundColor White
Write-Host "   - Auth:        http://localhost:8090" -ForegroundColor White
Write-Host "   - Order:       http://localhost:8091" -ForegroundColor White
Write-Host "   - Payment:     http://localhost:8092" -ForegroundColor White
Write-Host "   - Recommendation: http://localhost:8093" -ForegroundColor White
Write-Host "   - Discovery:   http://localhost:8761" -ForegroundColor White
Write-Host ""
Write-Host "Close individual windows to stop each service." -ForegroundColor Yellow
Write-Host ""
