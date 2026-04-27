# План экрана: Создание/Редактирование операции (Jetpack Compose)

## 1. Цель

Экран формы для создания и редактирования транзакции.

## 2. Поля формы

| Поле | Тип | Обязательно | Описание |
|------|-----|-------------|----------|
| amount | BigDecimal/Double | Да | Сумма операции |
| accountId | UUID | Да | Выбор счёта |
| categoryId | UUID? | Нет | Выбор категории (nullable) |
| date | Instant/LocalDateTime | Да | Дата и время операции |
| comment | String? | Нет | Комментарий |
| source | TransactionSource | Да | Источник (по умолча MANUAL) |

## 3. UI модель

```kotlin
data class TransactionFormState(
    val amount: String = "",
    val selectedAccount: AccountUi? = null,
    val selectedCategory: CategoryUi? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val comment: String = "",
    val isIncome: Boolean = false, // переключатель тип операции
    val isEditing: Boolean = false,
    val transactionId: UUID? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AccountUi(val id: UUID, val name: String, val currency: String, val icon: String?)
data class CategoryUi(val id: UUID, val name: String, val icon: String?, val isIncome: Boolean)
```

## 4. Компоненты Compose

### 4.1 Тип операции (переключатель)
- SegmentedButton: "Расход" / "Приход"
- При переключении фильтруется список категорий

### 4.2 Сумма
- TextField с цифровой клавиатурой
- Форматирование: разделитель тысяч, 2 знака после запятой
- Валюта берётся из выбранного счёта

### 4.3 Выбор счёта
- DropdownMenu или BottomSheet
- Показывает: название, валюту, иконку
- Список: `AccountRepository.getAccounts()`

### 4.4 Выбор категории
- Grid/List иконок категорий
- Фильтр по isIncome (расход/приход)
- `CategoryRepository.getCategoriesByIncome(isIncome)`
-Nullable (можно сохранить без категории)

### 4.5 Дата
- DatePicker + TimePicker
- По умолчанию: текущее время

### 4.6 Комментарий
- TextField, multiline, опционально
- Макс. 500 символов

### 4.7 Сохранение
- Кнопка "Сохранить" в AppBar или внизу
- Валидация: amount > 0, accountId != null

## 5. Навигация

```
TransactionsListScreen
    ├── (+) -> CreateTransactionScreen
    └── (click item) -> EditTransactionScreen
```

Аргументы: `transactionId` для редактирования, null для создания.

## 6. Валидация

- amount: не пусто, > 0
- account: выбран
- date: валидная дата

## 7. Этапы реализации

1) Создать TransactionFormState и UI модели (AccountUi, CategoryUi)
2) Реализовать маппинг Account -> AccountUi, Category -> CategoryUi
3) Создать AccountSelector и CategorySelector компоненты
4) Реализовать форму с SegmentedButton, TextField, DatePicker
5) Подключить ViewModel с валидацией
6) Добавить навигацию из списка
7) Реализовать Edit - загрузка данных по ID

## 8. Моки для тестирования

```kotlin
val mockAccounts = listOf(
    AccountUi(UUID.randomUUID(), "Наличка", "RUB", "wallet"),
    AccountUi(UUID.randomUUID(), "Карта Сбер", "RUB", "credit-card")
)

val mockCategories = listOf(
    CategoryUi(UUID.randomUUID(), "Еда", "restaurant", false),
    CategoryUi(UUID.randomUUID(), "Транспорт", "directions_bus", false),
    CategoryUi(UUID.randomUUID(), "Зарплата", "work", true),
    CategoryUi(UUID.randomUUID(), "Подарок", "card_giftcard", true)
)
```