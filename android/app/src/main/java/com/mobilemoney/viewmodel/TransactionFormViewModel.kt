package com.mobilemoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.model.CategoryUi
import com.mobilemoney.data.model.TransactionUi
import com.mobilemoney.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

data class TransactionFormState(
    val amount: String = "",
    val selectedAccount: AccountUi? = null,
    val targetAccount: AccountUi? = null,
    val selectedCategory: CategoryUi? = null,
    val date: Long = System.currentTimeMillis(),
    val comment: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val isEditing: Boolean = false,
    val transactionId: UUID? = null,
    val accounts: List<AccountUi> = emptyList(),
    val categories: List<CategoryUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isSplitMode: Boolean = false,
    val splitAmount: String = "",
    val splitCategory: CategoryUi? = null
)

class TransactionFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository = MobileMoneyApp.getRepository(application)

    private val _uiState = MutableStateFlow(TransactionFormState())
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getAccounts()
                .catch { /* handle error */ }
                .collect { accounts ->
                    _uiState.value = _uiState.value.copy(accounts = accounts)
                    if (!uiState.value.isEditing) {
                        val defaultAccount = accounts.find { it.isDefault } ?: accounts.firstOrNull()
                        _uiState.value = _uiState.value.copy(selectedAccount = defaultAccount)
                    }
                }
        }
        viewModelScope.launch {
            repository.getCategories()
                .catch { /* handle error */ }
                .collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
        }
    }

    fun loadTransaction(transactionId: UUID) {
        viewModelScope.launch {
            val transaction = repository.getTransactionById(transactionId.toString())
            if (transaction != null) {
                val accounts = _uiState.value.accounts
                val categories = _uiState.value.categories

                val targetAccount = if (transaction.relatedTransactionId != null) {
                    val relatedTx = repository.getRelatedTransaction(
                        transaction.relatedTransactionId.toString(),
                        transaction.id.toString()
                    )
                    relatedTx?.accountId
                } else null

                _uiState.value = _uiState.value.copy(
                    amount = transaction.amount.toString(),
                    selectedAccount = accounts.find { it.id == transaction.accountId },
                    targetAccount = targetAccount?.let { tid -> accounts.find { it.id == tid } },
                    selectedCategory = categories.find { it.id == transaction.categoryId },
                    date = transaction.date,
                    comment = transaction.comment,
                    type = when {
                        transaction.relatedTransactionId != null -> TransactionType.TRANSFER
                        transaction.isIncome -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    },
                    isEditing = true,
                    transactionId = transactionId
                )
            }
        }
    }

    fun updateAmount(value: String) {
        _uiState.value = _uiState.value.copy(amount = value)
    }

    fun updateAccount(account: AccountUi) {
        _uiState.value = _uiState.value.copy(selectedAccount = account)
    }

    fun updateCategory(category: CategoryUi?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateDate(date: Long) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateComment(comment: String) {
        _uiState.value = _uiState.value.copy(comment = comment)
    }

    fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun updateTargetAccount(account: AccountUi) {
        _uiState.value = _uiState.value.copy(targetAccount = account)
    }

    fun enableSplitMode() {
        _uiState.value = _uiState.value.copy(isSplitMode = true, splitAmount = "")
    }

    fun disableSplitMode() {
        _uiState.value = _uiState.value.copy(isSplitMode = false, splitAmount = "", splitCategory = null)
    }

    fun updateSplitAmount(value: String) {
        val parsed = value.toDoubleOrNull() ?: 0.0
        val currentAmount = _uiState.value.amount.toDoubleOrNull() ?: 0.0
        if (parsed <= currentAmount) {
            _uiState.value = _uiState.value.copy(splitAmount = value)
        }
    }

    fun updateSplitCategory(category: CategoryUi) {
        _uiState.value = _uiState.value.copy(splitCategory = category)
    }

    fun getRemainingAmount(): Double {
        val total = _uiState.value.amount.toDoubleOrNull() ?: 0.0
        val split = _uiState.value.splitAmount.toDoubleOrNull() ?: 0.0
        return total - split
    }

    fun getSplitFilteredCategories(): List<CategoryUi> {
        val isIncome = _uiState.value.type == TransactionType.INCOME
        return _uiState.value.categories.filter { it.isIncome == isIncome }
    }

    fun save(): Boolean {
        val state = _uiState.value

        if (state.amount.isBlank() || state.amount.toDoubleOrNull() == null || state.amount.toDouble() <= 0) {
            _uiState.value = state.copy(error = "Введите корректную сумму")
            return false
        }

        if (state.selectedAccount == null) {
            _uiState.value = state.copy(error = "Выберите счёт")
            return false
        }

        if (state.type == TransactionType.TRANSFER && state.targetAccount == null) {
            _uiState.value = state.copy(error = "Выберите целевой счёт")
            return false
        }

        if (state.isSplitMode) {
            val splitAmount = state.splitAmount.toDoubleOrNull() ?: 0.0
            if (splitAmount <= 0) {
                _uiState.value = state.copy(error = "Введите сумму для разделения")
                return false
            }
            if (state.splitCategory == null) {
                _uiState.value = state.copy(error = "Выберите категорию для новой операции")
                return false
            }
        }

        val isIncome = state.type == TransactionType.INCOME
        val icon = when (state.type) {
            TransactionType.TRANSFER -> "swap_horiz"
            TransactionType.INCOME -> state.selectedCategory?.icon ?: "work"
            TransactionType.EXPENSE -> state.selectedCategory?.icon ?: "shopping_cart"
        }

        val title = when (state.type) {
            TransactionType.TRANSFER -> "Перевод"
            else -> state.selectedCategory?.name ?: "Без категории"
        }

        val subtitle = when (state.type) {
            TransactionType.TRANSFER -> "${state.selectedAccount.name} → ${state.targetAccount?.name}"
            else -> state.selectedAccount.name
        }

        viewModelScope.launch {
            if (state.type == TransactionType.TRANSFER) {
                val transferId = UUID.randomUUID()
                val amount = state.amount.toDouble()

                val expenseTransaction = TransactionUi(
                    id = UUID.randomUUID(),
                    title = title,
                    subtitle = subtitle,
                    comment = state.comment,
                    amount = amount,
                    currency = state.selectedAccount.currency,
                    icon = icon,
                    color = 0xFF9C27B0,
                    isIncome = false,
                    date = state.date,
                    accountId = state.selectedAccount.id,
                    categoryId = null,
                    relatedTransactionId = transferId
                )

                val incomeTransaction = TransactionUi(
                    id = UUID.randomUUID(),
                    title = title,
                    subtitle = subtitle,
                    comment = state.comment,
                    amount = amount,
                    currency = state.targetAccount?.currency ?: state.selectedAccount.currency,
                    icon = icon,
                    color = 0xFF9C27B0,
                    isIncome = true,
                    date = state.date,
                    accountId = state.targetAccount?.id,
                    categoryId = null,
                    relatedTransactionId = transferId
                )

                repository.addTransaction(expenseTransaction)
                repository.addTransaction(incomeTransaction)
            } else if (state.isSplitMode) {
                val splitAmount = state.splitAmount.toDoubleOrNull() ?: 0.0
                val remainingAmount = state.amount.toDoubleOrNull()!! - splitAmount

                val splitIcon = state.splitCategory?.icon ?: "category"
                val mainTransaction = TransactionUi(
                    id = UUID.randomUUID(),
                    title = state.selectedCategory?.name ?: "Без категории",
                    subtitle = state.selectedAccount.name,
                    comment = state.comment,
                    amount = remainingAmount,
                    currency = state.selectedAccount.currency,
                    icon = state.selectedCategory?.icon ?: "shopping_cart",
                    color = if (isIncome) 0xFF2E7D32 else 0xFFD32F2F,
                    isIncome = isIncome,
                    date = state.date,
                    accountId = state.selectedAccount.id,
                    categoryId = state.selectedCategory?.id
                )

                val newTransaction = TransactionUi(
                    id = UUID.randomUUID(),
                    title = state.splitCategory?.name ?: "Без категории",
                    subtitle = state.selectedAccount.name,
                    comment = state.comment,
                    amount = splitAmount,
                    currency = state.selectedAccount.currency,
                    icon = splitIcon,
                    color = if (isIncome) 0xFF2E7D32 else 0xFFD32F2F,
                    isIncome = isIncome,
                    date = state.date,
                    accountId = state.selectedAccount.id,
                    categoryId = state.splitCategory?.id
                )

                if (state.isEditing) {
                    repository.deleteTransaction(state.transactionId.toString())
                }
                repository.addTransaction(mainTransaction)
                repository.addTransaction(newTransaction)
            } else {
                val transaction = TransactionUi(
                    id = state.transactionId ?: UUID.randomUUID(),
                    title = title,
                    subtitle = subtitle,
                    comment = state.comment,
                    amount = state.amount.toDouble(),
                    currency = state.selectedAccount.currency,
                    icon = icon,
                    color = when (state.type) {
                        TransactionType.TRANSFER -> 0xFF9C27B0
                        TransactionType.INCOME -> 0xFF2E7D32
                        TransactionType.EXPENSE -> 0xFFD32F2F
                    },
                    isIncome = isIncome,
                    date = state.date,
                    accountId = state.selectedAccount.id,
                    categoryId = state.selectedCategory?.id
                )

                if (state.isEditing) {
                    repository.updateTransaction(transaction)
                } else {
                    repository.addTransaction(transaction)
                }
            }
        }

        _uiState.value = state.copy(isSaved = true)
        return true
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun getFilteredCategories(): List<CategoryUi> {
        val isIncome = _uiState.value.type == TransactionType.INCOME
        return _uiState.value.categories.filter { it.isIncome == isIncome }
    }

    fun delete() {
        val id = _uiState.value.transactionId
        if (id != null) {
            viewModelScope.launch {
                repository.deleteTransaction(id.toString())
                _uiState.value = _uiState.value.copy(isDeleted = true)
            }
        }
    }

    fun copy() {
        _uiState.value = _uiState.value.copy(
            transactionId = null,
            isEditing = false,
            date = System.currentTimeMillis()
        )
    }
}