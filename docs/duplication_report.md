# Отчет по дублированию кода

## 🔴 Высокий приоритет (точные копии / ~90-100% идентичные)

| # | Что дублируется | Файлы | Строк |
|---|---|---|---|
| 1 | `AccountType` enum | `domain/model/Account.kt:5` и `data/model/Models.kt:6` | ~5 |
| 2 | Domain-модели vs UI-модели | `domain/model/{Account,Category,Transaction}.kt` vs `data/model/Models.kt` | ~50 |
| 3 | Room Entity vs DTO | `data/local/Entities.kt` vs `common/.../dto/{Account,Category,Transaction}Dto.kt` | ~50 |
| 4 | `createEncryptedSharedPreferences()` | `MobileMoneyApp.kt:31-63` и `SyncRepository.kt:34-66` | 40 |
| 5 | Auth-check в server routes | `SyncRoute.kt:22-31`, `47-57`, `72-82` | 30 |
| 11 | Currency definition | 3 файла: `Currencies.kt`, `Currency.kt` (Android), `Currencies.kt` (server) | ~30 |

## 🟡 Средний приоритет (pattern duplication)

| # | Что дублируется | Описание |
|---|---|---|
| 6 | Server Repository CRUD | 3 репозитория (`Account`, `Category`, `Transaction`) — одинаковые `upsert()`, `getAll()`, `getUpdatedSince()`, `mapRow()` |
| 7 | Mapper-файлы | 6 файлов, ~500 строк конвертации Entity↔Ui↔Domain↔DTO |
| 8 | Android Repository delegation | `RepositoryImpl` → `DatabaseRepository` → Room DAO — 3 слоя для каждой сущности |
| 9 | Use case one-liners | `GetXxxUseCase` — 4 шт, просто делегируют в репозиторий |

## 🟢 Низкий приоритет (шаблонный код)

| # | Что дублируется |
|---|---|
| 10 | ViewModel StateFlow pattern (8 ViewModel) |
| 12 | `IconOption` / `CategoryIconOption` |
| 13 | TopAppBar в FormScreen (3 шт) |
| 14 | Empty state в ListScreen (3 шт) |
| 15 | Icon picker ModalBottomSheet (2-3 шт) |
| 16 | API error handling в `SyncApiClient` (5 методов) |

## Топ-5 рекомендаций по исправлению

1. **Удалить `data/model/Models.kt`** — UI-модели на 95% копируют domain-модели; это также уберет ~500 строк мапперов
2. **Вынести Currencies в `common` модуль** — сейчас определена в 3 местах
3. **Вынести `createEncryptedSharedPreferences()`** в общий утилитарный файл
4. **Добавить `verifyAuth()` helper** в `SyncRoute.kt` — 30 строк повторяющегося auth-кода
5. **Оставить один `AccountType` enum** в `domain/model/Account.kt`
