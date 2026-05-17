# API Endpoints — Сценарии использования

## Endpoints Overview

| Method | Path | Auth | Описание |
|--------|------|------|---------|
| POST | `/api/v1/auth/login` | нет | Авторизация |
| POST | `/api/v1/sync/push` | Bearer | Отправка локальных изменений |
| GET | `/api/v1/sync/changes?since=` | Bearer | Изменения с timestamp |
| GET | `/api/v1/sync/pull` | Bearer | Полный дамп данных |
| GET | `/` | нет | Health check |

---

## 1. Авторизация

**Trigger:** пользователь нажал "Войти" на экране логина

```
Клиент                    Сервер                     БД
  │                         │                        │
  │──── login() ──────────▶│ POST /api/v1/auth/login │
  │   {login,password,     │                        │
  │    device_id,name}      │                        │
  │                         │────── SELECT users ───▶│
  │                         │◀───── user record ──────│
  │                         │──── INSERT device ─────▶│
  │                         │                        │
  │◀─── {token,login} ──────│                        │
  │                         │                        │
```

**Request:**
```json
{
  "login": "user@example.com",
  "password": "secret",
  "device_id": "android_id_xxx",
  "device_name": "Pixel 8"
}
```

**Response (200):**
```json
{
  "token": "uuid-token",
  "login": "user@example.com"
}
```

**Error handling:**
- 400 → Missing login or password
- 404 → User not found
- 401 → Invalid password

---

## 2. Push изменений

**Trigger:**
- Ручная синхронизация (пользователь нажал "Синхронизировать")
- Фоновая синхронизация (WorkManager, каждые 15 мин)

**Prerequisite:** токен сохранён в EncryptedSharedPreferences

```
SyncWorker            SyncRepository           Сервер                     БД
    │                    │                     │                        │
    │ sync()             │                     │                        │
    │───────────────────▶│                     │                        │
    │                    │                     │                        │
    │                    │ pushChanges() ─────▶│ POST /api/v1/sync/push │
    │                    │   {accounts:[...],  │                        │
    │                    │    categories:[...], │                        │
    │                    │    transactions:[...]}│                       │
    │                    │                     │──── upsert accounts ────▶│
    │                    │                     │──── upsert categories ──▶│
    │                    │                     │──── upsert transactions─▶│
    │                    │                     │                        │
    ├─200 ───────────────│ {success,timestamp,  │                        │
    │   обновить timestamp│   synced:N}         │                        │
    │                    │                     │                        │
    ├─401 ───────── logout() ◀─│              │                        │
    │   показать экран логина  │              │                        │
    │                         │                │                        │
    └─500 ───────── retry later ◀─│           │                        │
        (exponential backoff)    │            │                        │
```

**Request:**
```json
{
  "accounts": [...],
  "categories": [...],
  "transactions": [...]
}
```

**Response (200):**
```json
{
  "success": true,
  "timestamp": 1700000000000,
  "synced": 15
}
```

**Error handling:**
- 200 → OK, обновить lastSyncTimestamp
- 401 → logout(), показать экран логина
- 500 → retry via WorkManager (exponential backoff, max 3 attempts)

**Conflict resolution:** last-write-wins по времени получения на сервере
- Сервер всегда записывает данные при push (без проверки updatedAt)
- Последний push перезаписывает предыдущие данные

### Выборка изменений на клиенте

Клиент выбирает записи для отправки по принципу: `syncedAt IS NULL` — не синхронизированные.

Flow:
1. Пользователь создаёт/изменяет запись
2. Запись сохраняется в Room с syncedAt = null
3. При sync() выбираются все записи WHERE syncedAt IS NULL
4. Отправляются на сервер
5. При успехе (200) обновляется syncedAt = serverTimestamp
6. При следующей синхронизации запись не попадает в выборку
```

---

## 3. Get Changes (инкрементальная синхронизация)

**Trigger:** pull данных с момента последней синхронизации

```
Клиент                    Сервер                     БД
  │                         │                        │
  │──── getChanges(since) ─▶│ GET /api/v1/sync/changes?since=... │
  │   ?since=1700000000000   │   changes?since=...     │
  │                         │──── SELECT WHERE ──────▶│
   │                         │   serverReceivedAt > since │
  │                         │                        │
  │◀── {timestamp,         ◀│                        │
  │     accounts,categories,│                        │
  │     transactions} ───────│                        │
```

**Request params:**
- `since` — timestamp в миллисекундах (serverReceivedAt)

**Response (200):**
```json
{
  "timestamp": 1700000000000,
  "accounts": [...],
  "categories": [...],
  "transactions": [...]
}
```

---

## 4. Pull All (полный дамп)

**Trigger:** первая синхронизация нового устройства (full sync)

```
Клиент                    Сервер                     БД
  │                         │                        │
  │──── pullAll() ─────────▶│ GET /api/v1/sync/pull │
  │                         │──── SELECT WHERE ─────▶│
  │                         │   deleted_at IS NULL   │
  │                         │                        │
  │◀── {timestamp,          ◀│                        │
  │     accounts:[...],     │                        │
  │     categories:[...],    │                        │
  │     transactions:[...]}──│                        │
```

## 5. Health Check

**Trigger:** проверка доступности сервера

```
Клиент                    Сервер
  │                         │
  │──── ping() ────────────▶│ GET /
  │                         │
  │◀── {"status":"ok",      │
  │      "database":true} ───│
```

---
## 6. Семантика полей timestamp

| Поле | Кто пишет | Когда | На сервер? | Смысл |
|------|-----------|-------|------------|-------|
| createdAt | клиент | при создании | да | дата создания записи |
| updatedAt | клиент | при каждом изменении | да | last-write-wins конфликт resolution |
| deletedAt | клиент | при soft-delete | да | запись считается удалённой |
| syncedAt | клиент | после успешного push (server timestamp) | **нет** | не отправлять повторно синхронизированные |
| serverReceivedAt | сервер | при получении записи | **нет** | инкрементальный sync (параметр `since`) |

**Клиент отправляет:** createdAt, updatedAt, deletedAt
**Клиент получает:** server timestamp в ответе push → записывает в syncedAt
**Сервер хранит:** createdAt, updatedAt, deletedAt, serverReceivedAt

---

## 7. Sync на клиенте

```
полный цикл синхронизации

1. Проверка токена (deviceToken)
   → Нет токена → ошибка "Войдите в аккаунт"

2. pullChanges() — получить изменения с сервера
   ├─ lastSyncTimestamp == 0 ИЛИ нет локальных данных
   │   → GET /api/v1/sync/pull (полный дамп)
   └─ иначе
       → GET /api/v1/sync/changes?since=... (инкрементальный)

   Условия сохранения данных с сервера в Room:
   | existing == null         | insert (записи нет) |
   | existing.syncedAt != null| insert (сервер свежее)|
   | existing.syncedAt == null | пропуск (локальные не отправлены) |

3. pushChanges() — отправить локальные изменения
   ├─ SELECT * FROM [table] WHERE syncedAt IS NULL
   └─ POST /api/v1/sync/push
       → При успехе: UPDATE syncedAt = serverTimestamp
```
