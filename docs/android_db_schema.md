# Структура базы данных (Android)

**Иконки** - название иконки из Material Design Icons (https://fonts.google.com/icons), например `food`, `car`, `account-balance-wallet`

## Таблицы

### Счета (accounts)
- id (PK)
- name
- typeId - тип счёта: cash, card, account (строка, не enum)
- currencyCode (text, NOT NULL) - код валюты: RUB, USD, EUR
- icon (NOT NULL)
- isDefault (boolean, NOT NULL) - использовать по умолчанию для новых операций
- archived (boolean, NOT NULL) - для скрытия старых счетов
- createdAt (timestamp, NOT NULL)
- updatedAt (timestamp, NOT NULL)
- deletedAt (timestamp, nullable)
- syncedAt (timestamp, nullable) - время последней синхронизации
- serverReceivedAt (timestamp, nullable)

### Категории (categories)
- id (PK)
- name
- isIncome (boolean) - true=приход, false=расход
- icon (varchar)
- parentId (FK -> categories.id, nullable)
- createdAt (timestamp)
- updatedAt (timestamp)
- deletedAt (timestamp, nullable)
- syncedAt (timestamp, nullable)
- serverReceivedAt (timestamp, nullable)

### Метки (tags)
- id (PK)
- name
- color
- createdAt (timestamp)
- updatedAt (timestamp)
- deletedAt (timestamp, nullable)

### Операции (transactions)
- id (PK)
- accountId (FK -> accounts.id)
- categoryId (FK -> categories.id)
- amount (decimal)
- date (timestamp)
- comment (text)
- source (text, NOT NULL) - источник: MANUAL, SMS, PUSH
- sourceData (text, nullable) - необработанные данные от источника
- creatorId (FK -> users.id, nullable)
- relatedTransactionId (text, nullable) - UUID для связывания переводов (не FK)
- createdAt (timestamp)
- updatedAt (timestamp)
- deletedAt (timestamp, nullable)
- syncedAt (timestamp, nullable)
- serverReceivedAt (timestamp, nullable)

### Связь операций и меток (transaction_tags)
- transaction_id (FK -> transactions.id)
- tag_id (FK -> tags.id)
- PK (transaction_id, tag_id)

### Связь категорий и меток (category_tags)
- category_id (FK -> categories.id)
- tag_id (FK -> tags.id)
- PK (category_id, tag_id)

## Справочники

### Валюты (currencies) - enum
```kotlin
enum class Currency(val code: String, val name: String, val symbol: String) {
    RUB("RUB", "Российский рубль", "₽"),
    USD("Доллар США", "$"),
    EUR("Евро", "€")
}
```
Не хранится в БД, определён в коде.

## Связи

- Счета принадлежат одной валюте (по currencyCode)
- Тип счёта (type_id) - перечисление (cash, card, account), не внешний ключ
- Категории образуют иерархию через parent_id
- Операции привязаны к счету, категории, пользователю
- Метки можно назначить категории или операции
- Перевод между счетами - две связанные операции (расход с одного счёта, приход на другой) с общим UUID в related_transaction_id