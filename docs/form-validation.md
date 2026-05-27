# Форматирование проверки заполнения форм

## Типы полей

| Тип | Класс | Назначение |
|-----|-------|------------|
| Строка | `FormField` | `amount`, `name`, `comment` и т.д. |
| Nullable объект | `FieldState<T>` | `selectedAccount`, `selectedCategory`, `splitCategory` |

## Структура State

```kotlin
data class FormState(
    val amount: FormField = FormField(label = "Сумма"),
    val selectedAccount: FieldState<Account> = FieldState(),
    val selectedCategory: FieldState<Category> = FieldState()
)
```

- Label определяется только в State, Screen читает через `uiState.field.label`
- Screen НЕ содержит свои тексты ошибок или labels

## Валидация в ViewModel (save)

```kotlin
// Строка — встроенная validate()
val cleanAmount = state.amount.validate()
if (!cleanAmount.isValid) {
    _uiState.value = state.copy(amount = cleanAmount)
    viewModelScope.launch { ErrorHandler.emitError(cleanAmount.error!!) }
    return false
}

// Nullable объект — validate() с кастомным сообщением
val cleanAccount = state.selectedAccount.validate("Выберите счёт")
if (!cleanAccount.isValid) {
    _uiState.value = state.copy(selectedAccount = cleanAccount)
    viewModelScope.launch { ErrorHandler.emitError("Выберите счёт") }
    return false
}
```

## Сообщения об ошибках

- Пустое строковое поле: `"Заполните обязательные поля"` (кроме amount)
- Пустой amount: `"Заполните обязательные поля"`
- Некорректный amount: `"Введите корректную сумму"`
- Nullable объект null: кастомное сообщение (например, `"Выберите счёт"`, `"Выберите категорию"`)
- Формат `FormField.validate()`: `"Введите '<label>'"` (в одинарных кавычках)

## Сброс ошибок

При изменении значения ошибка очищается автоматически:

```kotlin
fun updateAccount(account: Account) {
    _uiState.value = _uiState.value.copy(
        selectedAccount = _uiState.value.selectedAccount.withValue(account)
    )
}
```

`FieldState.withValue(value)` — устанавливает value и очищает error.

## Отображение в Screen

```kotlin
// Inline error (текст под полем)
Column {
    Row(
        modifier = Modifier.border(
            1.dp,
            if (uiState.field.error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outline,
            RoundedCornerShape(8.dp)
        )
    ) { /* содержимое поля */ }
    if (uiState.field.error != null) {
        Text(
            text = uiState.field.error!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}

// Toast — ErrorHandler.emitError() (дублирует первое сообщение, ~2 сек)
```

- Inline error: цвет рамки `error`, текст ошибки под полем
- Toast для первого поля с ошибкой (через `ErrorHandler.emitError`)
- Ошибка снимается при повторном открытии/клике на поле

## Flow

1. User нажимает «Сохранить»
2. `save()` валидирует каждое поле по порядку
3. При первой ошибке: `state.copy(field = cleanField)` + `ErrorHandler.emitError()` + `return false`
4. Screen перерисовывается с `error != null` → красная рамка + текст
5. User видит Toast и inline-подсветку первого поля с ошибкой
6. При изменении поля `withValue()` очищает error
