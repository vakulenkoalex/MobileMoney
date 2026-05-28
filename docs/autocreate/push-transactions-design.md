# Design: Создание операций из Push-уведомлений

Дата: 2026-05-23
Статус: Утверждён

## Проблема

Сейчас операции создаются из SMS (через `SmsBroadcastReceiver`). Нужен второй канал — уведомления банковских приложений (например, Сбербанк, Тинькофф) через `NotificationListenerService`.

## Решение

Новый сервис `NotificationReceiverService`, который перехватывает уведомления от известных приложений, сохраняет их в таблицу `messages` и ставит в очередь `SmsWorker` (переименованный в `MessageWorker`) для обработки.

## Архитектура

### Flow

```
Приложение (bank)
  │
  ▼
NotificationReceiverService.onNotificationPosted()
  │
  ├─ FeaturePreferences.pushEnabled? ──NO──► drop
  ├─ senderDao.findBySender(packageName)? ──null──► drop
  ├─ transactionDao.countBySourceDataSince(body, todayStart)? ──>0──► drop
  │
  ▼
text = title + "\n" + body (без слова "SMS" в заголовке)
  │
  ▼
MessageDao.insert(MessageEntity(sender=packageName, body=text))
  │
  ▼
WorkManager.enqueue(MessageWorker)
  │
  ▼
MessageWorker.doWork()
  │
  ├─ Get unprocessed messages (SMS + Push — все)
  ├─ For each message:
  │   ├─ TextParser.parse(text, regex)
  │   ├─ AccountDao.getAccountByCardMask(mask)
  │   ├─ Balance discrepancy check
  │   ├─ Category lookup (последняя по shop / дефолтная)
  │   ├─ DatabaseRepository.addTransaction(TransactionUi(source=PUSH))
  │   └─ MessageDao.markProcessed(id, transactionId)
```

### Компоненты

#### Новые

| Компонент | Файл | Описание |
|-----------|------|----------|
| `NotificationReceiverService` | `service/NotificationReceiverService.kt` | `NotificationListenerService`, фильтр по `senders`, сохранение в `messages`, enqueue Worker |

#### Изменяемые

| Компонент | Изменения |
|-----------|-----------|
| `SmsWorker` | → **`MessageWorker`** (переименование, "SMS" → "message" внутри, канал `sms_processing` → `message_processing`) |
| `FeaturePreferences` | + `pushEnabled: Boolean` (default `false`) |
| `SettingsScreen` | + toggle "Push-уведомления" (аналогично SMS toggle). При включении: открыть `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`. `POST_NOTIFICATIONS` — переиспользовать существующий `checkNotificationPermission()` |
| `AndroidManifest.xml` | + `<service>` для `NotificationReceiverService` |

#### Без изменений

| Компонент | Причина |
|-----------|---------|
| `TextParser` | На вход `text` — конкатенация `title\nbody` делается до парсера |
| `MessageEntity` | Поле `sender` содержит `packageName` |
| `SenderEntity`, `senderDao` | Поле `sender` может быть как телефон, так и packageName |
| `DatabaseRepository.addTransaction()` | Абстрагирован от источника |
| `TransactionSource` | `PUSH` уже объявлен в enum |
| `messageDao` | Без изменений |
| `MessageRegexEntity`, `messageRegexDao` | Без изменений |
| `SyncRepository` | Без изменений |
| `SyncApiClient` | Без изменений |

### Детали реализации

#### NotificationReceiverService

```kotlin
class NotificationReceiverService : NotificationListenerService() {
    // onNotificationPosted:
    //   1. Проверить pushEnabled
    //   2. Получить packageName из StatusBarNotification
    //   3. senderDao.findBySender(packageName)
    //   4. duplicate-чек по sourceData
    //   5. text = notification.title + "\n" + notification.text
    //   6. MessageDao.insert(MessageEntity(...))
    //   7. MessageWorker.enqueue()
}
```

- Без `goAsync()` — `NotificationListenerService` не имеет таймаута
- Все операции на `Dispatchers.IO`

#### MessageWorker (бывший SmsWorker)

