# Архитектура приложения "Учет личных денег"

## 1. Общая архитектура: Clean Architecture + MVVM

```
app/
├── presentation/     # UI слой (Jetpack Compose + ViewModel)
├── domain/           # Бизнес-логика (UseCases, Entities, Repository Interfaces)
├── data/             # Data слой (Repository Impl, Remote/Local DataSources)
├── di/               # Dependency Injection (Hilt)
├── core/             # Утилиты, константы, расширения
└── security/         # Безопасность (шифрование, биометрия)
```

---

## 2. Domain слой (Бизнес-логика)

### 2.1 Сущности (Entities)

```kotlin
// Пользователь
data class User(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val name: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

// Счет
data class Account(
    val id: UUID,
    val name: String,
    val typeId: UUID?,
    val currencyCode: String,
    val icon: String?,
    val isDefault: Boolean = false, // использовать по умолчанию для новых операций
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

// Категория
data class Category(
    val id: UUID,
    val name: String,
    val isIncome: Boolean, // true=приход, false=расход
    val icon: String?,
    val parentId: UUID? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

// Тег (для группировки)
data class Tag(
    val id: UUID,
    val name: String,
    val color: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

// Операция
data class Transaction(
    val id: UUID,
    val accountId: UUID,
    val categoryId: UUID? = null,
    val amount: BigDecimal,
    val date: Instant,
    val comment: String?,
    val source: TransactionSource = TransactionSource.MANUAL, // MANUAL, SMS, PUSH
    val sourceData: String? = null, // Необработанные данные от источника
    val creatorId: UUID,
    val relatedTransactionId: UUID? = null, // Для переводов между счетами
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

// Валюта
data class Currency(
    val code: String, // RUB, USD, EUR (PK)
    val name: String,
    val symbol: String,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)

// Тип счета
data class AccountType(
    val id: UUID,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)
```

### 2.2 Типы источников операций

```kotlin
enum class TransactionSource { MANUAL, SMS, PUSH }
```

### 2.3 Repository Interfaces

```kotlin
interface UserRepository {
    fun getUser(userId: UUID): Flow<User?>
    suspend fun updateUser(user: User): User
    suspend fun deleteUser(userId: UUID) // Soft delete
    suspend fun logout()
}

interface AccountRepository {
    fun getAccounts(includeArchived: Boolean = false): Flow<List<Account>>
    suspend fun getAccountById(id: UUID): Account?
    suspend fun createAccount(wallet: Account): Account
    suspend fun updateAccount(wallet: Account): Account
    suspend fun deleteAccount(id: UUID) // Soft delete
    suspend fun archiveAccount(id: UUID)
}

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    fun getCategoriesByIncome(isIncome: Boolean): Flow<List<Category>>
    fun getCategoriesByParent(parentId: UUID?): Flow<List<Category>>
    suspend fun getCategoryById(id: UUID): Category?
    suspend fun createCategory(category: Category): Category
    suspend fun updateCategory(category: Category): Category
    suspend fun deleteCategory(id: UUID) // Soft delete
}

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    fun getTransactionsPaged(pageSize: Int, offset: Int): Flow<List<Transaction>>
    fun getTransactionsByPeriod(start: Instant, end: Instant): Flow<List<Transaction>>
    fun getTransactionsByAccount(walletId: UUID): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: UUID): Flow<List<Transaction>>
    fun getTransactionsByTags(tagIds: List<UUID>): Flow<List<Transaction>>
    suspend fun createTransaction(transaction: Transaction): Transaction
    suspend fun updateTransaction(transaction: Transaction): Transaction
    suspend fun deleteTransaction(id: UUID) // Soft delete
    suspend fun getTransactionBySourceId(sourceId: String): Transaction?
    suspend fun getRelatedTransaction(transactionId: UUID): Transaction?
}

interface TagRepository {
    fun getTags(): Flow<List<Tag>>
    suspend fun getTagById(id: UUID): Tag?
    suspend fun createTag(tag: Tag): Tag
    suspend fun updateTag(tag: Tag): Tag
    suspend fun deleteTag(id: UUID) // Soft delete
}

interface ReportRepository {
    fun getCurrentBalance(walletId: UUID): Flow<BigDecimal>
    fun getBalanceByAccount(userId: UUID, walletId: UUID, start: Instant, end: Instant): Flow<BigDecimal>
    fun getTotalBalance(userId: UUID): Flow<Map<Currency, BigDecimal>>
    fun getExpensesByCategory(userId: UUID, start: Instant, end: Instant): Flow<Map<Category, BigDecimal>>
    fun getIncomesByCategory(userId: UUID, start: Instant, end: Instant): Flow<Map<Category, BigDecimal>>
    fun getAccountSummary(userId: UUID, start: Instant, end: Instant): Flow<List<AccountSummary>>
    fun getCategorySummary(userId: UUID, start: Instant, end: Instant): Flow<List<CategorySummary>>
}

interface ExchangeRateRepository {
    fun getRates(baseCurrency: String): Flow<Map<String, BigDecimal>>
    suspend fun updateRates()
    fun getRate(from: String, to: String): Flow<BigDecimal>
    fun isRatesExpired(): Boolean
}

interface PreferencesRepository {
    fun getPreferences(): Flow<AppPreferences>
    suspend fun updatePreferences(preferences: AppPreferences)
}

data class AppPreferences(
    val baseCurrency: String = "RUB",
    val biometricEnabled: Boolean = false,
    val smsAutoImportEnabled: Boolean = false,
    val notificationAutoImportEnabled: Boolean = false,
    val theme: String = "SYSTEM"
)
```

