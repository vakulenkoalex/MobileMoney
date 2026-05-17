# Code Review: Анализ дублирования и архитектуры

## Архитектура проекта

### Android (Clean Architecture + MVVM)

```
presentation (ui/screens, viewmodel)
       ↓
domain (data/model - UI models)
       ↓
data (repository, local/Room, remote/Ktor)
```

**Проблемы архитектуры:**

1. **Смешанная структура** - `viewmodel` лежит отдельно от `ui/screens`, что нарушает классическую чистую архитектуру (presentation слой разбит)
2. **Manual DI** - в `MobileMoneyApp.kt` создаются все зависимости вручную, нет DI-фреймворка (хотя в AGENTS.md указано Hilt)
3. **Репозитории пересекаются** - `DatabaseRepository` и `SyncRepository` делают похожие операции

### Server (Monolithic)

```
Application.kt (routes) → AuthService/SyncService → Database.kt (JDBC)
```

## Дублирование кода

### Критические (приоритет 1)

#### 1. API Client HTTP pattern (SyncApiClient.kt)

5 методов с полностью идентичной структурой:

```kotlin
// Lines 35-72 (login), 74-97 (getChanges), 99-132 (pushChanges), 134-157 (pullAll), 165-187 (ping)
return withContext(Dispatchers.IO) {
    try {
        val url = URL("$baseUrl/...")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "..."
        conn.setRequestProperty("...")
        conn.connectTimeout = 30000
        conn.readTimeout = 30000
        val responseCode = conn.responseCode
        if (responseCode == 200) {
            Result.success(...)
        } else {
            Result.failure(Exception("$responseCode: ..."))
        }
    } catch (e: Exception) {
        Result.failure(Exception("500: ${e.message}"))
    }
}
```

**Объём:** ~125 строк повторяющегося кода

---

#### 2. Upsert pattern (SyncRepository.kt:215-234)

Три идентичных метода:

```kotlin
private suspend fun upsertAccount(dto: AccountDto, syncedAt: Long) {
    val existing = accountDao.getAccountById(dto.id)
    if (existing == null || existing.updatedAt < dto.updatedAt) {
        accountDao.insert(dto.toEntity().copy(syncedAt = syncedAt))
    }
}

private suspend fun upsertCategory(dto: CategoryDto, syncedAt: Long) { /* IDENTICAL */ }
private suspend fun upsertTransaction(dto: TransactionDto, syncedAt: Long) { /* IDENTICAL */ }
```

**Объём:** ~18 строк повторяющегося кода

---

### Средние (приоритет 2)

#### 3. ViewModel State Management

Все 8 ViewModel'ов дублируют паттерн:

```kotlin
data class FormState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)
```

**Файлы:** AccountFormViewModel, AccountListViewModel, CategoryFormViewModel, CategoryListViewModel, TransactionListViewModel, TransactionFormViewModel, SettingsViewModel, LoginViewModel

---

#### 4. Mapper UUID conversion

```kotlin
// Повторяется 6+ раз:
UUID.fromString(id)  // Entity -> UI
id.toString()        // UI -> Entity
```

**Файлы:** AccountMapper.kt, CategoryMapper.kt, TransactionMapper.kt

---

#### 5. TopAppBar/Scaffold UI

