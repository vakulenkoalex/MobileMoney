@echo off
set "JAVA_HOME="
set "DB_URL=jdbc:postgresql://localhost:5432/mobilemoney"
set "DB_USER=postgres"
set "DB_PASS="
set "NETTY_PORT=8080"

gradlew.bat run