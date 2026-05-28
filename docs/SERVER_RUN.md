# Запуск сервера

## 1. Запуск

```powershell
.\server\buildServer.bat run
```

Сервер запускается из корня проекта.

Сервер создаст базу данных SQLite при первом запуске: `data/sync.db`

## 2. Проверка

### Браузер

```
http://localhost:6080/
```

Должно вернуть:

```json
{"status":"ok","database":true}
```

### PowerShell

```powershell
Invoke-RestMethod http://localhost:6080/
```
