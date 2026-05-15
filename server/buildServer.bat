@echo off
if "%1"=="run" (
    .\gradlew.bat run --no-daemon
) else (
    .\gradlew.bat %*
)