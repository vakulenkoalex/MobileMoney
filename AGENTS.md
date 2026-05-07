# AGENTS.md

## Build Commands

### Android
```powershell
.\build.bat assembleDebug
```
- Output: `android/app/build/outputs/apk/debug/app-debug.apk`
- **Windows**: use `;` or separate lines — do NOT use `&&`

### Server
```powershell
.\buildServer.bat run
```
- Serves on `http://localhost:6080`

## Architecture

- **Monorepo** with two independent Gradle projects: `android/` and `server/`
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
| DI | Hilt |
| DB | Room + KSP |
| Network | Ktor client |
| Serialization | Kotlin Serialization |
| Async | Coroutines + Flow |
| Background | WorkManager |
| Pagination | Paging 3 |
| Charts | Vico |
| Security | EncryptedSharedPreferences, BiometricPrompt |

## Tech Stack (Server)

| | |
|---|---|
| Language | Kotlin 2.0.21 |
| Framework | Ktor 3.0.2 |
| Server | Netty |
| DB | SQLite |

## Project-Specific Conventions

- Docs are in Russian (`docs/*.md`)
- Icons: Material Design Icons names (e.g., `food`, `account-balance-wallet`)
- `READ_SMS` is a restricted permission on Android 14+; Google Play may reject apps using it

# Server health check
Invoke-RestMethod http://localhost:6080/
```

## References

- Architecture: `docs/architecture.md`
- DB schema: `docs/db_schema.md`
- Server run: `docs/SERVER_RUN.md`
- Build: `docs/BUILD.md`
- CI: `.github/workflows/android.yml` (builds on PR to master)