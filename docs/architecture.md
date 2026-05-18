# Архитектура приложения "Учет личных денег"

## 1. Common модуль

Общие DTO, используемые на Android и Server:

```
common/src/main/kotlin/com/mobilemoney/dto/
├── ErrorResponse.kt
├── LoginRequest.kt
├── LoginResponse.kt
├── SyncPushRequest.kt
├── SyncPushResponse.kt
├── SyncPullResponse.kt
├── SyncChangesResponse.kt
├── AccountDto.kt
├── CategoryDto.kt
└── TransactionDto.kt
```

## 2. Domain слой (Бизнес-логика)

### 2.1 Типы источников операций

```kotlin
enum class TransactionSource { MANUAL, SMS, PUSH }
```

> **Реализация:** В текущей версии используется только `MANUAL`. `SMS` и `PUSH` зарезервированы для будущей реализации автоматического импорта из SMS/уведомлений.

## 3. Data слой

### 3.4 BackupRepository

**Особенности:**
- Закрывает БД перед копированием для обеспечения целостности
- Копирует напрямую Room DB файл (не JSON/CSV)
- Удаляет WAL/SHM/JOURNAL файлы после импорта

## 4. Архитектура приложения

### 4.1 Структура проекта (Clean Architecture)

```
android/app/src/main/java/com/mobilemoney/
├── MainActivity.kt
├── MobileMoneyApp.kt
├── data/
│   ├── config/                # CategoryIcons, AccountIcons, Currencies
│   ├── local/                 # Room Entities, DAOs, AppDatabase, Mappers
│   ├── model/                 # Domain models (DTO)
│   ├── remote/                # SyncApiClient
│   └── repository/            # DatabaseRepository, SyncRepository, BackupRepository
├── domain/                    # Domain layer
│   ├── model/                 # Transaction, Category, Account (domain models)
│   ├── repository/            # Repository interfaces
│   │   ├── AccountRepository.kt
│   │   ├── CategoryRepository.kt
│   │   ├── TransactionRepository.kt
│   │   └── SyncRepository.kt
│   └── usecase/
│       ├── account/
│       │   ├── CreateAccountUseCase.kt
│       │   └── GetAccountsUseCase.kt
│       ├── category/
│       │   └── GetCategoriesUseCase.kt
│       └── transaction/
│           ├── GetTransactionsUseCase.kt
│           └── SaveTransactionUseCase.kt
├── di/                        # DI.kt (ручной DI)
├── ui/
│   ├── navigation/            # Navigation.kt
│   ├── screens/               # Compose UI screens
│   ├── theme/                 # Theme.kt, Typography.kt
│   └── utils/                 # FormatUtils
├── viewmodel/                 # ViewModels
│   ├── LoginViewModel.kt
│   ├── AccountListViewModel.kt
│   ├── AccountFormViewModel.kt
│   ├── CategoryListViewModel.kt
│   ├── CategoryFormViewModel.kt
│   ├── TransactionListViewModel.kt
│   ├── TransactionFormViewModel.kt
│   └── SettingsViewModel.kt
└── worker/                    # SyncWorker.kt
```

### 4.2 UI Screens

```
ui/screens/
├── LoginScreen.kt
├── AccountListScreen.kt
├── AccountFormScreen.kt       # Add/Edit account
├── CategoryListScreen.kt
├── CategoryFormScreen.kt      # Add/Edit category
├── TransactionListScreen.kt
├── TransactionFormScreen.kt   # Add/Edit transaction, split mode
└── SettingsScreen.kt
```

> **Примечание:** UI компоненты (AccountCard, TransactionItem и т.д.) реализованы inline внутри экранов, а не вынесены в отдельные файлы.

## 6. Безопасность

### 6.1 Требования

- Шифрование данных (EncryptedSharedPreferences, Room с SQLCipher или androidx.security:security-crypto)
- Биометрическая аутентификация
- Logout с очисткой данных

## 8. Технологический стек

| Компонент | Технология | Версия |
|-----------|------------|--------|
| Language | Kotlin | 2.3.21 |
| Compile SDK | API 37 | |
| Target/Min SDK | API 34 (Android 14) | |
| Gradle | | 9.4.1 |
| AGP | Android Gradle Plugin | 9.2.0 |
| UI | Jetpack Compose + Material 3 | BOM 2026.03.00 |
| Compose Compiler Plugin | Kotlin | 2.3.21 |
| Lifecycle | | 2.10.0 |
| Navigation Compose | | 2.9.8 |
| Networking | HttpURLConnection (ручной) | - |
| Serialization | Kotlin Serialization | 1.11.0 |
| Local DB | Room | 2.8.4 |
| Async | Kotlin Coroutines + Flow | 1.9.0 |
| Background | WorkManager | 2.11.1 |
| Security | EncryptedSharedPreferences | |

---

## 9. База данных (Room Schema)

Баланс кошелька вычисляется из транзакций:
  SUM(INCOME) - SUM(EXPENSE) по wallet_id

## 10. API Endpoints

См. [api_scenarios.md](api_scenarios.md)

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

### 11.4 Разделение операции

Позволяет разбить одну операцию на две части с разными категориями.

**Сценарий:**
- Есть операция "Продукты" на сумму 100
- Пользователь хочет: уменьшить "Продукты" до 80, создать "Лекарства" на 20

**UI:**
- Кнопка "Разделить операцию" в режиме редактирования
- Два поля суммы:
  - Основная категория (read-only, авторассчитывается): 100 - 20 = 80
  - Новая категория (редактируемое): ввод 20
- Выбор категории для новой операции через bottom sheet
- Итоговая сумма отображается для контроля

**Логика:**
1. При сохранении в режиме split:
   - Удаляется исходная операция (если редактирование)
   - Создаётся операция 1: исходная категория, сумма = общая - часть
   - Создаётся операция 2: новая категория, сумма = часть
2. Обе операции сохраняются с одинаковой датой и комментарием
