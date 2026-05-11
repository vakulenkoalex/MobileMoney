# Запуск сервера с нуля

## 1. Запуск сервера

```powershell
cd D:\Git\MobileMoney\server
buildServer.bat run
```

Сервер создаст базу данных SQLite при первом запуске: `data/sync.db`

## 2. Проверка

В браузере (базовая проверка):
```
http://localhost:6080/
```

Должно вернуть:
```json
{"status":"ok","database":true}
```