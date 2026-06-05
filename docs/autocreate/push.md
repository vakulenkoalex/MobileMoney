# Design: Создание операций из Push-уведомлений

## 1. Обзор

Автоматическое создание операций из push-уведомлений банковских приложений. Приложение перехватывает уведомления от доверенных приложений (из таблицы `senders`), парсит текст через `TextParser`, находит кошелёк по маске карты и создаёт транзакцию без подтверждения пользователя.

**Источник:** `TransactionSource.PUSH`

**Общие компоненты:**
- Парсинг текста, баланс, категории — [parsing_logic.md](parsing_logic.md)
- MessageWorker, CreateTransactionFromMessageUseCase, senders, дедупликация, ошибки — [background_message_processing.md](background_message_processing.md)

## 2. Flow

```
NotificationReceiverService.onNotificationPosted()
  ├─ FeaturePreferences.pushEnabled? ──NO──► drop
  │
  ▼
extract title, text, bigText из уведомления
  │
  ▼
body = title + "\n" + (bigText ?: text)
  │
  ▼
senderDao.findBySender(packageName)? ──null──► drop
  │
  ▼
MessageProcessor.process(context, sender=packageName, body, source="PUSH")
  │
  ▼
Дальнейшая обработка → background_message_processing.md
```

## 3. Компоненты

### 3.1 SettingsScreen — toggle

- Добавить toggle "Push-уведомления" после "Чтение SMS"
- При включении: открыть `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`
- После возврата в `onResume` — проверить доступ и установить `featurePrefs.pushEnabled = true`
- `POST_NOTIFICATIONS` (Android 13+) — переиспользовать существующий `checkNotificationPermission()`

### 3.2 AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />

<service
    android:name=".service.NotificationReceiverService"
    android:exported="true"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

Пользователь включает доступ вручную через системные настройки (`ACTION_NOTIFICATION_LISTENER_SETTINGS`).

## 4. FeaturePreferences — настройки для Push

| Ключ | Тип | По умолчанию | Описание |
|------|-----|-------------|----------|
| `push_enabled` | Boolean | false | Включить обработку Push-уведомлений |

Настройка управляется из `SettingsScreen`.