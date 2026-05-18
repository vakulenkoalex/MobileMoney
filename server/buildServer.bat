@echo off
cd /d %~dp0
cd ..
if "%1"=="run" (
    gradlew.bat :server:run --no-daemon
) else (
    gradlew.bat :server:%*
)