### 2.4 Use Cases

```kotlin
// Пользователь
class GetUserUseCase(private val repository: UserRepository)
class UpdateUserUseCase(private val repository: UserRepository)
class LogoutUseCase(private val repository: UserRepository)

// Кошельки
class GetAccountsUseCase(private val repository: AccountRepository)
class CreateAccountUseCase(private val repository: AccountRepository)
class UpdateAccountUseCase(private val repository: AccountRepository)
class DeleteAccountUseCase(private val repository: AccountRepository)
class GetAccountBalanceUseCase(private val reportRepository: ReportRepository)

// Категории
class GetCategoriesUseCase(private val repository: CategoryRepository)
class GetCategoriesByIncomeUseCase(private val repository: CategoryRepository)
class CreateCategoryUseCase(private val repository: CategoryRepository)
class UpdateCategoryUseCase(private val repository: CategoryRepository)
class DeleteCategoryUseCase(private val repository: CategoryRepository)

// Операции
class CreateTransactionUseCase(private val repository: TransactionRepository)
class GetTransactionsUseCase(private val repository: TransactionRepository)
class GetTransactionsByPeriodUseCase(private val repository: TransactionRepository)
class UpdateTransactionUseCase(private val repository: TransactionRepository)
class DeleteTransactionUseCase(private val repository: TransactionRepository)

// Отчеты
class GetAccountSummaryUseCase(private val repository: ReportRepository)
class GetCategorySummaryUseCase(private val repository: ReportRepository)
class GetTotalBalanceUseCase(private val repository: ReportRepository)

// Теги
class ManageTagsUseCase(private val repository: TagRepository)

// Курсы валют
class GetExchangeRatesUseCase(private val repository: ExchangeRateRepository)
class UpdateExchangeRatesUseCase(private val repository: ExchangeRateRepository)

// Настройки
class GetPreferencesUseCase(private val repository: PreferencesRepository)
class UpdatePreferencesUseCase(private val repository: PreferencesRepository)
```

### 2.5 Domain Aggregates

```kotlin
// Кошелек с вычисленным балансом
data class AccountWithBalance(
    val wallet: Account,
    val balance: BigDecimal
)

// Категория с суммой операций за период
data class CategoryWithSum(
    val category: Category,
    val totalAmount: BigDecimal
)

// Операция с прикрепленными тегами
data class TransactionWithTags(
    val transaction: Transaction,
    val tags: List<Tag>
)
```

### 2.6 Domain Events

