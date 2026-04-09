# Структура базы данных

**Иконки** - название иконки из Material Design Icons (https://fonts.google.com/icons), например `food`, `car`, `account-balance-wallet`

## Таблицы

### Пользователи (users)
- id (PK)
- email
- пароль
- имя
- создан (timestamp)
- изменен (timestamp)
- удален (timestamp, nullable) - пометка на удаление

### Валюты (currencies)
- код (PK) - RUB, USD, EUR...
- название
- символ
- изменен (timestamp)
- удален (timestamp, nullable)

### Счета (accounts)
- id (PK)
- название
- тип_id (FK -> account_types.id)
- валюта_id (FK -> currencies.код)
- иконка (varchar)
- архивный (boolean) - для скрытия старых счетов
- создан (timestamp)
- изменен (timestamp)
- удален (timestamp, nullable)

### Типы счетов (account_types)
- id (PK)
- название
- создан (timestamp)
- изменен (timestamp)
- удален (timestamp, nullable)

### Категории (categories)
- id (PK)
- название
- приход (boolean) - true=приход, false=расход
- иконка (varchar)
- родитель_id (FK -> categories.id, nullable)
- создан (timestamp)
- изменен (timestamp)
- удален (timestamp, nullable)

### Метки (tags)
- id (PK)
- название
- цвет
- создан (timestamp)
- изменен (timestamp)
- удален (timestamp, nullable)

### Операции (transactions)
- id (PK)
- счет_id (FK -> accounts.id)
- категория_id (FK -> categories.id, nullable)
- сумма
- дата (включает время)
- комментарий
- источник (varchar) - откуда пришла операция (SMS/push)
- источник_данные (text) - необработанные данные от источника
- создал_id (FK -> users.id)
- связанная_операция_id (FK -> transactions.id, nullable) - для переводов между счетами
- создан (timestamp)
- изменен (timestamp)
- удален (timestamp, nullable)

### Связь операций и меток (transaction_tags)
- transaction_id (FK -> transactions.id)
- tag_id (FK -> tags.id)
- PK (transaction_id, tag_id)

### Связь категорий и меток (category_tags)
- category_id (FK -> categories.id)
- tag_id (FK -> tags.id)
- PK (category_id, tag_id)

### Курсы валют (exchange_rates)
- валюта_from (FK -> currencies.код)
- валюта_to (FK -> currencies.код)
- курс
- дата (PK)
- PK (валюта_from, валюта_to, дата)

## Связи

- Счета принадлежат одной валюте
- Категории образуют иерархию через родитель_id
- Операции привязаны к счету, категории, пользователю
- Метки можно назначить категории или операции
- Перевод между счетами - две связанные операции (расход с одного счета, приход на другой) через связанная_операция_id

