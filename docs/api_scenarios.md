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
    │                    │   {accounts:[...],   │                        │
    │                    │    categories:[...], │                        │
    │                    │    transactions:[...]}│                       │
    │                    │                     │──── upsert accounts ──▶│
    │                    │                     │──── upsert categories ─▶│
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
  "accounts": [
    {
      "id": "uuid",
      "name": "Наличные",
      "typeId": "cash",
      "currencyCode": "RUB",
      "icon": "account-balance-wallet",
      "isDefault": true,
      "createdAt": 1700000000000,
      "updatedAt": 1700000000000,
      "deletedAt": null
    }
  ],
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

**Conflict resolution:** last-write-wins по `updatedAt`
- Сервер сохраняет запись только если `incoming.updatedAt > existing.updatedAt`
- Иначе пропускает

---

## 3. Get Changes (инкрементальная синхронизация)

**Trigger:** pull данных с момента последней синхронизации

**NOTE:** В текущей реализации `pullChanges()` отключён (`// pullChanges() disabled`). Сценарий зарезервирован для будущего.

```
Клиент                    Сервер                     БД
  │                         │                        │
  │──── getChanges(since) ─▶│ GET /api/v1/sync/       │
  │   ?since=1700000000000   │   changes?since=...     │
  │                         │──── SELECT WHERE ──────▶│
  │                         │   updated_at > since     │
  │                         │                        │
  │◀── {timestamp,accounts,◀│                        │
  │     categories,          │                        │
  │     transactions} ───────│                        │
```

**Request params:**
- `since` — timestamp в миллисекундах

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

**NOTE:** В текущей реализации `pullAll()` отключён. Первичная синхронизация не реализована.

---

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

## Сводная таблица: Error Handling

| Status | Причина | Действие |
|--------|---------|----------|
| 200 | OK | Продолжить |
| 400 | Bad request | Показать ошибку |
| 401 | Token invalid/revoked | logout(), показать логин |
| 404 | Not found | Показать ошибку |
| 500 | Server error | Retry via WorkManager |
| timeout | Нет сети | Retry via WorkManager |

---

## Flow: Авторизация → Синхронизация

```
┌─────────────────────────────────────────────────────────────┐
│ App Startup                                                │
│  │                                                        │
│  ├─ loadToken() from EncryptedSharedPreferences           │
│  │                                                        │
│  └─ if (token == null)                                    │
│       показать экран логина                                │
│       └── login() → saveToken() → main app                │
│                     │                                     │
│                     ▼                                     │
│              sync() через WorkManager                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ WorkManager (periodic, 15 min)                              │
│  │                                                        │
│  └─ sync()                                                │
│       │                                                    │
│       ├─ if (token == null) → failure "Не авторизован"   │
│       │                                                    │
│       └─ pushChanges()                                     │
│            │                                               │
│            ├─ 200 → success, update timestamp             │
│            ├─ 401 → logout(), failure                     │
│            └─ 500 → retry (max 3 attempts)                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ User-initiated Sync                                        │
│  │                                                        │
│  └─ sync() — same as WorkManager                          │
│       (отличается только UI feedback)                      │
└─────────────────────────────────────────────────────────────┘
```
