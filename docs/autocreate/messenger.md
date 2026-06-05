# Design: Создание операций из Push-уведомлений мессенджеров

## 1. Обзор

Обработка push-уведомлений от мессенджеров (Telegram, WhatsApp и т.д.) для создания операций. Отличается от обычных push-уведомлений дополнительной валидацией: уведомление должно приходить от известного контакта (пользователя мессенджера), а не от любого отправителя.

**Источник:** `TransactionSource.PUSH`

**Общие компоненты** — см. [push.md](push.md).

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
Валидация отправителя (см. ниже)
  │
  ▼
MessageProcessor.process(context, sender=packageName, body, source="PUSH")
  │
  ▼
Дальнейшая обработка → background_message_processing.md
```

## 3. Валидация отправителя

Основное отличие от обычного push — проверка, что уведомление пришло от известного контакта мессенджера.

### 3.1 Типы отправителей

| SenderType | Описание | Пример sender |
|------------|----------|---------------|
| `PACKAGE_NAME` | Обычное банковское приложение | `ru.sberbank.mobile` |
| `MESSENGER_PACKAGE_NAME` | Приложение-мессенджер | `org.telegram.messenger` |
| `MESSENGER_USERNAME` | Контакт в мессенджере | `Иван Иванов` |

### 3.2 Алгоритм

```kotlin
{ senderDao ->
    val knownSender = senderDao.findBySender(packageName) ?: return@process false
    val type = SenderType.valueOf(knownSender.type)

    type in listOf(SenderType.PACKAGE_NAME, SenderType.MESSENGER_PACKAGE_NAME) &&
        (type != SenderType.MESSENGER_PACKAGE_NAME ||
         senderDao.findByType(SenderType.MESSENGER_USERNAME.name)
             .any { it.sender.equals(title, ignoreCase = true) })
}
```

1. Найти отправителя по `packageName` в таблице `senders`
   - Не найден → уведомление игнорируется
2. Если тип отправителя — `PACKAGE_NAME` → уведомление принимается (обычный push)
3. Если тип отправителя — `MESSENGER_PACKAGE_NAME` → дополнительная проверка:
   - Искать все записи с типом `MESSENGER_USERNAME`
   - Если хотя бы один `MESSENGER_USERNAME.sender` совпадает с `title` уведомления (без учёта регистра) → уведомление принимается
   - Если ни один не совпал → уведомление игнорируется
4. Если тип отправителя — любой другой → уведомление игнорируется

### 3.3 Настройка

Для обработки уведомлений от мессенджера необходимо:

1. Добавить приложение-мессенджер в `senders` с типом `MESSENGER_PACKAGE_NAME`:
   - `sender` = packageName (например, `org.telegram.messenger`)
   - `type` = `MESSENGER_PACKAGE_NAME`

2. Добавить контакты в `senders` с типом `MESSENGER_USERNAME`:
   - `sender` = имя контакта как оно отображается в заголовке уведомления
   - `type` = `MESSENGER_USERNAME`

### 3.4 Пример

**Настройки:**
- `org.telegram.messenger` → `MESSENGER_PACKAGE_NAME`
- `Иван Иванов` → `MESSENGER_USERNAME`
- `Мария Петрова` → `MESSENGER_USERNAME`

**Уведомление:**
```
Title: Иван Иванов
Text: *1026 оплата 339.98р MAGNIT MK GO
```

**Обработка:**
1. `findBySender("org.telegram.messenger")` → найден, тип `MESSENGER_PACKAGE_NAME`
2. `findByType("MESSENGER_USERNAME")` → найдены `[Иван Иванов, Мария Петрова]`
3. `"Иван Иванов".equals("Иван Иванов", ignoreCase=true)` → true
4. Уведомление принято → `MessageProcessor.process(source="PUSH")`

## 4. Отличия от обычного Push

| Аспект | Обычный Push | Messenger Push |
|--------|-------------|----------------|
| Тип отправителя | `PACKAGE_NAME` | `MESSENGER_PACKAGE_NAME` |
| Валидация | Только packageName в senders | packageName + title совпадает с `MESSENGER_USERNAME` |
| Доп. настройка | Не требуется | Добавить контакты как `MESSENGER_USERNAME` |
| Парсинг тела | `body = title + "\n" + text` | То же самое |

Всё остальное (MessageWorker, CreateTransactionFromMessageUseCase, дедупликация, ошибки) — идентично обычному push, см. [push.md](push.md) и [background_message_processing.md](background_message_processing.md).
