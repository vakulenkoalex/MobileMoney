# Дизайн: Автоматическое создание операций из SMS

## Дата: 2026-05-22

## 1. Обзор

Автоматическое создание операций расхода из входящих SMS. Приложение получает SMS от доверенных отправителей, парсит текст, находит кошелёк по маске карты и создаёт транзакцию без подтверждения пользователя.

**Источник операции:** `source = TransactionSource.SMS`, `sourceData =` оригинальный текст SMS

**Только расход:** income-операции из SMS не создаются.

## 2. Архитектура

### 2.1 Общая схема

```
SMS_RECEIVED → SmsBroadcastReceiver
  ├─ smsEnabled? — нет → игнорим
  ├─ debugMode? — да → INSERT в messages (без проверок), выход
  ├─ sender в senders? — нет → игнорим
  ├─ sourceData в transactions за сегодня? — есть → игнорим
  └─ INSERT INTO messages (sender, body, receivedAt, processed=0)
      └─ enqueue SmsWorker (OneTimeWorkRequest)
             │
             ▼
         SmsWorker (CoroutineWorker)
           ├─ SELECT * FROM messages WHERE processed = 0
           ├─ для каждой:
           │   ├─ TextParser.parse(body, регексы из message_regexes)
           │   │   └─ не спарсилось → error="parse_failed"
           │   ├─ accountDao.findByCardMask(mask)
           │   │   └─ не найден → error="account_not_found"
            │   ├─ getLastTransactionByShop(shop) — подбор категории
            │   │   └─ если categoryId = null → getDefaultCategory(isIncome=false)
            │   ├─ saveTransaction(source=SMS, sourceData=body)
           │   └─ UPDATE processed=1, error, transactionId
           └─ показать Notification
```

### 2.2 SmsBroadcastReceiver (детально)

```
onReceive(context, intent):
  if (!featurePreferences.smsEnabled) return
  val sender = extractSender(intent)
  val body = extractBody(intent)
  if (sender == null || body == null) return

  if (featurePreferences.debugModeEnabled) {
      messageDao.insert(MessageEntity(sender, body, receivedAt=now, processed=0))
      return
  }

  if (senderDao.findBySender(sender) == null) return
  if (transactionDao.existsBySourceDataSince(body, startOfToday)) return

  messageDao.insert(MessageEntity(sender, body, receivedAt=now, processed=0))
  SmsWorker.enqueue(context)
```

## 3. Структуры данных

### 3.1 messages (локальная, не синкается)

```kotlin
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val processed: Boolean = false,
    val error: String? = null,
    val transactionId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 3.2 senders (синкается с сервером)

```kotlin
@Entity(
    tableName = "senders",
    indices = [Index("sender", unique = true)]
)
data class SenderEntity(
    @PrimaryKey val id: String,
    val sender: String,
    val label: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)
```

### 3.3 Индекс для дедупликации

Запрос дедупликации теперь фильтрует по `date >= :todayStart`, поэтому индекс по `sourceData` остаётся необходимым (ускоряет поиск в комбинации с `deletedAt IS NULL`).

```sql
CREATE INDEX IF NOT EXISTS idx_transactions_source_data ON transactions(sourceData)
```

### 3.4 AccountDao — метод для матчинга

```kotlin
@Query("SELECT * FROM accounts WHERE cardMask = :mask AND deletedAt IS NULL LIMIT 1")
suspend fun getAccountByCardMask(mask: String): AccountEntity?
```

## 4. Компоненты

### 4.1 SmsBroadcastReceiver

- Действие: `android.provider.Telephony.SMS_RECEIVED`
- Использует `goAsync()` для асинхронных операций с БД
- Разрешение: `RECEIVE_SMS` (runtime)

### 4.2 SmsWorker (CoroutineWorker)

- Input: отсутствует (читает сам `messages WHERE processed = 0`)
- Для каждого сообщения:
  1. Парсинг через TextParser — ошибка → уведомление "Не удалось обработать SMS: {shop/маска}"
  2. Поиск кошелька по cardMask — не найден → уведомление "Не удалось обработать SMS: кошелёк ****{mask} не найден"
   3. Подбор категории через getLastTransactionByShop
      — если транзакция не найдена или categoryId == null, используется категория по умолчанию (getDefaultCategory(isIncome=false))
   4. Проверка баланса: если `balance` есть — вычислить расхождение, записать в comment
  5. Создание транзакции через SaveTransactionUseCase
  6. UPDATE messages (processed, error, transactionId)
- Notification per-ошибка: каждая неудачная попытка — своё уведомление с причиной
- Notification успех: "Создана операция: {сумма} в {магазин}"
- Notification Channel: "sms_processing"

### 4.3 TextParser (rename ClipboardParser)

- `ClipboardParser` → `TextParser`
- `ParsedClipboardData` → `ParsedTextData`
- Все остальное без изменений

### 4.4 SenderDto (common)

```kotlin
@Serializable
data class SenderDto(
    val id: String,
    val sender: String,
    val label: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncedAt: Long? = null,
    val serverReceivedAt: Long? = null
)
```

### 4.5 FeaturePreferences (rename ClipboardPreferences)

```kotlin
class FeaturePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("feature_prefs", Context.MODE_PRIVATE)

    var clipboardParsingEnabled: Boolean
        get() = prefs.getBoolean("clipboard_parsing_enabled", false)
        set(value) = prefs.edit().putBoolean("clipboard_parsing_enabled", value).apply()

    var smsEnabled: Boolean
        get() = prefs.getBoolean("sms_enabled", false)
        set(value) = prefs.edit().putBoolean("sms_enabled", value).apply()

    var debugModeEnabled: Boolean
        get() = prefs.getBoolean("debug_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("debug_mode_enabled", value).apply()
}
```

### 4.6 MessageListScreen / MessageListViewModel

- Список messages (sender, body preview, receivedAt, статус)
- Иконка копирования → ClipboardManager
- Вход: SettingsScreen → кнопка "Сообщения SMS"

## 5. Синхронизация senders

### 5.1 Common

- `SenderDto` в `common/dto/SenderDto.kt`
- `SyncPushRequest.kt`: + `val senders: List<SenderDto> = emptyList()`
- `SyncChangesResponse.kt`: + `val senders: List<SenderDto> = emptyList()`

### 5.2 Server

- `Database.kt`: CREATE TABLE senders
- `SenderRepository.kt`: upsert, getAll, getUpdatedSince
- `DI.kt`: + senderRepository
- `SyncService.kt`: senders в push / pull / getChanges

### 5.3 Android

- `SenderEntity` в Entities.kt
- `SenderDao` в Dao.kt
- `SyncMapper.kt`: Entity ↔ Dto
- `SyncRepository.kt`: senders в push/pull

## 6. Разрешения и AndroidManifest

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Регистрация ресивера:
```xml
<receiver
    android:name=".receiver.SmsBroadcastReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="100">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

