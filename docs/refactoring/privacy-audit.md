# Аудит безопасности MobileMoney

Дата: 2026-05-21

---

## 🔴 Высокий уровень риска

| Проблема | Файл | Описание |
|----------|------|-----------|
| База данных не зашифрована | `android/src/main/java/com/mobilemoney/data/local/AppDatabase.kt:28-34` | Room database без encryption. Данные хранятся в plain text |
| Разрешён HTTP-трафик | `android/src/main/AndroidManifest.xml:13` | `usesCleartextTraffic="true"` — разрешены HTTP запросы (MITM уязвимость) |
| Бэкап без шифрования | `android/src/main/java/com/mobilemoney/data/repository/BackupRepository.kt:35-38` | Экспорт БД в открытом виде, доступен любому приложению |
| Сервер без SSL/TLS | `server/src/main/kotlin/com/mobilemoney/server/Application.kt:19` | Нет HTTPS конфигурации в Ktor — пароли и токены передаются в открытом виде |
| Слабый хеш пароля | `server/src/main/kotlin/com/mobilemoney/server/HashUtil.kt:5-9` | SHA-512 — быстрый хеш, уязвим к GPU-брутфорсу даже с солью. Рекомендуется bcrypt/Argon2/PBKDF2 |
| User enumeration | `server/src/main/kotlin/com/mobilemoney/server/route/AuthRoute.kt:38` | 404 при "user not found" vs 401 при "invalid password" — позволяет проверить существование пользователей |
| Нет rate limiting | `server/src/main/kotlin/com/mobilemoney/server/route/AuthRoute.kt:15-44` | Отсутствует ограничение скорости запросов, блокировка учётной записи или CAPTCHA — брутфорс паролей ничем не ограничен |
| Токены никогда не истекают | `server/src/main/kotlin/com/mobilemoney/server/service/AuthService.kt:34-43` | Нет проверки `expiresAt` — украденный токен работает бесконечно |
| Нет валидации данных на sync | `server/src/main/kotlin/com/mobilemoney/server/service/SyncService.kt:16-35` | Любые данные от клиента принимаются без проверки суммы, дат, ссылочной целостности |

---

## 🟡 Средний уровень риска

| Проблема | Файл | Описание |
|----------|------|-----------|
| Логирование токена | `android/src/main/java/com/mobilemoney/data/repository/SyncRepository.kt:122` | `deviceToken?.take(10)...` — частичный вывод токена в лог |
| Логирование URL | `android/src/main/java/com/mobilemoney/viewmodel/LoginViewModel.kt:53` | Вывод serverUrl в лог при проверке сервера |
| BiometricPrompt не реализован | — | Указан в AGENTS.md, но не используется для доп. защиты |
| minifyEnabled=false | `android/build.gradle.kts:41` | Код не обфусцирован — легко читается при декомпиляции |
| Вербозные ошибки сервера | `server/src/main/kotlin/com/mobilemoney/server/route/SyncRoute.kt:42,67,91` | `e.message` возвращается клиенту в HTTP-ответе — утечка внутренней информации |
| ANDROID_ID как идентификатор | `android/src/main/java/com/mobilemoney/data/remote/SyncApiClient.kt:31` | `Settings.Secure.ANDROID_ID` — проблемы приватности, сбрасывается при factory reset |

---

## 🟢 Низкий уровень риска

| Проблема | Файл | Описание |
|----------|------|-----------|
| Дефолтный счёт для всех | `server/src/main/kotlin/com/mobilemoney/server/repository/Database.kt:191` | Счёт «Наличные» создаётся при первой инициализации БС для всех пользователей |
| Destructive migration | `android/src/main/java/com/mobilemoney/data/local/AppDatabase.kt:33` | `fallbackToDestructiveMigration()` может молча удалить все данные при несовместимости схемы |
| Токен — UUID без подписи | `server/src/main/kotlin/com/mobilemoney/server/service/AuthService.kt:23` | Токен не содержит signature/claims, валидность проверяется только через БД |

---

## Рекомендации

### 1. HTTPS на сервере (высокий приоритет)
Настроить SSL/TLS в Ktor. Для dev — самоподписанный сертификат, для production — Let's Encrypt.

### 2. Запретить HTTP на клиенте (высокий приоритет)
В `AndroidManifest.xml`:
```xml
android:usesCleartextTraffic="false"
```
Настроить `network_security_config.xml` для пина сертификата.

### 3. Шифрование БД (высокий приоритет)
Добавить SQLCipher для Room:
- Зависимость `androidx.sqlite:sqlite-cipher`
- Изменить `Room.databaseBuilder()` на использование SQLCipher

### 4. Заменить SHA-512 на bcrypt/Argon2 (высокий приоритет)
Изменить `HashUtil.kt` и `AuthService.kt` на использование bcrypt (или Argon2) с регулируемой стоимостью.

### 5. Убрать user enumeration (высокий приоритет)
Возвращать одинаковый ответ (401) при любом неуспешном логине, не уточняя причину.

### 6. Rate limiting на /api/v1/auth/login (высокий приоритет)
Добавить throttling (например, не более 5 попыток в минуту с одного IP/deviceId).

### 7. Token expiration (высокий приоритет)
Добавить поле `expiresAt` в токен и проверять при verify. Реализовать refresh-токены.

### 8. Валидация входящих sync-данных (средний приоритет)
Проверять: `amount` — число, `date` — не в будущем, `accountId`/`categoryId` — существуют, `source` — допустимое значение.

### 9. Biometric authentication (средний приоритет)
Реализовать `BiometricPrompt` для разблокировки приложения после фонового режима.

### 10. Убрать sensitive data из логов (средний приоритет)
Удалить вывод токена и URL в `Log.d()`:
- `SyncRepository.kt:122`
- `LoginViewModel.kt:53`
- `SyncApiClient.kt:93,161`

### 11. Не возвращать exception message клиенту (средний приоритет)
В `SyncRoute.kt` заменить `e.message` на общее "Internal server error" без деталей.

### 12. Обфускация кода (низкий приоритет)
Включить `minifyEnabled = true` + `proguard-rules.pro` в release-сборках.

---

## Текущие меры защиты

✅ Токены хранятся в EncryptedSharedPreferences (AES-256 GCM)
✅ Все SQL-запросы на сервере через prepared statements — SQL injection невозможен
✅ Пароль скрыт в UI (PasswordVisualTransformation)
✅ Интернет-пермишен только `INTERNET` (нет лишних разрешений)
✅ Используется Authorization заголовок для API
