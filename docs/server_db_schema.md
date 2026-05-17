# Структура базы данных (Server)

## Таблицы

### Пользователи (users)
- login (PK) — TEXT NOT NULL, email пользователя
- passwordHash — TEXT NOT NULL, SHA-256(password + salt)
- salt — TEXT NOT NULL, соль для хеширования

### Устройства (devices)
- deviceId — TEXT NOT NULL UNIQUE, уникальный ID устройства
- deviceName (NOT NULL) — TEXT, название устройства
- token — TEXT, JWT токен авторизации
- login (TEXT NOT NULL) — FK -> users.login
- createdAt (INTEGER NOT NULL)
- lastSeenAt (INTEGER NOT NULL)
- revokedAt (INTEGER) — время отзыва токена

### Счета (accounts)
- id — TEXT NOT NULL, UUID
- name (TEXT)
- typeId (TEXT NOT NULL) — тип счёта: cash, card, account
- currencyCode (TEXT NOT NULL) — код валюты: RUB, USD, EUR
- icon (TEXT NOT NULL)
- isDefault (INTEGER NOT NULL)
- archived (INTEGER NOT NULL)
- createdAt (INTEGER NOT NULL)
- updatedAt (INTEGER NOT NULL)
- deletedAt (INTEGER)
- syncedAt (INTEGER)
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
- syncedAt (INTEGER)
- serverReceivedAt (INTEGER) — время получения записи сервером

### Операции (transactions)
- id (PK) — TEXT NOT NULL, UUID
- accountId (TEXT NOT NULL) — FK -> accounts.id
- categoryId (TEXT) — FK -> categories.id
- amount (REAL NOT NULL)
- date (INTEGER NOT NULL)
- comment (TEXT)
- source (TEXT NOT NULL) — источник: MANUAL, SMS, PUSH
- sourceData (TEXT)
- creatorId (TEXT)
- relatedTransactionId (TEXT) — UUID для связывания переводов
- createdAt (INTEGER NOT NULL)
- updatedAt (INTEGER NOT NULL)
- deletedAt (INTEGER)
- syncedAt (INTEGER)
- serverReceivedAt (INTEGER) — время получения записи сервером

## Справочники

### Валюты (currencies) - объект в коде
```kotlin
object Currencies {
    val all = listOf(
        Currency("RUB", "Российский рубль", "₽"),
        Currency("USD", "Доллар США", "$"),
        Currency("EUR", "Евро", "€")
    )
}
```
Не хранится в БД, определён в коде.

## Связи

- devices принадлежит users (по login)
- accounts принадлежит одной валюте (по currencyCode)
- categories образуют иерархию через parent_id
- transactions привязаны к account, category
- Перевод между счетами — две связанные операции с общим UUID в related_transaction_id