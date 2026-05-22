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
- id (PK) — TEXT NOT NULL, UUID
- name (TEXT)
- typeId (TEXT NOT NULL) — тип счёта: cash, card, account
- currencyCode (TEXT NOT NULL) — код валюты: RUB, USD, EUR
- icon (TEXT NOT NULL)
- isDefault (INTEGER NOT NULL)
- archived (INTEGER NOT NULL)
- autoCreateEnabled (INTEGER NOT NULL DEFAULT 0) — автосоздание транзакций
- cardMask (TEXT) — маска карты (последние 4 цифры, например "1026")
- createdAt (INTEGER NOT NULL)
- updatedAt (INTEGER NOT NULL)
- deletedAt (INTEGER)
- syncedAt (INTEGER)
- serverReceivedAt (INTEGER) — время получения записи сервером

### Регулярки для буфера обмена (message_regexes)
- id (PK) — TEXT NOT NULL, UUID
- pattern (TEXT NOT NULL) — regex с named groups: amount, shop, cardMask (balance опционально)
- skipBalanceCheck (INTEGER NOT NULL DEFAULT 0) — не проверять баланс при создании операции
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
- categoryId (TEXT NOT NULL) — FK -> categories.id
- amount (REAL NOT NULL)
- date (INTEGER NOT NULL)
- comment (TEXT)
- source (TEXT NOT NULL) — источник: MANUAL, SMS, PUSH, CLIPBOARD
- sourceData (TEXT)
- creatorId (TEXT)
- relatedTransactionId (TEXT) — UUID для связывания переводов
- shop (TEXT) — магазин/мерчант из SMS
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
- message_regexes — независимая таблица, не привязана к счетам
- categories образуют иерархию через parent_id
- transactions привязаны к account, category
- Перевод между счетами — две связанные операции с общим UUID в related_transaction_id