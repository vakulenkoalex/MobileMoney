# Запуск сервера с нуля

## 1. Установка PostgreSQL

### Windows (через installer)
1. Скачай: https://www.postgresql.org/download/windows/
2. Запусти installer
3. Пароль для postgres: `postgres`
4. Port: `5432`

### Windows (через chocolatey)
```powershell
choco install postgresql -y
```

### Запуск PostgreSQL
```powershell
# Запуск службы
Start-Service postgresql-x64-16

# Или через pg_ctl
"C:\Program Files\PostgreSQL\16\bin\pg_ctl" start -D "C:\Program Files\PostgreSQL\16\data"
```

## 2. Создание БД

```powershell
# Подключиться к PostgreSQL
psql -U postgres

# В psql выполнить:
CREATE DATABASE mobilemoney;
\q
```

## 3. Запуск сервера

```powershell
cd D:\Git\MobileMoney\server
buildServer.bat
```

## 4. Проверка

В браузере:
```
http://localhost:8080/api/v1/sync/register?deviceId=test&deviceName=test
```

Должно вернуть:
```json
{"token": "...", "deviceId": "test"}
```

## Если ошибка "port 8080 already used"

```powershell
# Найти процесс на порту 8080
netstat -ano | findstr :8080

# Убить процесс
taskkill /PID <номер> /F
```

## Если ошибка "database does not exist"

```powershell
psql -U postgres
CREATE DATABASE mobilemoney;
\q
```

## Если ошибка "connection refused"

Проверь PostgreSQL запущен:
```powershell
pg_isready
```