```kotlin
sealed class TransactionEvent {
    data class Created(val transaction: Transaction) : TransactionEvent()
    data class Updated(val transaction: Transaction) : TransactionEvent()
    data class Deleted(val id: UUID) : TransactionEvent()
}

sealed class AccountEvent {
    data class BalanceChanged(val walletId: UUID, val newBalance: BigDecimal) : AccountEvent()
}

sealed class SyncEvent {
    data object Started : SyncEvent()
    data class Completed(val syncedCount: Int) : SyncEvent()
    data class Failed(val error: Throwable) : SyncEvent()
}
```

### 2.7 Result Wrapper

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception, message)
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Throwable, String?) -> Unit): Result<T> {
        if (this is Error) action(exception, message)
        return this
    }
}

inline fun <T> runCatchingResult(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e, e.message)
    }
}
```

---

## 3. Data слой

### 3.1 Remote DataSource (API)

```kotlin
interface RemoteDataSource {
    // Auth
    suspend fun login(credentials: LoginRequest): AuthResponse
    suspend fun register(data: RegisterRequest): AuthResponse
    suspend fun refreshToken(token: String): AuthResponse
    
    // User
    suspend fun getUser(): UserDto
    suspend fun updateUser(user: UserDto): UserDto
    
    // Sync
    suspend fun syncAccounts(wallets: List<AccountDto>): SyncResponse
    suspend fun syncCategories(categories: List<CategoryDto>): SyncResponse
    suspend fun syncTransactions(transactions: List<TransactionDto>): SyncResponse
    suspend fun getChanges(since: Instant): ChangesResponse
    
    // Exchange Rates
    suspend fun getExchangeRates(): ExchangeRatesResponse
}
```

### 3.2 Local DataSource (Room)

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val password: String,
    val name: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "wallets")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val accountTypeId: String? = null,
    val currencyCode: String,
    val icon: String?,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isIncome: Boolean,
    val icon: String?,
    val parentId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val walletId: String,
    val categoryId: String? = null,
    val amount: String,
    val date: Long,
    val comment: String?,
    val source: String,
    val sourceData: String? = null,
    val creatorId: String,
    val relatedTransactionId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "category_tags",
    primaryKeys = ["categoryId", "tagId"]
)
data class CategoryTagCrossRef(val categoryId: String, val tagId: String)

@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transactionId", "tagId"]
)
data class TransactionTagCrossRef(val transactionId: String, val tagId: String)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val id: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: String,
    val date: Long
)

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey val code: String,
    val name: String,
    val symbol: String,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "account_types")
data class AccountTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
```

### 3.3 Repository Implementation

```kotlin
class AccountRepositoryImpl(
    private val localDataSource: AccountLocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    private val syncManager: SyncManager
) : AccountRepository {
    // Реализация с offline-first подходом
}
```

---

## 4. Presentation слой (UI)

### 4.1 Структура экранов

```
presentation/
├── navigation/
│   ├── AppNavigation.kt
│   └── Screen.kt
├── screens/
│   ├── auth/
│   │   ├── LoginScreen.kt
│   │   └── RegisterScreen.kt
│   ├── main/
│   │   ├── HomeScreen.kt (дашборд)
│   │   ├── AccountListScreen.kt
│   │   ├── AccountDetailScreen.kt
│   │   ├── AddAccountScreen.kt
│   │   ├── CategoryListScreen.kt
│   │   ├── AddCategoryScreen.kt
│   │   ├── TransactionListScreen.kt
│   │   ├── AddTransactionScreen.kt
│   │   ├── TransferScreen.kt
│   │   ├── ReportsScreen.kt
│   │   ├── TagsScreen.kt
│   │   └── SettingsScreen.kt
│   └── autoimport/
│       ├── SmsImportScreen.kt
│       ├── NotificationImportScreen.kt
│       └── AutoTransactionScreen.kt
├── components/
│   ├── AccountCard.kt
│   ├── CategoryChip.kt
│   ├── TransactionItem.kt
│   ├── BalanceDisplay.kt
│   ├── DateRangePicker.kt
│   └── ChartComponent.kt
├── viewmodels/
│   ├── AuthViewModel.kt
│   ├── HomeViewModel.kt
│   ├── AccountViewModel.kt
│   ├── CategoryViewModel.kt
│   ├── TransactionViewModel.kt
│   ├── ReportViewModel.kt
│   ├── SettingsViewModel.kt
│   └── AutoImportViewModel.kt
└── theme/
    ├── Theme.kt
    ├── Color.kt
    └── Typography.kt
```

