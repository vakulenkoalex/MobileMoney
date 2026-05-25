# Структура базы данных (Android)

**Иконки** — название иконки из Material Design Icons (https://fonts.google.com/icons), например `food`, `car`, `account-balance-wallet`

База данных: `mobile_money_database`, версия 11 (Room, миграции — `fallbackToDestructiveMigration`).

---

## Таблицы

### Счета (`accounts`)

| Колонка | Тип | Nullable | По умолчанию | Описание |
|---------|-----|----------|-------------|----------|
| `id` | `TEXT` (PK) | NO | — | UUID |
| `name` | `TEXT` | NO | — | Название счёта |
| `typeId` | `TEXT` | NO | — | Тип: `cash`, `card`, `account` (строка, не enum) |
| `currencyCode` | `TEXT` | NO | — | Код валюты: `RUB`, `USD`, `EUR`; **индексировано** |
| `icon` | `TEXT` | NO | — | Название иконки Material Design |
| `isDefault` | `INTEGER` (boolean) | NO | `0` | Использовать по умолчанию для новых операций |
| `archived` | `INTEGER` (boolean) | NO | `0` | Скрыть старый счёт |
| `autoCreateEnabled` | `INTEGER` (boolean) | NO | `0` | Автосоздание операции для этого счёта |
| `cardMask` | `TEXT` | YES | — | Маска карты (последние 4 цифры) |
| `createdAt` | `INTEGER` | NO | — | Unix timestamp (ms) |
| `updatedAt` | `INTEGER` | NO | — | |
| `deletedAt` | `INTEGER` | YES | — | Soft delete |
| `syncedAt` | `INTEGER` | YES | — | Время последней синхронизации |
| `serverReceivedAt` | `INTEGER` | YES | — | Время получения сервером |

**Индексы:** `currencyCode`

**Foreign keys:** нет

---

### Регулярки для буфера обмена (`message_regexes`)

| Колонка | Тип | Nullable | По умолчанию | Описание |
|---------|-----|----------|-------------|----------|
| `id` | `TEXT` (PK) | NO | — | UUID |
| `label` | `TEXT` | NO | — | Название (метка) для отображения в списке |
| `pattern` | `TEXT` | NO | — | Regex с named groups: amount, shop, cardMask (balance опционально) |
| `skipBalanceCheck` | `INTEGER` (boolean) | NO | `0` | Не проверять баланс при создании операции |
| `createdAt` | `INTEGER` | NO | — | Unix timestamp (ms) |
| `updatedAt` | `INTEGER` | NO | — | |
| `deletedAt` | `INTEGER` | YES | — | Soft delete |
| `syncedAt` | `INTEGER` | YES | — | Время последней синхронизации |
| `serverReceivedAt` | `INTEGER` | YES | — | Время получения сервером |

**Foreign keys:** нет

---

### Категории (`categories`)

| Колонка | Тип | Nullable | По умолчанию | Описание |
|---------|-----|----------|-------------|----------|
| `id` | `TEXT` (PK) | NO | — | UUID |
| `name` | `TEXT` | NO | — | Название |
| `isIncome` | `INTEGER` (boolean) | NO | — | `1` = доход, `0` = расход |
| `icon` | `TEXT` | NO | — | Иконка Material Design |
| `parentId` | `TEXT` | YES | — | Родительская категория; **индексировано** |
| `createdAt` | `INTEGER` | NO | — | |
| `updatedAt` | `INTEGER` | NO | — | |
| `deletedAt` | `INTEGER` | YES | — | Soft delete |
| `syncedAt` | `INTEGER` | YES | — | |
| `serverReceivedAt` | `INTEGER` | YES | — | |

**Индексы:** `parentId`

**Foreign keys:** `parentId` → `categories(id)` ON DELETE SET NULL

---

### Операции (`transactions`)

| Колонка | Тип | Nullable | По умолчанию | Описание |
|---------|-----|----------|-------------|----------|
| `id` | `TEXT` (PK) | NO | — | UUID |
| `accountId` | `TEXT` | NO | — | ID счёта; **индексировано** (не FK) |
| `categoryId` | `TEXT` | NO | — | ID категории; **индексировано** (не FK) |
| `amount` | `REAL` | NO | — | Доход > 0, расход < 0 |
| `date` | `INTEGER` | NO | — | Unix timestamp (ms) |
| `comment` | `TEXT` | NO | — | Комментарий |
| `source` | `TEXT` | NO | `MANUAL` | Источник: `MANUAL`, `SMS`, `PUSH`, `CLIPBOARD` |
| `sourceData` | `TEXT` | YES | — | Необработанные данные источника |
| `creatorId` | `TEXT` | YES | — | ID пользователя (не FK — на клиенте нет `users`) |
| `relatedTransactionId` | `TEXT` | YES | — | UUID для связывания перевода (не FK); **индексировано** |
| `shop` | `TEXT` | YES | — | Название магазина |
| `createdAt` | `INTEGER` | NO | — | |
| `updatedAt` | `INTEGER` | NO | — | |
| `deletedAt` | `INTEGER` | YES | — | Soft delete |
| `syncedAt` | `INTEGER` | YES | — | |
| `serverReceivedAt` | `INTEGER` | YES | — | |

**Индексы:** `accountId`, `categoryId`, `relatedTransactionId`

**Foreign keys:** нет

---

### Отправители (`senders`)

| Колонка | Тип | Nullable | По умолчанию | Описание |
|---------|-----|----------|-------------|----------|
| `id` | `TEXT` (PK) | NO | — | UUID |
| `sender` | `TEXT` | NO | — | Идентификатор (номер телефона, имя пакета и т.д.); **уникальный индекс** |
| `label` | `TEXT` | NO | `""` | Метка для отображения |
| `type` | `TEXT` | NO | `"PHONE_NUMBER"` | Вид: `PHONE_NUMBER`, `PACKAGE_NAME`, `MESSENGER_PACKAGE_NAME`, `MESSENGER_USERNAME` |
| `createdAt` | `INTEGER` | NO | — | Unix timestamp (ms) |
| `updatedAt` | `INTEGER` | NO | — | |
| `deletedAt` | `INTEGER` | YES | — | Soft delete |
| `syncedAt` | `INTEGER` | YES | — | Время последней синхронизации |
| `serverReceivedAt` | `INTEGER` | YES | — | Время получения сервером |

**Индексы:** `sender` (unique)

**Foreign keys:** нет

---

## Справочники

### Валюты (`Currency`) — enum в коде

```kotlin
enum class Currency(val code: String, val displayName: String, val symbol: String) {
    RUB("RUB", "Российский рубль", "₽"),
    USD("USD", "Доллар США", "$"),
    EUR("EUR", "Евро", "€");
}
```

Не хранится в БД, определён в `data/local/Currency.kt`.

### Источники операций (`TransactionSource`) — enum в коде

```kotlin
enum class TransactionSource { MANUAL, SMS, PUSH, CLIPBOARD }
```

Определён в `data/local/Entities.kt`.

---

## Связи

- Счёт привязан к валюте (по `currencyCode`, не FK)
- Тип счёта (`typeId`) — строка: `cash`, `card`, `account`
- Регулярки для буфера обмена (`message_regexes`) — независимая таблица, не привязана к счетам
- Категории образуют иерархию через `parentId`
- Операция привязана к счёту (`accountId`) и категории (`categoryId`) — без FK, с индексами
- Перевод между счетами — две операции (расход и доход) с общим UUID в `relatedTransactionId`
- Баланс счёта вычисляется: `SUM(amount WHERE amount > 0) - SUM(amount WHERE amount < 0)` — не хранится в колонке
- Soft delete: записи не удаляются, а получают timestamp в `deletedAt`