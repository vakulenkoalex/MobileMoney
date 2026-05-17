# Architectural Refactoring Design

## Date: 2026-05-13

## Goal

Full architectural refactoring of both Android and Server projects using incremental approach (recommended).

---

## Problem Statement

Current state analysis:

- **Hilt declared but not implemented**: Manual DI via `MobileMoneyApp.getRepository()`
- **Large monolith files**: TransactionFormScreen (671 lines), Navigation (289 lines)
- **DTO duplication**: AccountDto, CategoryDto, TransactionDto exist in both Android and Server with different types
- **Critical bugs**: `fallbackToDestructiveMigration(dropAllTables = true)`, split transactions without rollback
- **Zero tests**: No test files in either project

---

## Selected Approach

**Incremental Structural Refactoring** — refactor bottom-up without breaking existing functionality.

---

## Section 1: Common Module (DTO)

**Problem:** `AccountDto`, `CategoryDto`, `TransactionDto`, `CurrencyDto` duplicated in `android/.../SyncApiClient.kt` and `server/.../SyncDto.kt` with different field types (`isDefault: Int` vs `isDefault: Boolean`).

**Solution:** Create `common/` as plain Kotlin/JVM module:

```
common/
├── build.gradle.kts
└── src/main/kotlin/com/mobilemoney/dto/
    ├── AccountDto.kt
    ├── CategoryDto.kt
    ├── TransactionDto.kt
    ├── CurrencyDto.kt
    └── SyncRequests.kt  (SyncPushRequest, SyncPullResponse)
```

**Details:**
- All DTOs with `@Serializable`, unified types (`Boolean`, not `Int`)
- No dependencies except `kotlinx-serialization`
- Both projects include `implementation(project(":common"))`
- Mapping `Int ↔ Boolean` remains in Android mappers (Room Entity uses Int, not Boolean)

---

## Section 3: Android — Hilt DI Integration

**Current:** Manual DI via `MobileMoneyApp.getRepository()`. All ViewModels create dependencies manually.

**Target:** Hilt with gradual migration:

1. **Add dependencies** in `build.gradle.kts`:
```kotlin
implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-compiler:2.51.1")
```

2. **Annotations:**
```kotlin
@HiltAndroidApp
class MobileMoneyApp : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity()

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val syncRepository: SyncRepository
) : ViewModel()
```

3. **Transitional adapter**: Keep current `DatabaseRepository` implementations, but inject via `@Provides` in Hilt module:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    fun provideDatabaseRepository(...): DatabaseRepository = ...
}
```

4. **Migrate ViewModels one by one**: First annotate with `@HiltViewModel`, add constructor `@Inject`, remove manual creation.

**Benefit:** Gradual migration without big-bang. Each ViewModel can be migrated separately and verified.

---

## Section 4: Android — Split Large Files

**Problem:**
- `TransactionFormScreen.kt` — 671 lines (everything in one file)
- `TransactionFormViewModel.kt` — 352 lines
- `Navigation.kt` — 289 lines (routes + NavHost + Scaffold + login gate)

**Solution:**

| File | Split into |
|------|------------|
| `TransactionFormScreen.kt` | `TransactionFormScreen.kt` (wrapper) + `TransactionFormContent.kt` (UI) + `AccountPicker.kt` + `CategoryPicker.kt` + `DatePicker.kt` + `SplitTransactionSheet.kt` |
| `TransactionFormViewModel.kt` | + extract logic to Use Cases, keep only state management |
| `Navigation.kt` | `NavigationRoutes.kt` (routes sealed class) + `AppNavHost.kt` (NavHost) + `AppScaffold.kt` (Scaffold + bottom bar) + `LoginGate.kt` |
| `AccountFormScreen.kt` | + `AccountFormContent.kt` |

**Additional:**
- Extract common UI components: `FormScaffold.kt`, `IconPickerBottomSheet.kt`, `ValidationMessage.kt`
- Avoid duplicating TopAppBar/Scaffold (7 screens)

---

## Section 5: Server — Layered Architecture ✅ PARTIALLY DONE

**Already created:**
- `server/service/AuthService.kt`
- `server/service/SyncService.kt`
- `server/repository/` — Database, AccountRepository, CategoryRepository, TransactionRepository, DeviceRepository, UserRepository
- `server/route/AuthRoute.kt`
- `server/route/SyncRoute.kt`
- `server/route/HealthRoute.kt`

**Remaining:**
```
server/src/main/kotlin/com/mobilemoney/server/
├── controller/          # rename route → controller
├── dao/                 # extract from repository
├── middleware/          # ErrorHandling, RequestLogging, RateLimiting
├── model/dto/           # refactor to separate from model/entity
└── Application.kt      # cleanup
```

**Key changes:**

1. **Kotlinx.serialization** instead of manual parsing:
```kotlin
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String, val userId: String)
```

2. **Proper JSON** — remove regex parsing, use `kotlinx.serialization.json`

3. **Rate limiting** — simple in-memory based on IP

4. **Connection pooling** — use HikariCP (already in dependencies):
```kotlin
val config = HikariConfig().apply {
    jdbcUrl = "jdbc:sqlite:data/sync.db"
    maximumPoolSize = 10
}
val ds = HikariDataSource(config)
```

5. **Middleware** — centralized error handling, logging

---

## Section 6: Critical Bug Fixes (During Refactoring)

| Bug | Fix |
|-----|-----|
| `fallbackToDestructiveMigration` | Replace with `fallbackToDestructiveMigration()` without `dropAllTables = true`, add migrations |
| Split transactions without rollback | Wrap in Room `transaction { }` — if second `addTransaction` fails, first delete will be rolled back |
| Empty catch block | Add `Result.failure(e)`, logging, user-facing toast "Ошибка сохранения" |
| HashUtil — SHA-512 named as SHA-256 | Rename to `sha512()` or fix logic |

---

## Section 7: Execution Order

```
1. common/      → create module, move DTOs, link in both projects
3. Android Hilt  → add dependencies, @HiltAndroidApp, @HiltViewModel, module
4. Android code → split files, extract UI components
5. Server layers → ✅ PARTIALLY DONE (service/repository/route created, need controller/dao/middleware)
6. Bug fixes   → fix migrations, split transactions, catch blocks
```

---

## Approved By

User approved incremental approach covering Android + Server.

Date: 2026-05-13