### 4.2 ViewModel State

```kotlin
data class HomeUiState(
    val totalBalance: Map<Currency, BigDecimal> = emptyMap(),
    val wallets: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredByAccount: UUID? = null,
    val filteredByCategory: UUID? = null,
    val dateRange: DateRange? = null,
    val filteredByTags: List<UUID> = emptyList(),
    val isLoading: Boolean = false
)

data class ReportUiState(
    val period: DateRange = DateRange(),
    val walletSummaries: List<AccountSummary> = emptyList(),
    val categorySummaries: List<CategorySummary> = emptyList(),
    val expenseByCategory: Map<Category, BigDecimal> = emptyMap(),
    val incomeByCategory: Map<Category, BigDecimal> = emptyMap(),
    val isLoading: Boolean = false
)
```

### 4.3 Paging Integration

```kotlin
class TransactionListViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    val transactions: Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            prefetchDistance = 5
        ),
        pagingSourceFactory = { TransactionPagingSource(transactionRepository) }
    ).flow.cachedIn(viewModelScope)
}

class TransactionPagingSource(
    private val repository: TransactionRepository
) : PagingSource<Int, Transaction>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
        return try {
            val page = params.key ?: 0
            val data = repository.getTransactionsPaged(
                userId = currentUserId,
                pageSize = params.loadSize,
                offset = page * params.loadSize
            )
            LoadResult.Page(
                data = data,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (data.isEmpty()) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
```

---

## 5. Автоматизация операций (SMS/Push)

### 5.1 SMS Listener (Restricted Permission)

```kotlin
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Sms.Intents.getMessagesFromIntent(intent)
            // Обработка SMS и создание транзакции
        }
    }
}
```

> **Важно:** `READ_SMS` — restricted permission на Android 14+. Google Play может отклонить приложение.
> Для sideload/личного использования работает. Для публикации в Google Play рекомендуется Notification Listener.

### 5.2 Notification Listener (Fallback для push от банков)

```kotlin
class BankNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras?.getString(Notification.EXTRA_TITLE)
        val text = extras?.getString(Notification.EXTRA_TEXT)
        // Парсинг уведомления банка и создание транзакции
    }
}
```

### 5.3 Push Listener (Firebase)

```kotlin
class PushMessageService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Обработка push-уведомлений от собственного сервера
        val payload = remoteMessage.data
        // Синхронизация данных
    }
}
```

### 5.4 Parser SMS/Push

```kotlin
interface TransactionParser {
    fun parseSms(sender: String, body: String): ParsedTransaction?
    fun parseNotification(packageName: String, title: String, body: String): ParsedTransaction?
    fun parsePush(title: String, body: String): ParsedTransaction?
}

class BankSmsParser : TransactionParser {
    // Парсинг SMS от известных банков (Сбербанк, Тинькофф, Альфа и др.)
    // Определение типа операции, суммы, валюты
}
```

### 5.5 Permission Handling

```kotlin
object PermissionManager {
    fun requestSmsPermission(activity: Activity)
    fun requestNotificationPermission(activity: Activity)
    fun requestNotificationListener(context: Context)
    fun checkSmsPermission(): Boolean
    fun checkNotificationPermission(): Boolean
    fun isNotificationListenerEnabled(context: Context): Boolean
}
```

---

## 6. Безопасность

### 6.1 Требования

- Шифрование данных (EncryptedSharedPreferences, Room с SQLCipher или androidx.security:security-crypto)
- Биометрическая аутентификация
- Logout с очисткой данных

### 6.2 Security Module

