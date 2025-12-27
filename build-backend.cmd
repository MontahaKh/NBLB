@echo off
setlocal

REM Optional: set JAVA_HOME before running this script.
REM Example:
REM   set "JAVA_HOME=C:\jdk-17"
REM   set "PATH=%JAVA_HOME%\bin;%PATH%"

set ROOT=%~dp0

call :build discovery
call :build gateway
call :build auth-service
call :build order-service
call :build payment
echo.
echo Build termine.
exit /b 0

:build
set MOD=%~1
echo.
echo ===== Build %MOD% =====
pushd "%ROOT%%MOD%"
call mvnw.cmd -q -B clean package -DskipTests
if errorlevel 1 (
  popd
  echo Build echoue: %MOD%
  exit /b 1
)
popd
exit /b 0
