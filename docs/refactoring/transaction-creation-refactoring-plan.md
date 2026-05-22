# План рефакторинга: создание операций

## Цель

Уменьшить связанность, разделить ответственность, устранить хрупкие паттерны в коде создания/редактирования транзакций.

---

## Шаг 1: Разделить `TransactionFormScreen.kt` (727 → ~200 строк)

**Проблема:** Один файл содержит всю форму, диалоги, BottomSheet'ы и логику сплит-режима.

**Действия:**

1.1. Создать `TransactionFormScreen.kt` — только каркас формы, вызовы дочерних компонентов, нав-колбэки

1.2. Создать `AccountPickerSheet.kt` — `ModalBottomSheet` для выбора счёта

1.3. Создать `CategoryPickerSheet.kt` — `ModalBottomSheet` с `LazyVerticalGrid` категорий

1.4. Создать `TargetAccountPickerSheet.kt` — `ModalBottomSheet` с фильтром "не текущий счёт"

1.5. Создать `DatePickerDialog.kt` — `DatePickerDialog` + `TimePicker` (логика из строк 532-594)

1.6. Создать `SplitModeSection.kt` — карточка сплит-режима (строки 199-303)

**Файлы для изменения:**
- `android/.../ui/screens/TransactionFormScreen.kt` — вырезать, оставить каркас
- Новые файлы в `ui/components/` или `ui/screens/`

**Риски:** Низкие. Только перестановка кода. Сборка проверит imports.

---

## Шаг 2: Избавиться от `GlobalScope` в ViewModel

**Проблема:** Утечка корутины при уничтожении ViewModel.

**Действия:**

2.1. Заменить `GlobalScope.launch { ErrorHandler.emitError(message) }` на `viewModelScope.launch { ErrorHandler.emitError(message) }` в `TransactionFormViewModel.save()`

**Файлы для изменения:**
- `android/.../viewmodel/TransactionFormViewModel.kt`

**Риски:** Минимальные. `viewModelScope` отменяется при `onCleared()`.

---

## Шаг 3: Вынести бизнес-логику из ViewModel в UseCases

**Проблема:** `TransactionFormViewModel.save()` (~200 строк) содержит логику создания transfer-пар, сплит-режима и normal-операции. Дублируется с `SaveTransactionUseCase`.

**Действия:**

3.1. Создать `CreateTransferUseCase` — создаёт две транзакции (expense + income) с общим `relatedTransactionId`

3.2. Создать `CreateSplitTransactionUseCase` — создаёт основную транзакцию + сплит-транзакцию

3.3. Переделать `SaveTransactionUseCase` — принимать валидированные данные, а не сырой `Transaction`

3.4. В `TransactionFormViewModel.save()` оставить только сборку данных и вызов UseCase

**Сигнатуры:**
```kotlin
// Новый подход
class SaveTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    sealed class Result {
        data object Success : Result()
        data class Error(val message: String) : Result()
    }

    suspend fun normal(data: NormalTransactionData, isNew: Boolean): Result
    suspend fun transfer(data: TransferData): Result
    suspend fun split(data: SplitTransactionData, isNew: Boolean): Result
}

// или три отдельных UseCase
```

**Файлы для изменения:**
- `android/.../viewmodel/TransactionFormViewModel.kt` — сократить save()
- `android/.../usecase/transaction/SaveTransactionUseCase.kt` — переделать
- Новые: `CreateTransferUseCase.kt`, `CreateSplitTransactionUseCase.kt`

**Риски:** Средние. Нужно не сломать transfer/сплит логику. Хорошо покрыть тестами UseCase.

---

## Шаг 4: Исправить delete+insert на update при редактировании

**Проблема:** `SaveTransactionUseCase` удаляет старую запись и вставляет новую. Это проставляет `deletedAt`, и синхронизация видит её как удаление.

**Действия:**

4.1. В `TransactionRepositoryImpl` / `DatabaseRepository` использовать `update()` Room DAO вместо delete+insert

