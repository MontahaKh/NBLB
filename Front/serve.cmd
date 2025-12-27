@echo off
setlocal
REM Start the static frontend server even when PowerShell script execution is restricted.
cd /d "%~dp0"
echo.
echo Open this in your browser:
echo   http://127.0.0.1:5500/index.html
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0serve.ps1" -Port 5500