## 7. Пользовательский опыт

- **SMS включено:** фоновая обработка, уведомления о результатах
- **Режим отладки:** все SMS сохраняются в messages, но не обрабатываются
- **MessageListScreen:** просмотр и копирование текста сообщений

## 8. Проверка баланса

`balance` — опциональная named-группа в regex. Если её нет в тексте — проверка не выполняется.

Если баланс распарсен:
```
Текущий баланс счёта = SUM(INCOME) - SUM(EXPENSE) всех транзакций
Ожидаемый баланс = Баланс из SMS
Сумма операции = amount из SMS

Ожидаемый баланс после операции = Текущий баланс - Сумма операции
Расхождение = Ожидаемый баланс после операции - Баланс из SMS
```

Если расхождение != 0 → записать в комментарий транзакции:
```
Баланс расходится: ожидалось {ожидаемый после операции}, получено {баланс из SMS}
```

Если расхождение == 0 → комментарий пустой.

## 9. Обработка ошибок

| Ситуация | Результат |
|----------|-----------|
| SMS отключено в настройках | Игнор |
| debugMode + любая SMS | INSERT без проверок |
| Отправитель не в senders | Игнор |
| Дубликат sourceData за сегодня | Игнор |
| Regex не спарсил | error = "parse_failed", notification |
| cardMask не совпал | error = "account_not_found", notification |
| Ошибка сохранения транзакции | error = "save_failed", notification |
| Успех | notification: "{сумма} в {магазин}" |

> **Важно:** Дедупликация проверяется только среди операций за сегодня. Если та же SMS приходила раньше сегодняшнего дня — она не считается дубликатом (актуально для ежедневных SMS с одинаковым текстом, например проверка баланса).

## 10. Пример

### Входные данные

SMS:
```
*1026 покупка 339.98р MAGNIT MK GO Баланс 1441.81
```

Regex:
```
\*(?<cardMask>\d{4})\s+\S+\s+(?<amount>[\d]+(?:[.,]\d+)?)\s*р\.?\s+(?<shop>[^.]+?)\s*\.?\s*(?:Баланс\s+(?<balance>[\d]+(?:[.,]\d+)?))?
```

### Обработка

1. sender в senders → да
2. Дубликат → нет
3. TextParser: amount=339.98, shop=MAGNIT MK GO, cardMask=1026, balance=1441.81
4. findByCardMask("1026") → "Карта Сбер"
5. getLastTransactionByShop("MAGNIT MK GO") → категория "Продукты"
6. Проверка баланса:
   - Текущий баланс счёта: 1000.00 (сумма всех транзакций до)
   - Ожидаемый после операции: 1000.00 - 339.98 = 660.02
   - Баланс из SMS: 1441.81
   - Расхождение: 660.02 - 1441.81 = -781.79 ≠ 0
   - Комментарий: "Баланс расходится: ожидалось 660.02, получено 1441.81"
7. Создание транзакции: amount=-339.98, account="Карта Сбер", category="Продукты", shop="MAGNIT MK GO", comment="Баланс расходится: ...", source=SMS
8. Notification: "Создана операция: 339.98р в MAGNIT MK GO"

## 11. Категория по умолчанию для автосозданий

### 11.1 Назначение

Если `SmsWorker` не находит последнюю транзакцию по магазину (`getLastTransactionByShop` вернул null) или у найденной транзакции нет `categoryId`, он подставляет категорию по умолчанию для расходов:

```kotlin
val lastTx = dbRepo.getLastTransactionByShop(parsed.shop)
val defaultCategory = dbRepo.getDefaultCategory(isIncome = false)
val categoryId = lastTx?.categoryId ?: defaultCategory?.id
```

### 11.2 Разделение по типу

Система поддерживает **две независимые** категории по умолчанию: для расхода (`isIncome=false`) и для дохода (`isIncome=true`). При установке флага в UI сбрасывается `isDefault` только для категорий того же типа:

```kotlin
// CategoryDao
@Query("UPDATE categories SET isDefault = 0 WHERE isIncome = :isIncome")
suspend fun clearDefaultCategories(isIncome: Boolean)
```

### 11.3 UI — CategoryFormScreen

- Чекбокс "По умолчанию для автосозданий" под фильтром "Расход/Доход"
- При включении: `categoryRepository.clearDefaultCategories(state.isIncome)` → затем `addCategory/updateCategory`
- В списке категорий (`CategoryListScreen`) категория по умолчанию выделяется жирным и подписью "По умолчанию"
