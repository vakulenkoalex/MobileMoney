# Дизайн: Создание операций из буфера обмена

## 1. Обзор

Создание операций по тексту из буфера обмена. Пользователь нажимает кнопку "Вставить" на главном экране, приложение парсит текст из буфера, показывает диалог подтверждения, затем открывает форму с предзаполненными полями для редактирования и сохранения.

**Отличие от SMS/Push:** транзакция **не создаётся автоматически** — пользователь подтверждает в диалоге, затем редактирует форму и сохраняет сам.

**Источник:** `TransactionSource.CLIPBOARD`

**Общие компоненты** (TextParser, баланс, категории, regex) — см. `parsing_logic.md`.

**Структуры данных** (accounts, Transaction) — см. [parsing_logic.md](parsing_logic.md#6-структуры-данных-транзакции).

## 2. ParseClipboardTransactionUseCase

**Файл:** `android/.../domain/usecase/transaction/ParseClipboardTransactionUseCase.kt`

### Flow

```
parse(text, debugMode = false):
  ├─ Загрузить все regex из message_regexes
  ├─ Загрузить все счета, отфильтровать autoCreateEnabled
  │
  ├─ RegexMatcher.findBest(text, regexes) — единый поиск (см. parsing_logic.md)
  │   └─ Если не найден → Result.NoMatch
  │
  ├─ Найти enabledAccount по cardMask парсера
  │   └─ Если не найден → Result.NoAccount
  │
  ├─ debugMode? → вернуть Result.DebugInfo (всегда)
  │
  ├─ Подобрать категорию (общая логика)
  ├─ Проверить баланс (общая логика, учитывает skipBalanceCheck)
  └─ Вернуть Result.Success(ClipboardResult)
```

### Sealed Result

```kotlin
sealed class Result {
    data class Success(val prefill: ClipboardResult) : Result()
    data class NoMatch(val text: String) : Result()
    data class NoAccount(val text: String) : Result()
    data class DebugInfo(...) : Result()
}
```

### ClipboardResult

```kotlin
data class ClipboardResult(
    val parsed: ParsedTransaction,
    val account: Account,
    val categoryId: UUID?,
    val comment: String,
    val matchedRegex: Boolean = false
)
```

## 3. UI Flow

### 3.1 Кнопка "Вставить из буфера"

На главном экране "Операции" (`TransactionListScreen`) в TopAppBar (справа) — иконка `ContentPaste`.

**Видна только когда** `clipboard_parsing_enabled = true` (настройка).

При нажатии:
1. Читается буфер (`clipboardManager.primaryClip?.coerceToText()`)
2. Вызывается `ParseClipboardTransactionUseCase.parse(text)`
3. В зависимости от результата:

| Результат | Действие |
|-----------|----------|
| `Success` | Показать `ClipboardDialog` |
| `NoMatch` | `ErrorHandler.emitError("Не удалось распознать текст")` |
| `NoAccount` | `ErrorHandler.emitError("Не найден счёт для создания")` |
| `DebugInfo` | Показать `DebugClipboardDialog` (всегда, если debugMode) |

### 3.2 ClipboardDialog

- Показывается когда regex и cardMask совпали
- Отображает:
  - Оригинальный текст из буфера
  - Найденный счёт
  - Сумму и магазин
- Кнопки:
  - "Создать" → `TransactionFormViewModel.prefillFromClipboard()` и переход на `TransactionFormScreen`
  - "Отмена" → закрыть

### 3.3 TransactionFormViewModel.prefillFromClipboard()

```kotlin
fun prefillFromClipboard(data: ClipboardPrefillData)
```

Устанавливает:
- `amount` — из `parsed.amount`
- `selectedAccount` — найденный счёт
- `category` — подобранная категория (или null)
- `comment` — комментарий (если расхождение баланса)
- `type` — `INCOME` или `EXPENSE` (из `parsed.isIncome`)
- `shop` — `parsed.shop`
- `source` — `TransactionSource.CLIPBOARD`
- `sourceData` — оригинальный текст из буфера

После этого открывается `TransactionFormScreen` — пользователь может отредактировать все поля.

### 3.4 Сохранение

При сохранении через `TransactionFormViewModel.save()`:
- `TransactionSource.CLIPBOARD` → маппится в `TransactionOrigin.CLIPBOARD`
- Вызывается `SaveTransactionUseCase`
- Транзакция записывается в БД

### 3.5 DebugClipboardDialog

Показывается **всегда** при нажатии кнопки "Вставить", если включён `debug_mode_enabled` (Настройки → "Режим отладки") и буфер содержит текст.

Отображает:
- Текст из буфера
- Результат парсинга для первого подошедшего regex
- Если совпал: счёт, amount, shop, cardMask, isIncome, balance
- Если не совпал: причина

Кнопка **"Перечитать"** — заново читает буфер и парсит.

После закрытия:
- Если совпал → показать `ClipboardDialog`
- Если не совпал → ничего

## 4. Обработка ошибок

| Ситуация | Результат |
|----------|-----------|
| Буфер пустой | Ничего |
| Regex не совпал | `ErrorHandler.emitError("Не удалось распознать текст")` |
| Маска карты не совпала (нет счёта) | `ErrorHandler.emitError("Не найден счёт для создания")` |
| Нет regex в message_regexes | `ErrorHandler.emitError("Не удалось распознать текст")` |
| Нет счетов с autoCreateEnabled | `ErrorHandler.emitError("Не найден счёт для создания")` |
| Ошибка сохранения формы | Стандартная валидация формы |

## 5. FeaturePreferences — настройки для Clipboard

| Ключ | Тип | По умолчанию | Описание |
|------|-----|-------------|----------|
| `clipboard_parsing_enabled` | Boolean | false | Показывать кнопку "Вставить" |
| `debug_mode_enabled` | Boolean | false | Показывать DebugClipboardDialog |
