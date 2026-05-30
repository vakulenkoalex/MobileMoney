# AGENTS.md

## Build Commands

- Android: [docs/ANDROID_BUILD.md](docs/ANDROID_BUILD.md)
- Server: [docs/SERVER_RUN.md](docs/SERVER_RUN.md)

## Architecture

- **Monorepo** with three Gradle projects: `android/`, `server/`, `common/`
- **Common**: DTOs shared between Android and Server (`common/src/main/kotlin/com/mobilemoney/dto/`)
- **Android**: Kotlin/Jetpack Compose, Clean Architecture (presentation/domain/data), MVVM, minSDK 34
- **Server**: Kotlin/JVM + Ktor, SQLite (`data/sync.db`)

### Key domain rules
- Balance is **computed** from transactions: `SUM(income) - SUM(expense)` — not stored in a column
- Soft deletes only (`deleted_at` timestamp)
- Offline-first sync: write to Room locally, background sync via WorkManager, last-write-wins conflict resolution
- `related_transaction_id` links transfer pairs (two transactions with shared UUID)

## Tech Stack (Android)

| | |
|---|---|
| Language | Kotlin 2.3.21 |
| Compose Compiler | 2.3.21 |
| AGP | 9.2.0 |
| Min/Target SDK | 34 |
| DI | Manual (DI.kt) |
| DB | Room + KSP |
| Network | HttpURLConnection |
| Serialization | Kotlin Serialization |
| Async | Coroutines + Flow |
| Background | WorkManager |
| Pagination | Paging 3 |
| Charts | Vico |
| Security | EncryptedSharedPreferences, BiometricPrompt |

## Tech Stack (Server)

| | |
|---|---|
| Language | Kotlin 2.3.21 |
| Framework | Ktor 3.0.2 |
| Server | Netty |
| DB | SQLite |

## Project-Specific Conventions

- Docs are in Russian (`docs/*.md`)
- Icons: Material Design Icons names (e.g., `food`, `account-balance-wallet`)
- `READ_SMS` is a restricted permission on Android 14+; Google Play may reject apps using it
- Не забывай актуализировать документацию если что то меняешь

### Clean Architecture

**Поток вызовов (строго):**
```
ViewModel → UseCase → Repository(domain/) → RepositoryImpl(data/) → DatabaseRepository → DAO → SQLite
```

**Правила:**
- **Presentation** (`viewmodel/`, `ui/`) импортирует ТОЛЬКО `domain/`. Запрещён импорт `data/.*`
- **Domain** (`domain/`) не импортирует `data/.*` и не знает про Android, Room, SQLite
- **Workers** (`worker/`) получают use case через constructor injection, вызывают use case, не трогают DAO
- **SyncRepository** пишет транзакции через `TransactionRepositoryImpl.addTransaction()`, а не напрямую в `transactionDao.insert()`
- **Бизнес-логика** (расчёт баланса, склейка transfer, определение income/expense, валидация суммы) — в domain use cases, не в data-слое
- **`data/config/`** — конфиги UI (иконки, валюты) должны быть в `ui/config/`
- **DI** — только в `DI.kt`. ViewModel, Worker, Screen получают зависимости через constructor injection, не через `DI.` глобальный доступ

## Known Issues & Solutions

### Error Handling (Toast)

**Проблема:** Разрозненные inline ошибки в UI трудно поддерживать.

**Решение:** Единый механизм `ErrorHandler` (ui/common/ErrorHandler.kt) через Toast.

```kotlin
// Показать Toast
ErrorHandler.emitError("Сообщение об ошибке")
```

**Flow:**
1. Ошибка отправляется через `ErrorHandler.emitError()`
2. `MainActivity` слушает `ErrorHandler.errorFlow`
3. Показывает Android Toast (временно, ~2 сек)

**Использование:**
- Синхронизация: `SyncRepository.sync()` → `ErrorHandler.emitError()`
- Валидация: ViewModel.save() → `GlobalScope.launch { ErrorHandler.emitError() }`

**Files:**
- `ui/common/ErrorHandler.kt` — singleton object
- `MainActivity.kt` — подписка на errorFlow
- `SyncRepository.kt` — отправка sync errors
- ViewModels — отправка validation errors

### NetworkOnMainThreadException

**Проблема:** Сетевой запрос (HTTP) на главном потоке (UI thread) — запрещено в Android 3.0+.

**Симптом:** `android.os.NetworkOnMainThreadException` в stack trace.

**Решение:** Все сетевые методы в `SyncApiClient` должны использовать `withContext(Dispatchers.IO)`:

```kotlin
suspend fun verifyToken(): Result<Unit> {
    return withContext(Dispatchers.IO) {
        // сетевой код
    }
}
```

**Важно:** Функция должна быть `suspend` — иначе `withContext` не скомпилируется.

## References

- Architecture: `docs/architecture.md`
- DB schema: `docs/android_db_schema.md`, `docs/server_db_schema.md`
- Server run: `docs/SERVER_RUN.md`
- Build: `docs/ANDROID_BUILD.md`
- CI: `.github/workflows/android.yml` (builds on PR to master)