- Переименовать файл `SmsWorker.kt` → `MessageWorker.kt`
- Переименовать класс `SmsWorker` → `MessageWorker`
- Обновить ссылки в коде (`MobileMoneyApp.kt`, `SmsBroadcastReceiver.kt`)
- `MobileMoneyApp.kt:36-37`: channelId `"sms_processing"` → `"message_processing"`, name `"SMS обработка"` → `"Обработка сообщений"`
- `SmsWorker.showNotification()`: channelId `"sms_processing"` → `"message_processing"`, title `"SMS"` → `"Обработка"`
- `Log.e("SmsWorker", ...)` → `Log.e("MessageWorker", ...)` (2 места)
- `SmsBroadcastReceiver.kt`: import `SmsWorker` → `MessageWorker`, `OneTimeWorkRequestBuilder<SmsWorker>()` → `OneTimeWorkRequestBuilder<MessageWorker>()`
- `showErrorNotification()`: текст `"Не удалось обработать SMS"` → `"Не удалось обработать сообщение"`
- Логика без изменений — уже обрабатывает все `unprocessed` сообщения

#### FeaturePreferences

```kotlin
var pushEnabled: Boolean
    get() = prefs.getBoolean("push_enabled", false)
    set(value) = prefs.edit().putBoolean("push_enabled", value).apply()
```

#### SettingsScreen

Добавить toggle после "Чтение SMS". Паттерн аналогичный существующему:

```kotlin
// Новый toggle
androidx.compose.foundation.layout.Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Text("Push-уведомления")
    Switch(
        checked = pushEnabled,
        onCheckedChange = { enabled ->
            if (enabled) {
                // Проверить, есть ли доступ к NotificationListener
                val component = ComponentName(context, NotificationReceiverService::class.java)
                val listeners = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                )
                if (listeners == null || !listeners.contains(component.flattenToString())) {
                    // Открыть системные настройки Notification Access
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                } else {
                    featurePrefs.pushEnabled = true
                    pushEnabled = true
                    checkNotificationPermission()  // существующий метод
                }
            } else {
                featurePrefs.pushEnabled = false
                pushEnabled = false
            }
        }
    )
}
```

После включения через системные настройки пользователь возвращается в приложение — в `onResume`/при повторном открытии проверить доступ и установить `featurePrefs.pushEnabled = true`.

#### Permission

```xml
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
```

Пользователь включает доступ вручную: `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.

`POST_NOTIFICATIONS` (Android 13+) — уже запрашивается в `SettingsScreen.kt:74-91` для уведомлений от Worker. Push переиспользует существующий `checkNotificationPermission()`.

### Отличия от SMS-флоу

| Аспект | SMS | Push |
|--------|-----|------|
| Источник | `SmsBroadcastReceiver` | `NotificationReceiverService` |
| Таймаут | `goAsync()` (5-10 сек) | Нет |
| `sender` | Номер телефона | PackageName |
| `sourceData` | `body` | `title\nbody` |
| `TransactionSource` | `SMS` | `PUSH` |
| Персистенция | `MessageEntity` + Worker | `MessageEntity` + Worker |
| Worker | `MessageWorker` | `MessageWorker` (тот же) |

### Sync

`TransactionSource.PUSH` уже включён в `TransactionDto` через enum serialization. `SenderEntity` использует существующую таблицу `senders` — packageName синхронизируется как и любой другой sender. Отдельной логики синхронизации для PUSH не требуется.

### Error Handling

- `pushEnabled` выключен → silent drop
- Приложение не в `senders` → silent drop
- Дубликат (то же `sourceData` сегодня) → silent drop
- Парсинг не удался → `markProcessed("parse_failed")`, notification об ошибке
- Счёт не найден по cardMask → `markProcessed("account_not_found")`, notification об ошибке
- Ошибки через `ErrorHandler.emitError()` не требуются (это фоновый процесс)

## Decision Log

- **NotificationListenerService** вместо FCM — не требуется серверная инфраструктура
- **Один Worker** для SMS и Push — унифицированная очередь обработки
- **senderDao** переиспользован — packageName хранится как `sender`
- **messageDao** переиспользован — `MessageEntity` не привязан к типу источника
- **Отдельный toggle** `pushEnabled` — независимое управление от SMS
