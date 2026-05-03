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
- type_id (varchar) - тип счёта: cash, card, account (строка, не enum)
- currency_code (FK -> currencies.code)
- icon (varchar)
- is_default (boolean) - использовать по умолчанию для новых операций
- archived (boolean) - для скрытия старых счетов
- created_at (timestamp, millis)
- updated_at (timestamp, millis)
- deleted_at (timestamp, nullable)
- synced_at (timestamp, nullable) - время последней синхронизации

### Категории (categories)
- id (PK)
- name
- is_income (boolean) - true=приход, false=расход
- icon (varchar)
- parent_id (FK -> categories.id, nullable)
- created_at (timestamp, millis)
- updated_at (timestamp, millis)
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
- account_id (FK -> accounts.id)
- category_id (FK -> categories.id, nullable)
- amount (decimal)
- date (timestamp, millis)
- comment (text)
- source (text) - источник: MANUAL, SMS, PUSH
- source_data (text) - необработанные данные от источника
- creator_id (FK -> users.id, nullable)
- related_transaction_id (varchar, nullable) - UUID для связывания переводов (не FK)
- created_at (timestamp, millis)
- updated_at (timestamp, millis)
- deleted_at (timestamp, nullable)
- synced_at (timestamp, nullable)

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

