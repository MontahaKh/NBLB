param(
  [int]$Port = 5500,
  [string]$Root = $PSScriptRoot,
  [string[]]$Prefixes
)

$Root = [System.IO.Path]::GetFullPath($Root)

$mime = @{
  ".html" = "text/html; charset=utf-8"
  ".htm"  = "text/html; charset=utf-8"
  ".css"  = "text/css; charset=utf-8"
  ".js"   = "application/javascript; charset=utf-8"
  ".json" = "application/json; charset=utf-8"
  ".png"  = "image/png"
  ".jpg"  = "image/jpeg"
  ".jpeg" = "image/jpeg"
  ".gif"  = "image/gif"
  ".svg"  = "image/svg+xml"
  ".ico"  = "image/x-icon"
  ".woff" = "font/woff"
  ".woff2"= "font/woff2"
  ".ttf"  = "font/ttf"
  ".map"  = "application/json; charset=utf-8"
}

$defaultPrefixes = @(
  "http://[::1]:$Port/",
  "http://localhost:$Port/",
  "http://127.0.0.1:$Port/"
)

$Prefixes = if ($Prefixes -and $Prefixes.Count -gt 0) { $Prefixes } else { $defaultPrefixes }

$listener = [System.Net.HttpListener]::new()

$activePrefixes = New-Object System.Collections.Generic.List[string]

foreach ($p in $Prefixes) {
  try {
    $listener.Prefixes.Add($p)
    $activePrefixes.Add($p)
  } catch {
    Write-Host "Warning: impossible d'ajouter le prefix '$p' ($($_.Exception.Message))" -ForegroundColor Yellow
  }
}

if ($activePrefixes.Count -eq 0) {
  Write-Host "Erreur: aucun prefix n'a pu être ajouté. Essaie PowerShell en admin (URLACL) ou change de port." -ForegroundColor Red
  exit 1
}

try {
  try {
    $listener.Start()
  } catch {
    Write-Host "Erreur: impossible de démarrer le serveur sur $($Prefixes -join ', ')." -ForegroundColor Red
    Write-Host "Détails: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Astuce: essaye un autre port (ex: -Port 5501) ou lance PowerShell en admin (URLACL)." -ForegroundColor Yellow
    throw
  }

  Write-Host "Serving '$Root' at:" -ForegroundColor Green
  foreach ($p in $activePrefixes) { Write-Host "  $p" -ForegroundColor Green }
  Write-Host "Press Ctrl+C to stop." -ForegroundColor DarkGray

  while ($listener.IsListening) {
    try {
      $context = $listener.GetContext()
    } catch {
      if ($listener.IsListening) {
        Write-Host "Warning: listener error ($($_.Exception.Message))" -ForegroundColor Yellow
        Start-Sleep -Milliseconds 200
        continue
      }
      break
    }
    try {
      $requestPath = [System.Uri]::UnescapeDataString($context.Request.Url.AbsolutePath)
      if ([string]::IsNullOrWhiteSpace($requestPath) -or $requestPath -eq "/") {
        $requestPath = "/index.html"
      }

      $relative = $requestPath.TrimStart("/") -replace "/", "\\"
      $candidate = [System.IO.Path]::GetFullPath((Join-Path $Root $relative))

      if (-not $candidate.StartsWith($Root, [System.StringComparison]::OrdinalIgnoreCase)) {
        $context.Response.StatusCode = 403
        $bytes = [System.Text.Encoding]::UTF8.GetBytes("Forbidden")
        $context.Response.ContentType = "text/plain; charset=utf-8"
        try { $context.Response.OutputStream.Write($bytes, 0, $bytes.Length) } catch {}
        continue
      }

      if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) {
        $context.Response.StatusCode = 404
        $bytes = [System.Text.Encoding]::UTF8.GetBytes("Not Found")
        $context.Response.ContentType = "text/plain; charset=utf-8"
        try { $context.Response.OutputStream.Write($bytes, 0, $bytes.Length) } catch {}
        continue
      }

      $ext = [System.IO.Path]::GetExtension($candidate).ToLowerInvariant()
      $contentType = $mime[$ext]
      if (-not $contentType) { $contentType = "application/octet-stream" }

      $bytes = [System.IO.File]::ReadAllBytes($candidate)
      $context.Response.StatusCode = 200
      $context.Response.ContentType = $contentType
      $context.Response.ContentLength64 = $bytes.Length
      try { $context.Response.OutputStream.Write($bytes, 0, $bytes.Length) } catch {}
    }
    finally {
      try { $context.Response.OutputStream.Close() } catch {}
      try { $context.Response.Close() } catch {}
    }
  }
}
finally {
  if ($listener) {
    try { $listener.Stop() } catch {}
    try { $listener.Close() } catch {}
  }
}
