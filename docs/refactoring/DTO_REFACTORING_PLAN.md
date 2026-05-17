# DTO Refactoring Plan

**Status:** NOT STARTED — низкий приоритет

## Problem
DTOs are duplicated between Android and Server:
- `AccountDto` - in `android/.../SyncApiClient.kt` and `server/.../model/dto/AccountDto.kt`
- `CategoryDto` - same locations
- `TransactionDto` - same locations

## Solution Options

### Option 1: Shared Kotlin Module (Recommended)
Create a shared Gradle module with common DTOs.

**Structure:**
```
common/
├── build.gradle.kts
└── src/commonMain/kotlin/
    └── com/mobilemoney/dto/
        ├── AccountDto.kt
        ├── CategoryDto.kt
        ├── TransactionDto.kt
        └── SyncPushRequest.kt
```

**Pros:**
- Single source of truth
- Type-safe
- Automatic serialization setup

**Cons:**
- Requires multi-module setup
- More complex build configuration

### Option 2: OpenAPI / Schema Generation
Generate DTOs from OpenAPI spec.

**Pros:**
- Industry standard
- Multiple language support

**Cons:**
- More complex setup
- Overhead for simple use case

### Option 3: Copy-Paste with Note
Keep as-is with documentation note.

**Pros:**
- No code changes
- Quick

**Cons:**
- Not really solving the problem

## Recommended: Option 1

1. Create `common/` module
2. Move DTOs to common
3. Update Android `build.gradle` to depend on common
4. Update Server `build.gradle` to depend on common
5. Remove duplicate DTOs from both projects
6. Update imports in SyncApiClient and SyncDto

## Implementation Steps

1. Create directory structure: `common/src/commonMain/kotlin/com/mobilemoney/dto/`
2. Create `common/build.gradle.kts` with kotlin serialization
3. Move DTOs:
   - `AccountDto.kt`
   - `CategoryDto.kt`
   - `TransactionDto.kt`
   - `SyncPushRequest.kt` (combine from both)
4. Update Android: `implementation(project(":common"))`
5. Update Server: `implementation(project(":common"))`
6. Remove duplicate files from:
   - `android/.../data/remote/SyncApiClient.kt` (keep DTOs as imports)
   - `server/.../SyncDto.kt` (remove DTOs, import from common)