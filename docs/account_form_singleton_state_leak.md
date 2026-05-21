# Проблема: утечка состояния AccountFormViewModel

## Симптомы

1. **Нажатие "+" открывает форму с данными существующего счёта.** Вместо пустой формы "Новый счёт" отображаются поля, заполненные ранее просмотренным/отредактированным счётом.
2. **После сохранения счёта и повторной попытки открыть другой счёт — мерцание экрана.** Форма открывается и сразу закрывается (popBackStack), со второго раза работает нормально.

## Причина

### Корень: ViewModel-синглтон

В `DI.kt` все ViewModel создаются через `by lazy` — это **синглтоны**:

```kotlin
val accountFormViewModel: AccountFormViewModel by lazy {
    AccountFormViewModel(getAccountsUseCase, createAccountUseCase)
}
```

Один экземпляр живёт всё время жизни приложения и используется всеми экранами, разделяющими одну и ту же ViewModel. При навигации Compose-компонент пересоздаётся, но ViewModel остаётся той же — со старым состоянием.

### Первая проблема: не сбрасывалось состояние при входе в Create

`LaunchedEffect(accountId)` проверял `accountId != null` и вызывал `loadAccount()` только для редактирования. При создании (`accountId = null`) ничего не происходит — ViewModel хранит данные от предыдущего использования:

```kotlin
LaunchedEffect(accountId) {
    if (accountId != null) {          // null → пропускаем
        viewModel.loadAccount(accountId)
    }
}
```

### Вторая проблема: stale `isSaved`

После успешного сохранения `save()` устанавливает `isSaved = true`, и `LaunchedEffect(uiState.isSaved)` тут же навигирует назад. Флаг остаётся `true` в синглтоне. При повторном открытии формы (редактирование другого счёта) `LaunchedEffect(uiState.isSaved)` **сразу же срабатывает**, потому что ключ эффекта — это текущее значение `isSaved`, которое всё ещё `true`. Эффект запускается при входе в композицию, а `resetState()` в параллельном `LaunchedEffect` может не успеть выполниться (race condition).

## Решение

### 1. Метод `resetState()` в ViewModel

`AccountFormViewModel.kt` — новый метод, сбрасывающий всё состояние в начальные значения:

```kotlin
fun resetState() {
    _uiState.value = AccountFormState()
}
```

### 2. Сброс при любом входе в форму

`AccountFormScreen.kt` — `resetState()` вызывается всегда, вне зависимости от create/edit:

```kotlin
LaunchedEffect(accountId) {
    viewModel.resetState()
    if (accountId != null) {
        viewModel.loadAccount(accountId)
    }
}
```

### 3. Замена LaunchedEffect на snapshotFlow с drop(1)

Вместо `LaunchedEffect(uiState.isSaved)`, который реагирует на начальное stale-значение, используем `snapshotFlow` с `drop(1)`, игнорирующий первое (текущее) значение и реагирующий только на **изменения**:

```kotlin
LaunchedEffect(Unit) {
    snapshotFlow { uiState.isSaved }
        .drop(1)
        .collect { if (it) onNavigateBack() }
}
```

## Затронутые файлы

| Файл | Изменение |
|---|---|
| `android/.../viewmodel/AccountFormViewModel.kt` | Добавлен `resetState()` |
| `android/.../ui/screens/AccountFormScreen.kt` | Вызов `resetState()` при каждом входе; `LaunchedEffect(isSaved)` → `snapshotFlow.drop(1)` |

## Примечание

Аналогичная проблема потенциально есть у других ViewModel-синглтонов (`transactionFormViewModel`, `categoryFormViewModel`), но пока не была воспроизведена.
