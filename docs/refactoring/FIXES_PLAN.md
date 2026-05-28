# План исправлений

## CRITICAL

### 1. Нет изоляции данных между пользователями
**Файлы:** `server/src/main/kotlin/com/mobilemoney/server/route/SyncRoute.kt`

**Проблема:** Аутентифицированное устройство может получить ВСЕ транзакции/аккаунты/категории ВСЕХ пользователей.

**Исправление:**
```kotlin
// SyncRoute.kt - добавить фильтрацию по login
suspend fun getChanges(call: ApplicationCall) {
    val login = authenticate(call) ?: return
    val since = call.parameters["since"]?.toLongOrNull() ?: 0

    val accounts = accountRepository.getUpdatedSince(since).filter { it.creatorId == login }
    val categories = categoryRepository.getUpdatedSince(since).filter { it.creatorId == login }
    val transactions = transactionRepository.getUpdatedSince(since).filter { it.creatorId == login }

    call.respond(SyncChangesResponse(accounts, categories, transactions))
}
```

### 2. Потеря parentId при обновлении категории
**Файл:** `android/src/main/java/com/mobilemoney/data/local/CategoryMapper.kt:21`

**Проблема:** `parentId = null` всегда — иерархия категорий ломается при обновлении.

**Исправление:**
```kotlin
// 1. Добавить parentId в CategoryUi (data/model/Models.kt)
data class CategoryUi(
    ...
    val parentId: String? = null  // ДОБАВИТЬ
)

// 2. Обновить CategoryMapper.kt
fun CategoryUi.toEntity(): CategoryEntity {
    return CategoryEntity(
        ...
        parentId = this.parentId,  // ИЗМЕНИТЬ с null на this.parentId
        ...
    )
}
```

### 3. Sync conflict resolution bug
**Файл:** `android/src/main/java/com/mobilemoney/data/repository/SyncRepository.kt:222-238`

**Проблема:** `syncedAt != null` блокирует любые обновления с сервера после первой синхронизации.

**Исправление:**
```kotlin
private suspend fun upsertAccount(dto: AccountDto, syncedAt: Long) {
    val existing = accountDao.getAccountById(dto.id)
    if (existing == null) {
        accountDao.insert(dto.toEntity(syncedAt))
    } else if (existing.updatedAt < dto.updatedAt) {
        // Last-write-wins: обновить если серверные данные новее
        accountDao.update(dto.toEntity(syncedAt))
    }
}
```

---

## HIGH

### 4. JSON injection в AuthRoute
**Файл:** `server/src/main/kotlin/com/mobilemoney/server/route/AuthRoute.kt:43`

**Проблема:** `loginRequest.login` вставляется напрямую в JSON.

**Исправление:**
```kotlin
// Использовать kotlinx.serialization вместо строковой интерполяции
data class AuthResponse(val token: String?, val login: String)

call.respondText(
    contentType = ContentType.Application.Json
) {
    json.encodeToString(AuthResponse(token, loginRequest.login))
}
```

### 5. Потеря состояния archived при обновлении счета
**Файл:** `android/src/main/java/com/mobilemoney/data/local/AccountMapper.kt:27`

**Проблема:** `archived = false` захардкожено.

**Исправление:**
```kotlin
fun AccountUi.toEntity(): AccountEntity {
    return AccountEntity(
        ...
        archived = this.archived,  // ИЗМЕНИТЬ
        ...
    )
}
```

### 6. Раскрытие внутренней информации в ошибках
**Файл:** `server/src/main/kotlin/com/mobilemoney/server/route/SyncRoute.kt:41,66,90`

**Исправление:**
```kotlin
try {
    // operation
} catch (e: Exception) {
    logger.error("Sync error", e)  // Логировать на сервере
    call.respond(HttpStatusCode.InternalServerError, "Sync failed")  // generic message клиенту
}
```

### 7. Пустые catch блоки
**Файл:** `android/viewmodel/TransactionFormViewModel.kt:61,72`

**Исправление:**
```kotlin
getAccountsUseCase()
    .catch { e ->
        _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to load accounts")
    }
    .collect { ... }
```

---

## MEDIUM