```kotlin
interface SecurityManager {
    fun encrypt(data: String): String
    fun decrypt(encrypted: String): String
    fun getSecureKey(): String
    fun isBiometricEnabled(): Boolean
    fun authenticateWithBiometric(onSuccess: () -> Unit, onError: (Exception) -> Unit)
    fun clearSensitiveData()
    fun getEncryptedDatabaseKey(): ByteArray
}
```

---

## 7. Синхронизация

### 7.1 Sync Strategy: Offline-First

```
1. Запись в локальную БД (Room)
2. Пометка как "несинхронизировано" (isSynced = false)
3. Фоновая синхронизация с сервером через WorkManager
4. Разрешение конфликтов: last-write-wins по updatedAt
   - Все Entity имеют updatedAt: Long
   - При конфликте побеждает запись с более свежим updatedAt
   - Сервер возвращает актуальные изменения с момента lastSync
5. Optimistic Locking через поле version
   - При обновлении инкрементируется version
   - Если version на сервере выше - конфликт
```

### 7.2 WorkManager

```kotlin
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val remoteDataSource: RemoteDataSource
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        // Синхронизация изменений с сервера
    }
}
```

---

## 8. Технологический стек

| Компонент | Технология | Версия |
|-----------|------------|--------|
| Language | Kotlin | 2.3.21 |
| Gradle | | 9.4.1 |
| AGP | Android Gradle Plugin | 9.2.0 |
| Min SDK | API 34 (Android 14) | |
| UI | Jetpack Compose + Material 3 | BOM 2024.02.00 |
| Compose Compiler Plugin | Kotlin | 2.3.21 |
| Lifecycle | | 2.10.0 |
| Navigation Compose | | 2.9.8 |
| DI | Hilt | |
| Networking | Retrofit + OkHttp + Kotlin Serialization | |
| Local DB | Room + SQLCipher (или androidx.security:security-crypto) | |
| Async | Kotlin Coroutines + Flow | |
| Networking | Retrofit + OkHttp + Kotlin Serialization | |
| Charts | Vico | |
| Auth | Firebase Auth | |
| Push | Firebase Cloud Messaging | |
| Background | WorkManager | |
| Security | BiometricPrompt, EncryptedSharedPreferences | |
| Pagination | Paging 3 | |
| Testing | JUnit, MockK, Turbine | |

---

## 9. База данных (Room Schema)

См. [db_schema.md](../docs/db_schema.md)

Баланс кошелька вычисляется из транзакций:
  SUM(INCOME) - SUM(EXPENSE) по wallet_id

## 10. API Endpoints (Backend)

```
POST   /api/v1/auth/login
POST   /api/v1/auth/register
POST   /api/v1/auth/refresh

GET    /api/v1/user
PUT    /api/v1/user

GET    /api/v1/wallets
POST   /api/v1/wallets
PUT    /api/v1/wallets/{id}
DELETE /api/v1/wallets/{id}

GET    /api/v1/categories
POST   /api/v1/categories
PUT    /api/v1/categories/{id}
DELETE /api/v1/categories/{id}

GET    /api/v1/transactions
POST   /api/v1/transactions
PUT    /api/v1/transactions/{id}
DELETE /api/v1/transactions/{id}

GET    /api/v1/tags
POST   /api/v1/tags
DELETE /api/v1/tags/{id}

GET    /api/v1/exchange-rates

GET    /api/v1/sync/changes?since={timestamp}
POST   /api/v1/sync/push
```

---

## 11. Ключевые сценарии

### 11.1 Создание операции расхода
1. Пользователь выбирает кошелек, категорию, вводит сумму
2. Создание транзакции в Room
3. Баланс кошелька вычисляется из транзакций (SUM INCOME - SUM EXPENSE)
4. Синхронизация с сервером (фоново)

### 11.2 Автоматическое создание из SMS
1. Приложение получает SMS
2. Парсинг текста (поиск суммы, валюты)
3. Поиск кошелька по ключевым свойствам
4. Категория подбирается по информации из текста, либо без категории
5. Создание транзакции без подтверждения

### 11.3 Отчет за период
1. Выбор периода (дата начало/конец)
2. Запрос транзакций за период
3. Группировка по категориям/кошелькам
4. Расчет итогов
5. Построение графиков
