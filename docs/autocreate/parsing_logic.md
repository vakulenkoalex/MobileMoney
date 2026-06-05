# Общая логика парсинга и создания транзакций

Этот документ описывает компоненты и алгоритмы, общие для всех режимов авто-создания транзакций из текста (SMS, Push, Clipboard).

## 1. TextParser — движок парсинга

**Файл:** `android/.../domain/parser/TextParser.kt`

Stateless object с единственным методом:

```kotlin
fun parse(text: String, regex: String): ParsedTransaction?
```

### Алгоритм
1. Проверка: `text` и `regex` не пустые — иначе `null`
2. Создать `Regex(pattern, IGNORE_CASE)` и применить `find(text)`
3. Извлечь все именованные группы из regex через `(?<name>...)`
4. Проверить обязательные группы: **amount**, **shop**, **cardMask**, **direction** — если хоть одна пустая/отсутствует → `null`
5. Определить `isIncome` по `direction` (см. ниже)
6. Вернуть `ParsedTransaction`

### Обязательные named-группы

| Группа | Описание |
|--------|----------|
| `(?<amount>...)` | Сумма операции (может содержать `,` или `.`) |
| `(?<shop>...)` | Название магазина/мерчанта |
| `(?<cardMask>...)` | Последние 4 цифры карты |
| `(?<direction>...)` | Тип операции (покупка, оплата, поступление и т.д.) |

### Опциональные named-группы

| Группа | Описание |
|--------|----------|
| `(?<balance>...)` | Баланс после операции (если есть — выполняется проверка баланса) |

### Пример работы TextParser

**Входной текст:**
```
*1026 покупка 339.98р MAGNIT MK GO Баланс 1441.81
```

**Regex:**
```
\*(?<cardMask>\d{4})\s+(?<direction>\S+)\s+(?<amount>[\d]+(?:[.,]\d+)?)\s*р\.?\s+(?<shop>[^.]+?)\s*\.?\s*(?:Баланс\s+(?<balance>[\d]+(?:[.,]\d+)?))?
```

**Результат парсинга:**
- `amount` = `339.98`
- `shop` = `"MAGNIT MK GO"`
- `cardMask` = `"1026"`
- `direction` = `"покупка"` → не содержит ключевых слов дохода → **expense** (`isIncome=false`)
- `balance` = `"1441.81"` (опциональная группа — присутствует, проверка баланса будет выполнена)

**ParsedTransaction:**
```kotlin
ParsedTransaction(
    amount = 339.98,
    shop = "MAGNIT MK GO",
    cardMask = "1026",
    isIncome = false,
    balance = "1441.81"
)
```

## 1.1 Алгоритм выбора regex (RegexMatcher)

Единый для всех режимов. Один проход:

Для каждого regex в порядке из `message_regexes`:
- Применить `TextParser.parse(text, regex.pattern)` — первый успешный → используем его
- Если TextParser совпал → это результат
- Если **режим отладки** включён — параллельно считать **filledGroups** для fallback:
  - `amount` > 0
  - `shop` не пустой
  - `cardMask` не пустой
  - `balance` не пустой (опционально)

Если ни один regex не спарсил:
- Режим отладки **включён** → fallback: regex с максимальным `filledGroups`
- Режим отладки **выключен** → ошибка "Не найден формат"

> `filledGroups` учитывает только непустые группы. `balance` считается опционально — если группы нет в тексте, она не заполнена.

Используется в:
- `CreateTransactionFromMessageUseCase` — фоновая обработка (SMS/Push)
- `ParseClipboardTransactionUseCase` — буфер обмена (+ доп. проверка `autoCreateEnabled`)

## 2. ParsedTransaction — результат парсинга

**Файл:** `android/.../domain/model/ParsedTransaction.kt`

```kotlin
data class ParsedTransaction(
    val amount: Double,
    val shop: String,
    val cardMask: String,
    val isIncome: Boolean = false,
    val balance: String? = null
)
```

Сумма сразу хранится как `Double`. Остальные поля — строки.

## 3. MessageRegex — конфигурация regex

**Файл:** `android/.../domain/model/MessageRegex.kt`

```kotlin
data class MessageRegex(
    val id: UUID = UUID.randomUUID(),
    val label: String,
    val pattern: String,
    val skipBalanceCheck: Boolean = false
)
```

### Валидация при сохранении (`MessageRegexFormViewModel.validateAndSave()`)

1. Label и pattern не пустые
2. Regex синтаксически корректен (компилируется `Regex(...)`)
3. Содержит именованные группы: `amount`, `shop`, `cardMask`, `direction`
4. Если `skipBalanceCheck == false` — должен содержать `(?<balance>...)`

### Хранение

Таблица `message_regexes` (Room Entity, синкается с сервером):

