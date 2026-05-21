package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.Account
import com.mobilemoney.domain.model.Category
import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.domain.usecase.account.GetAccountsUseCase
import com.mobilemoney.domain.usecase.category.GetCategoriesUseCase
import com.mobilemoney.domain.usecase.transaction.DeleteTransactionUseCase
import com.mobilemoney.domain.usecase.transaction.GetTransactionsUseCase
import com.mobilemoney.domain.usecase.transaction.SaveTransactionUseCase
import com.mobilemoney.domain.repository.TransactionRepository
import com.mobilemoney.domain.model.TransactionOrigin
import com.mobilemoney.data.local.TransactionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.mobilemoney.ui.common.ErrorHandler
import java.util.UUID

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

data class TransactionFormState(
    val amount: String = "",
    val selectedAccount: Account? = null,
    val targetAccount: Account? = null,
    val selectedCategory: Category? = null,
    val date: Long = System.currentTimeMillis(),
    val comment: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val isEditing: Boolean = false,
    val transactionId: UUID? = null,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isSplitMode: Boolean = false,
    val splitAmount: String = "",
    val splitCategory: Category? = null,
    val clipboardText: String? = null,
    val source: TransactionSource = TransactionSource.MANUAL,
    val sourceData: String = "",
    val shop: String = "",
    val pendingAccountId: UUID? = null,
    val pendingCategoryId: UUID? = null
)

class TransactionFormViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val saveTransactionUseCase: SaveTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val transactionRepository: com.mobilemoney.domain.repository.TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormState())
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            getAccountsUseCase()
                .catch { }
                .collect { accounts ->
                    _uiState.value = _uiState.value.copy(accounts = accounts)
                    if (!uiState.value.isEditing) {
                        val pendingAccount = uiState.value.pendingAccountId?.let { id ->
                            accounts.find { it.id == id }
                        }
                        val defaultAccount = pendingAccount
                            ?: accounts.find { it.isDefault }
                            ?: accounts.firstOrNull()
                        _uiState.value = _uiState.value.copy(selectedAccount = defaultAccount)
                    }
                }
        }
        viewModelScope.launch {
            getCategoriesUseCase()
                .catch { }
                .collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                    val pendingCategory = uiState.value.pendingCategoryId?.let { id ->
                        categories.find { it.id == id }
                    }
                    if (pendingCategory != null) {
                        _uiState.value = _uiState.value.copy(selectedCategory = pendingCategory, pendingCategoryId = null)
                    }
                }
        }
    }

    fun resetForNewTransaction() {
        val defaultAccount = _uiState.value.accounts.find { it.isDefault } ?: _uiState.value.accounts.firstOrNull()
        _uiState.value = TransactionFormState(
            accounts = _uiState.value.accounts,
            categories = _uiState.value.categories,
            selectedAccount = defaultAccount,
            date = System.currentTimeMillis(),
            pendingAccountId = null,
            pendingCategoryId = null
        )
    }

    fun resetSavedState() {
        _uiState.value = _uiState.value.copy(isSaved = false, isDeleted = false, isEditing = false, transactionId = null)
    }

    data class ClipboardPrefillData(
        val amount: String,
        val accountId: UUID,
        val comment: String,
        val shop: String?,
        val categoryId: UUID?,
        val clipboardText: String
    )

    fun prefillFromClipboard(data: ClipboardPrefillData) {
        val account = _uiState.value.accounts.find { it.id == data.accountId }
        val category = _uiState.value.categories.find { it.id == data.categoryId }
        _uiState.value = _uiState.value.copy(
            amount = data.amount,
            selectedAccount = account,
            pendingAccountId = if (account == null) data.accountId else null,
            selectedCategory = category,
            pendingCategoryId = if (category == null) data.categoryId else null,
            comment = data.comment,
            type = TransactionType.EXPENSE,
            date = System.currentTimeMillis(),
            isEditing = false,
            transactionId = null,
            isSaved = false,
            clipboardText = data.clipboardText,
            source = TransactionSource.CLIPBOARD,
            sourceData = data.clipboardText,
            shop = data.shop ?: ""
        )
    }

    fun loadTransaction(transactionId: UUID) {
        viewModelScope.launch {
            val transaction = getTransactionsUseCase().first().find { it.id == transactionId }
            if (transaction != null) {
                val accounts = _uiState.value.accounts
                val categories = _uiState.value.categories

                val targetAccount = if (transaction.relatedTransactionId != null) {
                    val relatedTx = getTransactionsUseCase().first()
                        .find { it.relatedTransactionId == transactionId && it.id != transaction.id }
                    relatedTx?.accountId?.let { tid -> accounts.find { it.id == tid } }
                } else null

                val txSource = when (transaction.origin) {
                    TransactionOrigin.CLIPBOARD -> TransactionSource.CLIPBOARD
                    TransactionOrigin.MANUAL -> TransactionSource.MANUAL
                }

                _uiState.value = _uiState.value.copy(
                    amount = transaction.amount.toString(),
                    selectedAccount = accounts.find { it.id == transaction.accountId },
                    targetAccount = targetAccount,
                    selectedCategory = categories.find { it.id == transaction.categoryId },
                    date = transaction.date,
                    comment = transaction.comment,
                    type = when {
                        transaction.relatedTransactionId != null -> TransactionType.TRANSFER
                        transaction.isIncome -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    },
                    isEditing = true,
                    transactionId = transactionId,
                    source = txSource,
                    sourceData = transaction.sourceData ?: "",
                    shop = transaction.shop ?: ""
                )
            }
        }
    }

    fun updateAmount(value: String) {
        _uiState.value = _uiState.value.copy(amount = value)
    }

    fun updateAccount(account: Account) {
        _uiState.value = _uiState.value.copy(selectedAccount = account)
    }

    fun updateCategory(category: Category?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateDate(date: Long) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateComment(comment: String) {
        _uiState.value = _uiState.value.copy(comment = comment)
    }

    fun updateSource(source: TransactionSource) {
        _uiState.value = _uiState.value.copy(source = source)
    }

    fun updateSourceData(value: String) {
        _uiState.value = _uiState.value.copy(sourceData = value)
    }

    fun updateShop(value: String) {
        _uiState.value = _uiState.value.copy(shop = value)
    }

    fun updateType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun updateTargetAccount(account: Account) {
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

    fun updateSplitCategory(category: Category) {
        _uiState.value = _uiState.value.copy(splitCategory = category)
    }

    fun getRemainingAmount(): Double {
        val total = _uiState.value.amount.toDoubleOrNull() ?: 0.0
        val split = _uiState.value.splitAmount.toDoubleOrNull() ?: 0.0
        return total - split
    }

    fun getSplitFilteredCategories(): List<Category> {
        val isIncome = _uiState.value.type == TransactionType.INCOME
        return _uiState.value.categories.filter { it.isIncome == isIncome }
    }

    fun save(): Boolean {
        val state = _uiState.value

        if (state.amount.isBlank() || state.amount.toDoubleOrNull() == null || state.amount.toDouble() <= 0) {
            kotlinx.coroutines.GlobalScope.launch {
                ErrorHandler.emitError("Введите корректную сумму")
            }
            return false
        }

        if (state.selectedAccount == null) {
            kotlinx.coroutines.GlobalScope.launch {
                ErrorHandler.emitError("Выберите счёт")
            }
            return false
        }

        if (state.type == TransactionType.TRANSFER && state.targetAccount == null) {
            kotlinx.coroutines.GlobalScope.launch {
                ErrorHandler.emitError("Выберите целевой счёт")
            }
            return false
        }

        if (state.type != TransactionType.TRANSFER && state.selectedCategory == null) {
            kotlinx.coroutines.GlobalScope.launch {
                ErrorHandler.emitError("Выберите категорию")
            }
            return false
        }

        if (state.isSplitMode) {
            val splitAmount = state.splitAmount.toDoubleOrNull() ?: 0.0
            if (splitAmount <= 0) {
                kotlinx.coroutines.GlobalScope.launch {
                    ErrorHandler.emitError("Введите сумму для разделения")
                }
                return false
            }
            if (state.splitCategory == null) {
                kotlinx.coroutines.GlobalScope.launch {
                    ErrorHandler.emitError("Выберите категорию для новой операции")
                }
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
            try {
                if (state.type == TransactionType.TRANSFER) {
                    val transferId = UUID.randomUUID()
                    val amount = state.amount.toDouble()

                    val expenseTransaction = Transaction(
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

                    val incomeTransaction = Transaction(
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

                    saveTransactionUseCase(expenseTransaction, false)
                    saveTransactionUseCase(incomeTransaction, false)
                    _uiState.update { it.copy(isSaved = true) }
                } else if (state.isSplitMode) {
                    val splitAmount = state.splitAmount.toDoubleOrNull() ?: 0.0
                    val remainingAmount = state.amount.toDoubleOrNull()!! - splitAmount

                    val splitIcon = state.splitCategory?.icon ?: "category"
                    val newTransaction = Transaction(
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

                    if (state.isEditing && state.transactionId != null) {
                        transactionRepository.splitTransaction(
                            originalId = state.transactionId.toString(),
                            mainAmount = remainingAmount,
                            newTransaction = newTransaction
                        )
                    } else {
                        val mainTransaction = Transaction(
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
                        transactionRepository.addTransaction(mainTransaction)
                        transactionRepository.addTransaction(newTransaction)
                    }
                    _uiState.update { it.copy(isSaved = true) }
                } else {
                    val transactionOrigin = when (state.source) {
                        TransactionSource.CLIPBOARD -> TransactionOrigin.CLIPBOARD
                        TransactionSource.MANUAL, TransactionSource.SMS, TransactionSource.PUSH -> TransactionOrigin.MANUAL
                    }
                    val transactionTitle = if (state.clipboardText != null && state.selectedCategory == null) state.comment.ifBlank { "Без категории" } else title

                    val transaction = Transaction(
                        id = state.transactionId ?: UUID.randomUUID(),
                        title = transactionTitle,
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
                        categoryId = state.selectedCategory?.id,
                        shop = state.shop.takeIf { it.isNotBlank() },
                        origin = transactionOrigin,
                        sourceData = state.sourceData.takeIf { it.isNotBlank() }
                    )

                    val result = saveTransactionUseCase(transaction, state.isEditing)
                    if (result is SaveTransactionUseCase.Result.Error) {
                        kotlinx.coroutines.GlobalScope.launch {
                            ErrorHandler.emitError(result.message)
                        }
                        return@launch
                    }
                    _uiState.update { it.copy(isSaved = true) }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.GlobalScope.launch {
                    ErrorHandler.emitError(e.message ?: "Ошибка сохранения")
                }
            }
        }
        return true
    }

    fun getFilteredCategories(): List<Category> {
        val isIncome = _uiState.value.type == TransactionType.INCOME
        return _uiState.value.categories.filter { it.isIncome == isIncome }
    }

    fun delete() {
        val id = _uiState.value.transactionId
        if (id != null) {
            viewModelScope.launch {
                deleteTransactionUseCase(id.toString())
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