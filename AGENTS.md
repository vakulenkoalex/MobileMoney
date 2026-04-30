# AGENTS.md - MobileMoney Android App

## Build Commands

```powershell
# Windows (PowerShell) - use ; not &&
.\build.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

**APK locations:**
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Project Constraints

- **minSDK 34** (Android 14 only) - older Android versions not supported
- **JDK 17** required
- Uses Java from Android Studio: `C:\Program Files\Android\Android Studio\jbr` (configured in `build.bat`)

## Architecture

```
app/
├── presentation/   # UI (Compose + ViewModel)
├── domain/          # UseCases, Entities, Repository interfaces
├── data/            # Repository impl, Local/Remote datasources
├── di/               # Hilt dependency injection
├── core/             # Utilities, constants
└── security/        # Encryption, biometrics
```

- Clean Architecture + MVVM pattern
- Room database with offline-first sync
- Soft deletes on all entities

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.21 |
| AGP | 9.2.0 |
| Compose BOM | 2024.02.00 |
| Room | 2.8.4 |
| Navigation | 2.9.8 |
| Lifecycle | 2.10.0 |

## CI

- GitHub Actions: builds on PR to `master`
- Uses JDK 17 on Ubuntu

## References

- Full build docs: `BUILD.md`
- Architecture: `docs/architecture.md`
- DB schema: `docs/db_schema.md`