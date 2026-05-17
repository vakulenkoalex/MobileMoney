# Аудит конфиденциальности MobileMoney

Дата: 2026-05-15

---

## 🔴 Высокий уровень риска

| Проблема | Файл | Описание |
|----------|------|-----------|
| База данных не зашифрована | `android/app/src/main/java/com/mobilemoney/data/local/AppDatabase.kt:28-34` | Room database без encryption. Данные хранятся в plain text |
| Разрешён HTTP-трафик | `android/app/src/main/AndroidManifest.xml:13` | `usesCleartextTraffic="true"` — разрешены HTTP запросы (MITM уязвимость) |
| Бэкап без шифрования | `android/app/src/main/java/com/mobilemoney/data/repository/BackupRepository.kt:35-38` | Экспорт БД в открытом виде, доступен любому приложению |
| Сервер без SSL/TLS | `server/src/main/kotlin/com/mobilemoney/server/` | Нет HTTPS конфигурации в Ktor — пароли и токены передаются в открытом виде |

---

## 🟡 Средний уровень риска

| Проблема | Файл | Описание |
|----------|------|-----------|
| Логирование токена | `android/app/src/main/java/com/mobilemoney/data/repository/SyncRepository.kt:95` | `deviceToken?.take(10)...` — частичный вывод токена в лог |
| Логирование URL | `android/app/src/main/java/com/mobilemoney/viewmodel/LoginViewModel.kt:55` | Вывод serverUrl в лог при проверке сервера |
| BiometricPrompt не реализован | — | Указан в AGENTS.md, но не используется для доп. защиты |
| minifyEnabled=false | `android/app/build.gradle.kts:42` | Код не обфусцирован — легко читается при декомпиляции |

---

## Рекомендации

### 1. Шифрование БД (высокий приоритет)
Добавить SQLCipher для Room:
- Добавить зависимость `androidx.sqlite:sqlite-cipher`
- Изменить `Room.databaseBuilder()` на использование SQLCipher

### 2. Запретить HTTP (высокий приоритет)
В `AndroidManifest.xml`:
```xml
android:usesCleartextTraffic="false"
```

### 3. Biometric authentication (средний приоритет)
Реализовать `BiometricPrompt` для разблокировки приложения после фонового режима.

### 4. Убрать sensitive data из логов (средний приоритет)
Удалить вывод токена и URL в `Log.d()`:
- `SyncRepository.kt:95`
- `LoginViewModel.kt:55`

### 5. SSL на сервере (высокий приоритет)
Настроить HTTPS в Ktor с самоподписанным сертификатом для dev-режима.

### 6. Обфускация кода (низкий приоритет)
Включить `minifyEnabled = true` в release-сборках.

---

## Текущие меры защиты

✅ Токены хранятся в EncryptedSharedPreferences (AES-256)
✅ Пароли хешируются с солью (SHA-512)
✅ Используется Authorization заголовок для API