### 8. Два файла Database.kt (дублирование)
**Файлы:**
- `server/src/main/kotlin/com/mobilemoney/server/Database.kt`
- `server/src/main/kotlin/com/mobilemoney/server/repository/Database.kt`

**Исправление:** Удалить один из файлов. Рекомендуется оставить `server/Database.kt` (основной).

### 9. Дублирование кода в репозиториях
**Файлы:** `server/src/main/kotlin/com/mobilemoney/server/repository/*.kt`

**Исправление:** Создать базовый класс:
```kotlin
abstract class BaseRepository(private val db: Database) {
    protected fun <T> query(sql: String, block: ResultSet.() -> T): List<T> {
        return db.connection.use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeQuery().let { rs ->
                    generateSequence { if (rs.next()) rs.block() else null }.toList()
                }
            }
        }
    }
}
```

### 10. Дублирование в Room Mapper'ах
**Файлы:** `android/src/main/java/com/mobilemoney/data/local/*Mapper.kt`

**Исправление:** Использовать统一的扩展函数模式 или кодогенерацию.

### 11. Дублирование в FormViewModel
**Файлы:** `android/viewmodel/*FormViewModel.kt`

**Исправление:** Создать базовый класс с generic update:
```kotlin
abstract class BaseFormViewModel<S : UiState>(initialState: S) : ViewModel() {
    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    protected fun updateState(reducer: S.() -> S) {
        _uiState.value = _uiState.value.reducer()
    }
}
```

---

## LOW

### 12. Магические числа — цвета транзакций
**Файлы:** `TransactionMapper.kt`, `TransactionFormViewModel.kt`, `TransactionListScreen.kt`

**Исправление:** Вынести в `Theme.kt` или `Colors.kt`:
```kotlin
object TransactionColors {
    val INCOME = Color(0xFF2E7D32)
    val EXPENSE = Color(0xFFD32F2F)
    val TRANSFER = Color(0xFF9C27B0)
}
```

### 13. Магические числа — временные константы
**Файл:** `android/src/main/java/com/mobilemoney/data/local/TransactionMapper.kt:41-48`

**Исправление:**
```kotlin
companion object {
    private const val MILLIS_PER_DAY = 86400000L
    private const val TWO_DAYS_MILLIS = 172800000L
}
```

### 14. Захардкоженные таймауты
**Файл:** `android/src/main/java/com/mobilemoney/data/remote/SyncApiClient.kt`

**Исправление:** Вынести в конфиг:
```kotlin
object NetworkConfig {
    const val CONNECT_TIMEOUT = 30_000L
    const val READ_TIMEOUT = 30_000L
    const val PING_TIMEOUT = 10_000L
}
```

### 15. Отсутствующий индекс на deletedAt
**Файл:** `android/src/main/java/com/mobilemoney/data/local/Entities.kt`

**Исправление:**
```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["relatedTransactionId"]),
        Index(value = ["deletedAt"])  // ДОБАВИТЬ
    ]
)
class TransactionEntity { ... }
```

### 16. TODO комментарии
**Файлы:**
- `TransactionListViewModel.kt:58` — inject delete use case
- `TransactionFormViewModel.kt:342` — inject delete use case

---

## План выполнения

### Фаза 1 (CRITICAL) — 1-2 дня
- [ ] Исправить изоляцию данных в SyncRoute.kt
- [ ] Исправить CategoryMapper.kt (parentId)
- [ ] Исправить SyncRepository.kt (conflict resolution)

### Фаза 2 (HIGH) — 1 день
- [ ] Исправить JSON injection в AuthRoute
- [ ] Исправить AccountMapper.kt (archived)
- [ ] Заменить пустые catch блоки
- [ ] Скрыть ошибки от клиента

### Фаза 3 (MEDIUM) — 1-2 дня
- [ ] Удалить дубликат Database.kt
- [ ] Рефакторинг репозиториев с BaseRepository
- [ ] Рефакторинг FormViewModel с базовым классом

### Фаза 4 (LOW) — 0.5 дня
- [ ] Вынести магические числа в константы
- [ ] Добавить индекс на deletedAt
- [ ] Удалить TODO комментарии
- [ ] Документировать исправления