~8 строк повторяются в 7 экранах:

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text(...) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
)
```

**Файлы:** AccountListScreen, AccountFormScreen, CategoryListScreen, CategoryFormScreen, TransactionListScreen, TransactionFormScreen, SettingsScreen

---

#### 6. Icon grid selection

~30 строк повторяются в 3 экранах:

```kotlin
ModalBottomSheet(
    modifier = Modifier.fillMaxHeight(0.5f),
    onDismissRequest = { showBottomSheet = false }
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { ... }
}
```

**Файлы:** AccountFormScreen.kt (210-242), CategoryFormScreen.kt (139-171), TransactionFormScreen.kt

---

#### 7. Form validation

```kotlin
private fun validate(): Boolean {
    val state = _uiState.value
    if (state.name.isBlank()) {
        _uiState.value = state.copy(error = "Название не может быть пустым")
        return false
    }
    return true
}
```

**Файлы:** AccountFormViewModel.kt:93-99, CategoryFormViewModel.kt:72-78, TransactionFormViewModel.kt:169-196

---

### Низкие (приоритет 3)

#### 8. Repository Initialization в ViewModels

```kotlin
private val repository: DatabaseRepository = MobileMoneyApp.getRepository(application)
```

Повторяется во всех ViewModel'ах с формами.

---

## Рекомендации по улучшению

### 1. Рефакторинг API Client

```kotlin
// SyncApiClient.kt
private suspend inline fun <reified T> makeRequest(
    path: String,
    method: String = "GET",
    crossinline bodyProvider: (() -> String)? = null
): Result<T> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("$baseUrl$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = method
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 30000
                readTimeout = 30000
            }
            bodyProvider?.invoke()?.let { conn.outputStream.write(it.toByteArray()) }
            
            val responseCode = conn.responseCode
            val body = conn.inputStream.bufferedReader().readText()
            
            if (responseCode == 200) {
                Result.success(json.decodeFromString<T>(body))
            } else {
                Result.failure(Exception("$responseCode: $body"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("500: ${e.message}"))
        }
    }
}
```

---

### 2. Обобщённый Upsert

```kotlin
// SyncRepository.kt
private suspend fun <T : Any> upsertEntity(
    dto: T,
    dao: BaseDao<T>,
    dtoIdExtractor: (T) -> String,
    dtoUpdatedAtExtractor: (T) -> Long,
    toEntity: (T) -> T
) {
    val id = dtoIdExtractor(dto)
    val existing = when (dao) {
        is AccountDao -> dao.getAccountById(id)
        is CategoryDao -> dao.getCategoryById(id)
        is TransactionDao -> dao.getTransactionById(id)
        else -> null
    }
    
    if (existing == null || existing.updatedAt < dtoUpdatedAtExtractor(dto)) {
        dao.insert(toEntity(dto).copy(syncedAt = System.currentTimeMillis()))
    }
}
```

Или через reflection (менее типобезопасно, но короче):

```kotlin
private suspend fun <T : SyncDto> upsertGeneric(dto: T, syncedAt: Long) {
    val id = dto.id
    val updatedAt = dto.updatedAt
    // Универсальная логика через when exhaustive
}
```

---

### 3. Базовый ViewModel

```kotlin
// BaseViewModel.kt
abstract class BaseViewModel<State>(initialState: State) : ViewModel() {
    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()
    
    protected inline fun updateState(reducer: State.() -> State) {
        _uiState.value = _uiState.value.reducer()
    }
    
    protected fun setLoading(state: State.(Boolean) -> State) {
        updateState { state(true) }
    }
    
    protected fun setError(message: String?, state: State.(String?) -> State) {
        updateState { state(message) }
    }
}
```

---

### 4. UI-компоненты для форм

```kotlin
// FormScaffold.kt
@Composable
fun FormScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) { content() }
    }
}

// IconPickerBottomSheet.kt
@Composable
fun IconPickerBottomSheet(
    icons: List<IconOption>,
    selectedIcon: String?,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) { ... }

// SelectionBottomSheet.kt
@Composable
fun <T> SelectionBottomSheet(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    onDismiss: () -> Unit
) { ... }
```

---

### 6. DI (опционально)

Если планируется использовать Hilt (как указано в AGENTS.md):

```kotlin
// MobileMoneyApp.kt
@HiltAndroidApp
class MobileMoneyApp : Application()

// ViewModel
@HiltViewModel
class AccountFormViewModel @Inject constructor(
    private val repository: DatabaseRepository
) : ViewModel() { ... }
```

---

## Оценка трудозатрат

| Приоритет | Задача | Время | Статус |
|-----------|--------|-------|--------|
| 1 | API Client рефакторинг | ~1 час | |
| 1 | Upsert generalization | ~30 минут | |
| 2 | Base ViewModel | ~1 час | |
| 2 | UI компоненты (FormScaffold, IconPicker) | ~2 часа | |
| 2 | Mapper utilities | ~30 минут | |
| 3 | Hilt DI | ~2 часа | |

**Итого:** ~7 часов для полного рефакторинга

---

## Приоритеты реализации

1. **Только критические** (API Client + Upsert) - ~2 часа
2. **Критические + средние** - ~5 часов
3. **Включая архитектурные** - ~9 часов
