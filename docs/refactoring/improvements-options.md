# Варианты улучшения кода

Сводка на основе анализа кодовой базы и существующих документов (`code-review.md`, `2026-05-13-architectural-refactoring-design.md`).

---

## 🔴 Критические (баги — потеря данных)

| Проблема | Файл | Суть |
|----------|------|------|
| `fallbackToDestructiveMigration(dropAllTables = true)` | `AppDatabase.kt` | При любом изменении схемы все данные удаляются |
| Split без транзакции | `TransactionFormViewModel.kt` | Если вторая операция не создалась, первая уже удалена |
| Пустые catch блоки | Несколько файлов | Ошибки проглатываются без логирования |

Без исправления этих багов остальные улучшения не имеют смысла — данные могут потеряться в любой момент.

---

## 🟠 Дублирование кода (Android)

| Что | Где | Объём | Решение |
|-----|-----|-------|---------|
| HTTP client (5 идентичных методов) | `SyncApiClient.kt` | ~125 строк | Обобщённый `makeRequest(path, method, body)` |
| Upsert (3 метода) | `SyncRepository.kt` | ~18 строк | Generic upsert с единым DAO интерфейсом |
| ViewModel State + validation | 8 ViewModel'ов | ~15 строк × 8 | `BaseViewModel<State>` с `updateState()` |
| TopAppBar/Scaffold | 7 экранов | ~8 строк × 7 | `FormScaffold` composable |
| IconPicker BottomSheet | 3 экрана | ~30 строк × 3 | `IconPickerBottomSheet` composable |
| UUID mapping | 3 маппера | ~3 строки × 3 | Extension-функция `String.toUUID()` / `UUID.toDbString()` |
| Репозитории в ViewModel | 8 ViewModel'ов | 1 строка × 8 | Hilt DI |

---

## 🟡 Архитектурные улучшения

| Улучшение | Эффект | Статус |
|-----------|--------|--------|
| **Hilt DI** | Убрать `MobileMoneyApp.getRepository()` из всех ViewModel. Поэтапная миграция | ❌ |
| **Разделение больших файлов** | `TransactionFormScreen.kt` (671 → ~150 строк), `Navigation.kt` (289 → ~80) | ❌ |
| **Server: слоистая архитектура** | `route/` разбит. Осталось: `controller/`, `dao/`, `middleware/` | ⚠️ Частично |
| **Server: Kotlin Serialization** | Заменить regex-парсинг JSON на `kotlinx.serialization` | ❌ |
| **Server: HikariCP pool** | Уже в зависимостях, настроить connection pooling | ❌ |
| **Server: Rate limiting** | In-memory по IP | ❌ |

---

## 🟢 Недостающее (нет в документах)

| Что | Описание | Приоритет |
|-----|----------|-----------|
| **Тесты** | В проекте 0 тестов. Нет юнит, интеграционных, UI-тестов | Высокий |
| **Линтер/Форматтер** | Нет `ktlint`, `detekt` — код без единого стандарта | Средний |
| **CI: прогон тестов** | `.github/workflows/android.yml` только собирает APK | Средний |
| **ErrorHandler** | Toast через `GlobalScope.launch` — возможны утечки | Низкий |
| **ProGuard/R8** | Нет правил минификации | Низкий |

---

## Подходы к реализации

### А) Инкрементальный (рекомендуется)

1. Баги (destructive migration, split, catch)
2. Дублирование (API Client, Upsert, BaseViewModel)
3. UI-компоненты (FormScaffold, IconPicker)
4. Hilt DI (по одному ViewModel)
5. Server (controller/dao/middleware)
6. Тесты

**Плюсы:** Безопасно, можно остановиться на любом этапе, минимум конфликтов.
**Минусы:** Дольше до полного результата.

### Б) Feature-ориентированный

Взять одну фичу (например, создание операции) и вычистить всё в её стеке:
баги → дублирование → Hilt → тесты.

**Плюсы:** Быстрая видимая польза в одном месте.
**Минусы:** Остальной проект остаётся в старом состоянии.

### В) Big-bang

Переписать всё сразу в отдельной ветке.

**Плюсы:** Единомоментный переход.
**Минусы:** Риск месяца без релиза, конфликты при мерже.