4.2. `TransactionFormViewModel.save()` при редактировании не генерировать новый UUID

4.3. Если нужна смена категории/суммы — обновить поля, не трогая `id`, `createdAt`, `syncedAt`

**Файлы для изменения:**
- `android/.../domain/usecase/transaction/SaveTransactionUseCase.kt`
- `android/.../data/repository/TransactionRepositoryImpl.kt`
- `android/.../data/repository/DatabaseRepository.kt`

**Риски:** Средние. `OnConflictStrategy.REPLACE` работает, но поля вроде `createdAt` могут перезатереться. Нужно явно копировать их из существующей записи.

---

## Шаг 5: Подтверждение выхода с незаполненной формой

**Проблема:** При нажатии "назад" данные формы теряются без предупреждения.

**Действия:**

5.1. Добавить в `TransactionFormViewModel` флаг `isDirty: Boolean` — true, если хоть одно поле изменилось

5.2. В `TransactionFormScreen` добавить `BackHandler`:

```kotlin
BackHandler(enabled = viewModel.isDirty) {
    showExitDialog = true
}
```

5.3. Показать `AlertDialog` с опциями: "Сохранить", "Не сохранять", "Отмена"

**Файлы для изменения:**
- `android/.../viewmodel/TransactionFormViewModel.kt` — добавить `isDirty`
- `android/.../ui/screens/TransactionFormScreen.kt` — добавить `BackHandler`

**Риски:** Низкие.

---

## Шаг 6: Убрать UUID из навигации

**Проблема:** `UUID.randomUUID()` генерируется при навигации и передаётся как параметр роута — хрупко и не нужно.

**Действия:**

6.1. Изменить навигацию:
```kotlin
data object CreateTransaction : Screen("create_transaction")
data object EditTransaction : Screen("edit/{transactionId}") {
    fun createRoute(transactionId: UUID) = "edit/$transactionId"
    const val ROUTE = "edit/{transactionId}"
}
```

6.2. В `TransactionFormViewModel` генерировать `UUID` при первом сохранении (если `transactionId == null`)

**Файлы для изменения:**
- `android/.../ui/navigation/Navigation.kt`
- `android/.../viewmodel/TransactionFormViewModel.kt`

**Риски:** Низкие.

---

## Шаг 7: Amount — `String` → `TextFieldValue` с фильтром

**Проблема:** Сумма как `String` ломается от пробелов, тысяч разделителей, копипаста.

**Действия:**

7.1. В `TransactionFormState` заменить `amount: String` на:
```kotlin
val amountText: TextFieldValue = TextFieldValue("")
val amount: Double? = null // парсится из amountText
```

7.2. Добавить `VisualTransformation` для форматирования числа (например, разделители тысяч)

7.3. Валидация: отфильтровать не-цифровые символы (кроме `.` и `,`)

**Файлы для изменения:**
- `android/.../viewmodel/TransactionFormViewModel.kt`
- `android/.../ui/screens/TransactionFormScreen.kt`

**Риски:** Средние. Фильтр ввода нужно аккуратно реализовать с поддержкой локали.

---

## Порядок выполнения

| Шаг | Описание | Риск | Зависит от |
|-----|----------|------|------------|
| 1 | Разделить экран на компоненты | низкий | — |
| 2 | GlobalScope → viewModelScope | низкий | — |
| 5 | BackHandler при выходе | низкий | — |
| 6 | Убрать UUID из навигации | низкий | — |
| 3 | Вынести логику в UseCases | средний | 1, 2 |
| 4 | delete+insert → update | средний | 3 |
| 7 | Amount: String → TextFieldValue | средний | 1 |

Шаги 1, 2, 5, 6 независимы — можно делать параллельно.
Шаги 3, 4, 7 — последовательно, после шага 1.

## Верификация

После каждого шага:
1. `.\android\build.bat assembleDebug` — компиляция
2. Проверить: создание expense/income/transfer, редактирование, сплит, ввод сумм
3. Проверить синхронизацию: создать → подождать sync → проверить на сервере
