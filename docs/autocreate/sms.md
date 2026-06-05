# Дизайн: Автоматическое создание операций из SMS

## 1. Обзор

Автоматическое создание операций из входящих SMS. Приложение получает SMS от доверенных отправителей (из таблицы `senders`), парсит текст через `TextParser`, находит кошелёк по маске карты и создаёт транзакцию без подтверждения пользователя.

**Источник:** `TransactionSource.SMS`

**Общие компоненты:**
- Парсинг текста, баланс, категории — [parsing_logic.md](parsing_logic.md)
- MessageWorker, senders, дедупликация, ошибки — [background_message_processing.md](background_message_processing.md)

## 2. Flow

```
SMS_RECEIVED → SmsBroadcastReceiver
  ├─ smsEnabled? — нет → игнор
  │
  ▼
extract sender, body из intent
  │
  ▼
MessageProcessor.process(context, sender, body, source="SMS")
  └─ валидация: sender в senders + тип PHONE_NUMBER
  │
  ▼
Дальнейшая обработка → background_message_processing.md
```

## 3. Компоненты

### 3.1 SettingsScreen — toggle

- Добавить toggle "Чтение SMS" в настройках
- При включении: запросить разрешение `RECEIVE_SMS`
- Если `RECEIVE_SMS` получено → запросить `POST_NOTIFICATIONS`
- Если любое разрешение не получено → toggle выключается
- При загрузке экрана: проверить наличие разрешений, отключить toggle если отсутствуют

### 3.2 AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<receiver
    android:name=".receiver.SmsBroadcastReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="100">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

## 4. FeaturePreferences — настройки для SMS

| Ключ | Тип | По умолчанию | Описание |
|------|-----|-------------|----------|
| `sms_enabled` | Boolean | false | Включить обработку SMS |

Настройки управляются из `SettingsScreen`.
