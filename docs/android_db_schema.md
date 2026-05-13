# Структура базы данных (Android)

**Иконки** - название иконки из Material Design Icons (https://fonts.google.com/icons), например `food`, `car`, `account-balance-wallet`

## Таблицы

### Пользователи (users)
- id (PK)
- email
- password_hash
- name
- created_at (timestamp)
- updated_at (timestamp)
- deleted_at (timestamp, nullable) - пометка на удаление

### Валюты (currencies)
- code (PK) - RUB, USD, EUR...
- name
- symbol
- updated_at (timestamp)
- deleted_at (timestamp, nullable)

### Счета (accounts)
- id (PK)
- name
- typeId - тип счёта: cash, card, account (строка, не enum)
- currencyCode (FK -> currencies.code, NOT NULL)
- icon (NOT NULL)
- isDefault (boolean, NOT NULL) - использовать по умолчанию для новых операций
- archived (boolean, NOT NULL) - для скрытия старых счетов
- createdAt (timestamp, NOT NULL)
- updatedAt (timestamp, NOT NULL)
- deletedAt (timestamp)
- syncedAt (timestamp) - время последней синхронизации

### Категории (categories)
- id (PK)
- name
- is_income (boolean) - true=приход, false=расход
- icon (varchar)
- parent_id (FK -> categories.id, nullable)
- created_at (timestamp)
- updated_at (timestamp)
- deleted_at (timestamp, nullable)
- synced_at (timestamp, nullable)

### Метки (tags)
- id (PK)
- name
- color
- created_at (timestamp)
- updated_at (timestamp)
- deleted_at (timestamp, nullable)

### Операции (transactions)
- id (PK)
- accountId (FK -> accounts.id)
- categoryId (FK -> categories.id, nullable)
- amount (decimal)
- date (timestamp)
- comment (text, nullable)
- source (text, NOT NULL) - источник: MANUAL, SMS, PUSH
- sourceData (text, nullable) - необработанные данные от источника
- creatorId (FK -> users.id, nullable)
- relatedTransactionId (text, nullable) - UUID для связывания переводов (не FK)
- createdAt (timestamp)
- updatedAt (timestamp)
- deletedAt (timestamp, nullable)
- syncedAt (timestamp, nullable)

### Связь операций и меток (transaction_tags)
- transaction_id (FK -> transactions.id)
- tag_id (FK -> tags.id)
- PK (transaction_id, tag_id)

### Связь категорий и меток (category_tags)
- category_id (FK -> categories.id)
- tag_id (FK -> tags.id)
- PK (category_id, tag_id)

### Курсы валют (exchange_rates)
- currency_from (FK -> currencies.code)
- currency_to (FK -> currencies.code)
- rate (decimal)
- date (PK)
- PK (currency_from, currency_to, date)

## Связи

- Счета принадлежат одной валюте
- Тип счёта (type_id) - перечисление (cash, card, account), не внешний ключ
- Категории образуют иерархию через parent_id
- Операции привязаны к счету, категории, пользователю
- Метки можно назначить категории или операции
- Перевод между счетами - две связанные операции (расход с одного счёта, приход на другой) с общим UUID в related_transaction_id

