# Структура базы данных

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
- type_id (FK -> account_types.id)
- currency_code (FK -> currencies.code)
- icon (varchar)
- is_default (boolean) - использовать по умолчанию для новых операций
- archived (boolean) - для скрытия старых счетов
- created_at (timestamp)
- updated_at (timestamp)
- deleted_at (timestamp, nullable)

### Типы счетов (account_types)
- id (PK)
- name
- created_at (timestamp)
- updated_at (timestamp)
- deleted_at (timestamp, nullable)

### Категории (categories)
- id (PK)
- name
- is_income (boolean) - true=приход, false=расход
- icon (varchar)
- parent_id (FK -> categories.id, nullable)
- created_at (timestamp)
- updated_at (timestamp)
- deleted_at (timestamp, nullable)

### Метки (tags)
- id (PK)
- name
- color
- created_at (timestamp)
- updated_at (timestamp)
- deleted_at (timestamp, nullable)

### Операции (transactions)
- id (PK)
- account_id (FK -> accounts.id)
- category_id (FK -> categories.id)
- amount (decimal)
- date (datetime)
- comment
- source (text) - источник операции: MANUAL (ручной ввод), SMS, PUSH
- source_data (text) - необработанные данные от источника
- creator_id (FK -> users.id)
- related_transaction_id (varchar, nullable) - UUID для связывания переводов между счетами (не внешний ключ)
- created_at (timestamp)
- updated_at (timestamp)
- deleted_at (timestamp, nullable)

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
- Категории образуют иерархию через parent_id
- Операции привязаны к счету, категории, пользователю
- Метки можно назначить категории или операции
- Перевод между счетами - две связанные операции (расход с одного счёта, приход на другой) с общим UUID в related_transaction_id

