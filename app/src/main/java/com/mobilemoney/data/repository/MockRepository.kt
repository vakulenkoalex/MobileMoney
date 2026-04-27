package com.mobilemoney.data.repository

import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.data.model.TransactionUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object MockRepository {

    private val _accounts = MutableStateFlow(getMockAccounts())
    val accounts: StateFlow<List<AccountUi>> = _accounts.asStateFlow()

    private val _categories = MutableStateFlow(getMockCategories())
    val categories: StateFlow<List<CategoryUi>> = _categories.asStateFlow()

    private val _transactions = MutableStateFlow(getMockTransactions())
    val transactions: StateFlow<List<TransactionUi>> = _transactions.asStateFlow()

    private fun getMockAccounts(): List<AccountUi> = listOf(
        AccountUi(UUID.randomUUID(), "Наличка", "₽", "wallet"),
        AccountUi(UUID.randomUUID(), "Карта Сбер", "₽", "credit_card"),
        AccountUi(UUID.randomUUID(), "Тинькофф", "₽", "account_balance")
    )

    private fun getMockCategories(): List<CategoryUi> = listOf(
        CategoryUi(UUID.randomUUID(), "Еда", "restaurant", false),
        CategoryUi(UUID.randomUUID(), "Транспорт", "directions_bus", false),
        CategoryUi(UUID.randomUUID(), "Магазин", "shopping_cart", false),
        CategoryUi(UUID.randomUUID(), "Развлечения", "movie", false),
        CategoryUi(UUID.randomUUID(), "Здоровье", "local_hospital", false),
        CategoryUi(UUID.randomUUID(), "Зарплата", "work", true),
        CategoryUi(UUID.randomUUID(), "Подарок", "card_giftcard", true),
        CategoryUi(UUID.randomUUID(), "Прочее", "more_horiz", true)
    )

    private fun getMockTransactions(): List<TransactionUi> {
        val accounts = getMockAccounts()
        val categories = getMockCategories()
        return listOf(
            TransactionUi(
                id = UUID.randomUUID(),
                title = "Пятёрочка",
                subtitle = "Сегодня, 14:30",
                comment = "Покупки на неделю",
                amount = 1500.00,
                currency = "₽",
                icon = "shopping_cart",
                color = 0xFF4CAF50,
                isIncome = false,
                date = System.currentTimeMillis()
            ),
            TransactionUi(
                id = UUID.randomUUID(),
                title = "Такси",
                subtitle = "Сегодня, 10:15",
                comment = "Яндекс Такси",
                amount = 250.00,
                currency = "₽",
                icon = "local_taxi",
                color = 0xFFFF9800,
                isIncome = false,
                date = System.currentTimeMillis() - 3600000
            ),
            TransactionUi(
                id = UUID.randomUUID(),
                title = "Зарплата",
                subtitle = "25 апр",
                comment = "",
                amount = 50000.00,
                currency = "₽",
                icon = "work",
                color = 0xFF2196F3,
                isIncome = true,
                date = System.currentTimeMillis() - 86400000 * 2
            ),
            TransactionUi(
                id = UUID.randomUUID(),
                title = "Перевод",
                subtitle = "24 апр, Наличка",
                comment = "Перевод на карту",
                amount = 5000.00,
                currency = "₽",
                icon = "swap_horiz",
                color = 0xFF9C27B0,
                isIncome = false,
                date = System.currentTimeMillis() - 86400000 * 3
            ),
            TransactionUi(
                id = UUID.randomUUID(),
                title = "Аптека",
                subtitle = "Вчера",
                comment = "Лекарства",
                amount = 890.00,
                currency = "₽",
                icon = "local_hospital",
                color = 0xFFF44336,
                isIncome = false,
                date = System.currentTimeMillis() - 86400000
            ),
            TransactionUi(
                id = UUID.randomUUID(),
                title = "Кино",
                subtitle = "22 апр",
                comment = "Новый фильм",
                amount = 600.00,
                currency = "₽",
                icon = "movie",
                color = 0xFFE91E63,
                isIncome = false,
                date = System.currentTimeMillis() - 86400000 * 5
            )
        )
    }

    fun getTransactionById(id: UUID): TransactionUi? {
        return _transactions.value.find { it.id == id }
    }

    fun addTransaction(transaction: TransactionUi) {
        _transactions.value = listOf(transaction) + _transactions.value
    }

    fun updateTransaction(transaction: TransactionUi) {
        _transactions.value = _transactions.value.map {
            if (it.id == transaction.id) transaction else it
        }
    }

    fun deleteTransaction(id: UUID) {
        _transactions.value = _transactions.value.filter { it.id != id }
    }
}