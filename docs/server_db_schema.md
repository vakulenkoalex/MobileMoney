# Структура базы данных (Server)

## Таблицы

### Пользователи (users)
- login (PK) — email пользователя
- password_hash — SHA-256(password + salt)
- salt — соль для хеширования

### Устройства (devices)
- device_id (PK) — уникальный ID устройства
- device_name (NOT NULL) — название устройства
- token — JWT токен авторизации
- login (FK -> users.login)
- created_at (timestamp, millis)
- last_seen_at (timestamp, millis)
- revoked_at (timestamp, millis, nullable) — время отзыва токена

### Валюты (currencies)
- code (PK) — RUB, USD, EUR...
- name
- symbol
- created_at (timestamp, millis)
- updated_at (timestamp, millis)

### Счета (accounts)
- id (PK) — UUID
- name
- type_id — тип счёта: cash, card, account
- currency_code (FK -> currencies.code)
- icon
- is_default (boolean)
- archived (boolean)
- created_at (timestamp, millis)
- updated_at (timestamp, millis)
- deleted_at (timestamp, nullable)

### Категории (categories)
- id (PK) — UUID
- name
- is_income (boolean) — true=приход, false=расход
- icon
- parent_id (FK -> categories.id, nullable)
- created_at (timestamp, millis)
- updated_at (timestamp, millis)
- deleted_at (timestamp, nullable)

### Операции (transactions)
- id (PK) — UUID
- account_id (FK -> accounts.id)
- category_id (FK -> categories.id, nullable)
- amount (real)
- date (timestamp, millis)
- comment (text)
- source (varchar) — источник: MANUAL, SMS, PUSH
- source_data (text)
- creator_id (varchar)
- related_transaction_id (varchar, nullable) — UUID для связывания переводов
- created_at (timestamp, millis)
- updated_at (timestamp, millis)
- deleted_at (timestamp, nullable)

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