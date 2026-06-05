# Общая фоновая обработка (SMS + Push)

Этот документ описывает компоненты, общие для фоновой обработки SMS и Push-уведомлений. Оба режима сохраняют входящие сообщения в таблицу `messages` и обрабатывают их через единый `MessageWorker`.

## 1. CreateTransactionFromMessageUseCase

**Файл:** `android/.../domain/usecase/transaction/CreateTransactionFromMessageUseCase.kt`

Вызывается из `MessageWorker` для каждого непрочитанного сообщения.

```kotlin
suspend operator fun invoke(message: Message): Result
```

### Flow

1. Загрузить все regex из `messageRegexRepository`
2. Выбор regex — см. [parsing_logic.md: алгоритм выбора regex](parsing_logic.md#11-алгоритм-выбора-regex-regexmatcher)
3. Найти счёт по `cardMask` — если нет → `AccountNotFound`
4. Определить `isIncome` из парсера
5. Подобрать категорию (см. `parsing_logic.md`)
6. Проверить баланс (см. `parsing_logic.md`)
7. Создать `Transaction(origin = message.source, sourceData = message.body)` — `SMS` или `PUSH`
8. `transactionRepository.addTransaction(transaction)`
9. `messageRepository.markProcessed(message.id, transactionId)`

### Sealed Result

```kotlin
sealed class Result {
    data class Success(val amount: Double, val shop: String) : Result()
    data class ParseFailed(val error: String) : Result()
    data class AccountNotFound(val cardMask: String) : Result()
    data class SaveFailed(val error: String) : Result()
}
```

## 2. MessageWorker

**Файл:** `android/.../worker/MessageWorker.kt`

- `CoroutineWorker`, запускается через `WorkManager.enqueue()`
- Читает все `unprocessed` сообщения из `messages` (SMS + Push — единая очередь)
- Для каждого вызывает `CreateTransactionFromMessageUseCase`
- Notification per-ошибка: каждая неудачная попытка — своё уведомление с причиной
- Notification успех: "Создана операция: {сумма} в {магазин}"
- Notification Channel: `"message_processing"` (канал создаётся в `MobileMoneyApp.onCreate()`)
- Логи тегированы `"MessageWorker"`

## 3. messages — таблица входящих сообщений

**Локальная таблица (не синкается с сервером).**

```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: String,          // для SMS — номер телефона, для Push — packageName
    val body: String,            // для SMS — body, для Push — title + "\n" + body
    val receivedAt: Long,
    val processed: Boolean = false,
    val error: String? = null,   // null, "parse_failed", "account_not_found", "save_failed"
    val transactionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "SMS"   // "SMS" или "PUSH"
)
```

## 4. senders — таблица доверенных отправителей

**Синкается с сервером.** Используется для валидации: сообщения от неизвестных отправителей игнорируются.

```kotlin
@Entity(
    tableName = "senders",
    indices = [Index("sender", unique = true)]
)
data class SenderEntity(
    @PrimaryKey val id: String,
    val sender: String,          // для SMS — номер телефона, для Push — packageName
    val label: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)
```

### Синхронизация senders

**Common:**
- `SenderDto` в `common/dto/SenderDto.kt`
- `SyncPushRequest.kt`: `+ val senders: List<SenderDto>`
- `SyncChangesResponse.kt`: `+ val senders: List<SenderDto>`

**Server:**
- `Database.kt`: CREATE TABLE senders
- `SenderRepository.kt`: upsert, getAll, getUpdatedSince
- `DI.kt`: + senderRepository
- `SyncService.kt`: senders в push / pull / getChanges

**Android:**
- `SenderEntity` в Entities.kt
- `SenderDao` в Dao.kt
- `SyncMapper.kt`: Entity ↔ Dto
- `SyncRepository.kt`: senders в push/pull

## 5. Дедупликация

Проверяется **только за сегодня** (`date >= todayStart`). Это актуально для ежедневных сообщений с одинаковым текстом (например, проверка баланса).

```sql
CREATE INDEX IF NOT EXISTS idx_transactions_source_data ON transactions(sourceData)
```

Алгоритм: перед вставкой в `messages` проверить, есть ли транзакция с таким же `sourceData` за сегодня. Если есть — игнорировать сообщение.

## 6. Обработка ошибок (фоновые уведомления)

| Ситуация | Результат |
|----------|-----------|
| Источник отключён в настройках | Silent drop (до Worker) |
| Отправитель не в senders | Silent drop (до Worker) |
| Дубликат sourceData за сегодня | Silent drop (до Worker) |
| Regex не спарсил | `markProcessed("parse_failed")`, notification |
| cardMask не совпал | `markProcessed("account_not_found")`, notification |
| Ошибка сохранения транзакции | `markProcessed("save_failed")`, notification |
| Успех | Notification: "{сумма} в {магазин}" |

Ошибки отображаются через Android Notification (не `ErrorHandler`), т.к. это фоновый процесс без UI.