| Поле | Тип | Описание |
|------|-----|----------|
| `id` | String (UUID) | PK |
| `label` | String | Название |
| `pattern` | String | Regex pattern |
| `skipBalanceCheck` | Boolean | Не проверять баланс |
| `createdAt` | Long | |
| `updatedAt` | Long | |
| `deletedAt` | Long? | Soft delete |
| `syncedAt` | Long? | |
| `serverReceivedAt` | Long? | |

## 4. Определение дохода/расхода

`isIncome` вычисляется по группе `direction`:

```kotlin
private val INCOME_KEYWORDS = setOf("поступление", "зачисление", "доход", "приход")

val isIncome = direction.lowercase().let { dir ->
    INCOME_KEYWORDS.any { dir.contains(it) }
}
```

Если `direction` содержит любое из ключевых слов → **income**. Иначе → **expense**.

## 5. TransactionSource / TransactionOrigin

### TransactionSource — источник операции

**Файл:** `android/.../domain/model/TransactionSource.kt`

```kotlin
enum class TransactionSource { MANUAL, SMS, PUSH, CLIPBOARD }
```

### TransactionOrigin — происхождение (для UI фильтрации)

**Файл:** `android/.../domain/model/Transaction.kt`

```kotlin
enum class TransactionOrigin { MANUAL, SMS, PUSH, CLIPBOARD }
```

Маппинг `source → origin`:

| TransactionSource | TransactionOrigin |
|-------------------|-------------------|
| `MANUAL` | `MANUAL` |
| `SMS` | `SMS` |
| `PUSH` | `PUSH` |
| `CLIPBOARD` | `CLIPBOARD` |

## 6. Структуры данных транзакции

### 6.1 accounts — поле autoCreateEnabled

```kotlin
// AccountEntity
val autoCreateEnabled: Boolean = false,  // Включить авто-создание для счёта
val cardMask: String                     // Последние 4 цифры карты
```

Только счета с `autoCreateEnabled = true` участвуют в матчинге при парсинге из буфера (Clipboard).

### 6.2 Transaction — поля source/sourceData/shop

| Поле | Тип | Описание |
|------|-----|----------|
| `source` | TransactionSource | Источник операции (MANUAL, SMS, PUSH, CLIPBOARD) |
| `sourceData` | String? | Оригинальный текст сообщения/буфера |
| `shop` | String? | Магазин из сообщения (парсится из именованной группы `shop`) |

## 7. Алгоритм подбора категории

Используется во всех режимах для автоматического определения категории:

```kotlin
val lastTx = transactionRepository.getLastTransactionByShop(shop, isIncome)
val defaultCategory = categoryRepository.getDefaultCategory(isIncome = isIncome)
val categoryId = lastTx?.categoryId ?: defaultCategory?.id
```

1. Если есть `shop` — искать последнюю транзакцию с таким же `shop` и тем же типом (`isIncome`)
2. Если транзакция найдена и у неё есть `categoryId` — использовать её категорию
3. Если не найдена — использовать категорию по умолчанию для этого типа (`isIncome`)
4. Если категории по умолчанию нет — `categoryId = null`

## 8. Алгоритм проверки баланса

Выполняется **только если**:
- Regex содержит группу `(?<balance>...)` **и** в тексте найдено значение баланса
- `skipBalanceCheck == false` (для regex'а или всего режима)

### Формула

```
currentBalance = accountRepository.getAccountBalance(accountId)
                └─ SUM(INCOME) - SUM(EXPENSE) всех транзакций счёта

expectedAfter = currentBalance - amount
discrepancy = expectedAfter - parsedBalance (из текста)
```

> **Примечание:** `amount` для expense — положительное число (сумма в тексте). Вычитание корректно для обоих типов, т.к. `getAccountBalance` возвращает сумму с учётом знака транзакций.

### Результат

- `|discrepancy| <= 0.01` → комментарий пустой (баланс сошёлся)
- `|discrepancy| > 0.01` → комментарий:
  ```
  Баланс расходится: ожидалось {expectedAfter}, получено {parsedBalance}
  ```

### Пример

- Текущий баланс счёта: 1000.00
- Сумма операции: 339.98
- Ожидаемый после: 1000.00 - 339.98 = 660.02
- Баланс из SMS: 1441.81
- Расхождение: 660.02 - 1441.81 = -781.79 ≠ 0
- Комментарий: `"Баланс расходится: ожидалось 660.02, получено 1441.81"`

## 9. Категория по умолчанию для авто-созданий

Система поддерживает **две независимые** категории по умолчанию: для расхода и для дохода.

```sql
-- CategoryDao
@Query("UPDATE categories SET isDefault = 0 WHERE isIncome = :isIncome")
suspend fun clearDefaultCategories(isIncome: Boolean)
```

- В `CategoryFormScreen` есть чекбокс "По умолчанию для автосозданий"
- При включении: сбрасывается `isDefault` для всех категорий того же типа → затем устанавливается у выбранной
- В списке категорий категория по умолчанию выделяется жирным и подписью "По умолчанию"
