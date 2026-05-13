# Структура базы данных (Server)

## Таблицы

### Пользователи (users)
- login (PK) — TEXT NOT NULL, email пользователя
- passwordHash — TEXT NOT NULL, SHA-256(password + salt)
- salt — TEXT NOT NULL, соль для хеширования

### Устройства (devices)
- deviceId (PK) — TEXT NOT NULL, уникальный ID устройства
- deviceName (NOT NULL) — TEXT, название устройства
- token — TEXT, JWT токен авторизации
- login (TEXT NOT NULL) — FK -> users.login
- createdAt (INTEGER NOT NULL)
- lastSeenAt (INTEGER NOT NULL)
- revokedAt (INTEGER) — время отзыва токена

### Валюты (currencies)
- code (PK) — TEXT NOT NULL, RUB, USD, EUR...
- name (TEXT NOT NULL)
- symbol (TEXT NOT NULL)
- createdAt (INTEGER NOT NULL)
- updatedAt (INTEGER NOT NULL)
- serverReceivedAt (INTEGER) — время получения записи сервером

### Счета (accounts)
- id (PK) — TEXT NOT NULL, UUID
- name (TEXT)
- typeId (TEXT NOT NULL) — тип счёта: cash, card, account
- currencyCode (TEXT NOT NULL) — FK -> currencies.code
- icon (TEXT NOT NULL)
- isDefault (INTEGER NOT NULL)
- archived (INTEGER NOT NULL)
- createdAt (INTEGER NOT NULL)
- updatedAt (INTEGER NOT NULL)
- deletedAt (INTEGER)
- serverReceivedAt (INTEGER) — время получения записи сервером

### Категории (categories)
- id (PK) — TEXT NOT NULL, UUID
- name (TEXT NOT NULL)
- isIncome (INTEGER NOT NULL) — true=приход, false=расход
- icon (TEXT NOT NULL)
- parentId (TEXT) — FK -> categories.id
- createdAt (INTEGER NOT NULL)
- updatedAt (INTEGER NOT NULL)
- deletedAt (INTEGER)
- serverReceivedAt (INTEGER) — время получения записи сервером

### Операции (transactions)
- id (PK) — TEXT NOT NULL, UUID
- accountId (TEXT NOT NULL) — FK -> accounts.id
- categoryId (TEXT) — FK -> categories.id
- amount (REAL NOT NULL)
- date (INTEGER NOT NULL)
- comment (TEXT)
- source (TEXT) — источник: MANUAL, SMS, PUSH
- sourceData (TEXT)
- creatorId (TEXT)
- relatedTransactionId (TEXT) — UUID для связывания переводов
- createdAt (INTEGER NOT NULL)
- updatedAt (INTEGER NOT NULL)
- deletedAt (INTEGER)
- serverReceivedAt (INTEGER) — время получения записи сервером

## Связи

- devices принадлежит users (по login)
- accounts принадлежит одной валюте (currency_code)
- categories образуют иерархию через parent_id
- transactions привязаны к account, category
- Перевод между счетами — две связанные операции с общим UUID в related_transaction_id

## Отличия от клиентской БД

| Сущность | Клиент | Сервер |
|----------|--------|--------|
| users | полная структура (id, email, name, timestamps) | упрощённая (login, password_hash, salt) |
| currencies | есть (с deleted_at) | есть (без deleted_at) |
| tags | есть | нет |
| transaction_tags | есть | нет |
| category_tags | есть | нет |
| exchange_rates | есть | нет |
| synced_at поля | есть у accounts, categories, transactions | нет |
| source/sourceData | есть | нет |