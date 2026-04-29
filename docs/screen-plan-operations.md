# План экрана: Операции (Jetpack Compose)

- Цель: экран списка транзакций с левым значком категории, двумя строками текста и правой суммой. Элементы разделяются линией; сумма окрашена по знаку. Внизу — нижняя навигация, как на скрине.

- Архитектура: MVVM + UI-композиты
  - UI модель: `data class TransactionUi(val id: String, val title: String, val subtitle: String, val comment: String, val amount: Double, val currency: String = "₽", val icon: ImageVector, val color: Color, val isIncome: Boolean)`
  - Domain модель (из db_schema): `data class Transaction(val id: UUID, val accountId: UUID, val categoryId: UUID?, val amount: BigDecimal, val date: Instant, val comment: String?, val source: String, val sourceData: String?, val creatorId: UUID, val relatedTransactionId: UUID?)`
  - Перевод между счетами: создаются 2 транзакции с общим UUID в `relatedTransactionId` (расход + приход)
  - Маппинг: domain -> UI (category name -> title, category icon -> icon, accountId -> subtitle)
  - ViewModel: хранит список транзакций и состояние загрузки/ошибки.
  - Репозиторий: моковые данные на старте (позже Replace with Room/Retrofit).

- Источник данных
  - Моки: 6–8 элементов с различными категориями и знаками суммы.

- Компоненты Compose ( MVP-версия )
  - TransactionRow(transaction: Transaction)
    - Row(VerticalAlignment.Center)
      - Box: круглый фон и внутренняя иконка слева (цвет фона — transaction.color)
      - Column: Text(title, style = MaterialTheme.typography.subtitle1, maxLines = 1)
        - Text(subtitle, style = MaterialTheme.typography.body2, color = Color.Gray, maxLines = 1)
      - Spacer()
      - Text(text = formatAmount(transaction.amount, transaction.currency), color = amountColor(transaction.amount))
      
  - TransactionsScreen(transactions: List<Transaction>, onItemClick: (Transaction)->Unit)
    - Scaffold(
        topBar = { TopAppBar(title = { Text("Операции") }) },
        bottomBar = { BottomNavigation /* реиспользуемый модуль проекта */ }
      ) { padding ->
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
          items(transactions) { t -> TransactionRow(t, onClick = { onItemClick(t) }) }
        }
      }
    )

- Форматирование и стиль
  - Функция formatAmount(amount: Double, currency: String, isIncome: Boolean): String = "${if (isIncome) '+' else '-'}${abs(amount)} $currency"
  - amountColor(isIncome: Boolean): Color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFD32F2F) // зелёный/красный
  - Иконки: использовать ImageVector или vector resources; можно заменить на Icon с vector.

- Данные и состояние
  - FakeRepository: fun getTransactions(): List<Transaction> = listOf(...)
  - ViewModel: val transactions: StateFlow<List<Transaction>> = MutableStateFlow(fakeRepository.getTransactions())

- Адаптивность
  - LazyColumn с adapt padding; Divider между элементами.
  - Поддержка тем: цвета под MaterialTheme.

- Навигация
  - Пример: навигация к детальной странице по клику; заготовка `onItemClick`.

- Тестирование
  - Preview-компоненты TransactionRowPreview
  - Юнит-тест formatAmount и amountColor

- Этапы реализации
  1) Создать data class Transaction и FakeRepository
  2) Реализовать TransactionRow и TransactionsScreen
  3) Подключить ViewModel и моковые данные
  4) Добавить Preview и стили
  5) Интеграция с темой и навигацией
  6) Тесты и документация

- Риски
  - Переход на Compose требует модульной поддержки; при интеграции с существующим кодом проверить совместимость
