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
import com.mobilemoney.domain.model.TransactionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.mobilemoney.dto.TransferConstants
import com.mobilemoney.ui.common.ErrorHandler
import com.mobilemoney.ui.common.FieldState
import com.mobilemoney.ui.common.FormField
import java.util.UUID

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

data class TransactionFormState(
    val amount: FormField = FormField(label = "Сумма"),
    val selectedAccount: FieldState<Account> = FieldState(),
    val targetAccount: FieldState<Account> = FieldState(),
    val selectedCategory: FieldState<Category> = FieldState(),
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
    private val transactionRepository: TransactionRepository
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
                        _uiState.value = _uiState.value.copy(selectedAccount = _uiState.value.selectedAccount.withValue(defaultAccount))
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
                        _uiState.value = _uiState.value.copy(selectedCategory = _uiState.value.selectedCategory.withValue(pendingCategory), pendingCategoryId = null)
                    }
                }
        }
    }

    fun resetForNewTransaction() {
        val defaultAccount = _uiState.value.accounts.find { it.isDefault } ?: _uiState.value.accounts.firstOrNull()
        _uiState.value = TransactionFormState(
            accounts = _uiState.value.accounts,
            categories = _uiState.value.categories,
            selectedAccount = _uiState.value.selectedAccount.withValue(defaultAccount),
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
        val isIncome: Boolean = false,
        val clipboardText: String
    )

    fun prefillFromClipboard(data: ClipboardPrefillData) {
        val account = _uiState.value.accounts.find { it.id == data.accountId }
        val category = _uiState.value.categories.find { it.id == data.categoryId }
        _uiState.value = _uiState.value.copy(
            amount = _uiState.value.amount.withValue(data.amount),
            selectedAccount = _uiState.value.selectedAccount.withValue(account),
            pendingAccountId = if (account == null) data.accountId else null,
            selectedCategory = _uiState.value.selectedCategory.withValue(category),
            pendingCategoryId = if (category == null) data.categoryId else null,
            comment = data.comment,
            type = if (data.isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
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
                    val relatedTx = transactionRepository.getRelatedTransaction(
                        transaction.relatedTransactionId.toString(), transaction.id.toString()
                    )
                    relatedTx?.accountId?.let { tid -> accounts.find { it.id == tid } }
                } else null

                _uiState.value = _uiState.value.copy(
                    amount = _uiState.value.amount.withValue(transaction.amount.toString()),
                    selectedAccount = _uiState.value.selectedAccount.withValue(accounts.find { it.id == transaction.accountId }),
                    targetAccount = _uiState.value.targetAccount.withValue(targetAccount),
                    selectedCategory = _uiState.value.selectedCategory.withValue(categories.find { it.id == transaction.categoryId }),
                    date = transaction.date,
                    comment = transaction.comment,
                    type = when {
                        transaction.relatedTransactionId != null -> TransactionType.TRANSFER
                        transaction.isIncome -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    },
                    isEditing = true,
                    transactionId = transactionId,
                    source = transaction.source,
                    sourceData = transaction.sourceData ?: "",
                    shop = transaction.shop ?: ""
                )
            }
        }
    }

    fun updateAmount(value: String) {
        _uiState.value = _uiState.value.copy(
            amount = _uiState.value.amount.withValue(value)
        )
    }

    fun updateAccount(account: Account) {
        _uiState.value = _uiState.value.copy(
            selectedAccount = _uiState.value.selectedAccount.withValue(account)
        )
    }

    fun updateCategory(category: Category?) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = _uiState.value.selectedCategory.withValue(category)
        )
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
        _uiState.value = _uiState.value.copy(
            targetAccount = _uiState.value.targetAccount.withValue(account)
        )
    }

    fun enableSplitMode() {
        _uiState.value = _uiState.value.copy(isSplitMode = true, splitAmount = "")
    }

    fun disableSplitMode() {
        _uiState.value = _uiState.value.copy(isSplitMode = false, splitAmount = "", splitCategory = null)
    }

    fun updateSplitAmount(value: String) {
        val parsed = value.toDoubleOrNull() ?: 0.0
        val currentAmount = _uiState.value.amount.value.toDoubleOrNull() ?: 0.0
        if (parsed <= currentAmount) {
            _uiState.value = _uiState.value.copy(splitAmount = value)
        }
    }

    fun updateSplitCategory(category: Category) {
        _uiState.value = _uiState.value.copy(splitCategory = category)
    }

    fun getRemainingAmount(): Double {
        val total = _uiState.value.amount.value.toDoubleOrNull() ?: 0.0
        val split = _uiState.value.splitAmount.toDoubleOrNull() ?: 0.0
        return total - split
    }

    fun save(): Boolean {
        val state = _uiState.value

        val cleanAmount = state.amount.validate()
        if (!cleanAmount.isValid) {
            _uiState.value = state.copy(amount = cleanAmount)
            viewModelScope.launch {
                ErrorHandler.emitError("Введите корректную сумму")
            }
            return false
        }
        if (state.amount.value.toDoubleOrNull() == null || state.amount.value.toDouble() <= 0) {
            _uiState.value = state.copy(amount = state.amount.copy(error = "Введите корректную сумму"))
            viewModelScope.launch {
                ErrorHandler.emitError("Введите корректную сумму")
            }
            return false
        }

        val cleanAccount = state.selectedAccount.validate("Выберите счёт")
        if (!cleanAccount.isValid) {
            _uiState.value = state.copy(selectedAccount = cleanAccount)
            viewModelScope.launch {
                ErrorHandler.emitError("Выберите счёт")
            }
            return false
        }

        if (state.type == TransactionType.TRANSFER) {
            val cleanTarget = state.targetAccount.validate("Выберите целевой счёт")
            if (!cleanTarget.isValid) {
                _uiState.value = state.copy(targetAccount = cleanTarget)
                viewModelScope.launch {
                    ErrorHandler.emitError("Выберите целевой счёт")
                }
                return false
            }
        }

        val account = state.selectedAccount.value!!
        val targetAccount = state.targetAccount.value

        if (state.type != TransactionType.TRANSFER) {
            val cleanCategory = state.selectedCategory.validate("Выберите категорию")
            if (!cleanCategory.isValid) {
                _uiState.value = state.copy(selectedCategory = cleanCategory)
                viewModelScope.launch {
                    ErrorHandler.emitError("Выберите категорию")
                }
                return false
            }
        }

        if (state.isSplitMode) {
            val splitAmount = state.splitAmount.toDoubleOrNull() ?: 0.0
            if (splitAmount <= 0) {
                viewModelScope.launch {
                    ErrorHandler.emitError("Введите сумму для разделения")
                }
                return false
            }
            if (state.splitCategory == null) {
                viewModelScope.launch {
                    ErrorHandler.emitError("Выберите категорию для новой операции")
                }
                return false
            }
        }

        val isIncome = state.type == TransactionType.INCOME
        val icon = when (state.type) {
            TransactionType.TRANSFER -> "swap_horiz"
            TransactionType.INCOME -> state.selectedCategory.value?.icon ?: "work"
            TransactionType.EXPENSE -> state.selectedCategory.value?.icon ?: "shopping_cart"
        }

        val title = when (state.type) {
            TransactionType.TRANSFER -> "Перевод"
            else -> state.selectedCategory.value?.name ?: "Без категории"
        }

        val subtitle = when (state.type) {
            TransactionType.TRANSFER -> "${account.name} → ${targetAccount?.name}"
            else -> account.name
        }

        viewModelScope.launch {
            try {
                if (state.type == TransactionType.TRANSFER) {
                    var oldTx: Transaction? = null
                    var oldPartner: Transaction? = null
                    if (state.isEditing && state.transactionId != null) {
                        oldTx = transactionRepository.getTransactionById(state.transactionId.toString())
                        if (oldTx?.relatedTransactionId != null) {
                            oldPartner = transactionRepository.getRelatedTransaction(
                                oldTx.relatedTransactionId.toString(), state.transactionId.toString()
                            )
                        }
                    }

                    val transferId = oldTx?.relatedTransactionId ?: UUID.randomUUID()
                    val amount = state.amount.value.toDouble()

                    val expenseTransaction = Transaction(
                        id = oldTx?.takeIf { !it.isIncome }?.id
                            ?: oldPartner?.takeIf { !it.isIncome }?.id
                            ?: UUID.randomUUID(),
                        title = title,
                        subtitle = subtitle,
                        comment = state.comment,
                        amount = amount,
                        currency = account.currency,
                        icon = icon,
                        color = 0xFF9C27B0,
                        isIncome = false,
                        date = state.date,
                        accountId = account.id,
                        categoryId = UUID.fromString(TransferConstants.EXPENSE_CATEGORY_ID),
                        relatedTransactionId = transferId
                    )

                    val incomeTransaction = Transaction(
                        id = oldTx?.takeIf { it.isIncome }?.id
                            ?: oldPartner?.takeIf { it.isIncome }?.id
                            ?: UUID.randomUUID(),
                        title = title,
                        subtitle = subtitle,
                        comment = state.comment,
                        amount = amount,
                        currency = targetAccount?.currency ?: account.currency,
                        icon = icon,
                        color = 0xFF9C27B0,
                        isIncome = true,
                        date = state.date,
                        accountId = targetAccount?.id,
                        categoryId = UUID.fromString(TransferConstants.INCOME_CATEGORY_ID),
                        relatedTransactionId = transferId
                    )

                    transactionRepository.addTransaction(expenseTransaction)
                    transactionRepository.addTransaction(incomeTransaction)
                    _uiState.update { it.copy(isSaved = true) }
                } else if (state.isSplitMode) {
                    val splitAmount = state.splitAmount.toDoubleOrNull() ?: 0.0
                    val remainingAmount = state.amount.value.toDoubleOrNull()!! - splitAmount

                    val splitIcon = state.splitCategory?.icon ?: "category"
                    val newTransaction = Transaction(
                        id = UUID.randomUUID(),
                        title = state.splitCategory?.name ?: "Без категории",
                        subtitle = account.name,
                        comment = state.comment,
                        amount = splitAmount,
                        currency = account.currency,
                        icon = splitIcon,
                        color = if (isIncome) 0xFF2E7D32 else 0xFFD32F2F,
                        isIncome = isIncome,
                        date = state.date,
                        accountId = account.id,
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
                            title = state.selectedCategory.value?.name ?: "Без категории",
                            subtitle = account.name,
                            comment = state.comment,
                            amount = remainingAmount,
                            currency = account.currency,
                            icon = state.selectedCategory.value?.icon ?: "shopping_cart",
                            color = if (isIncome) 0xFF2E7D32 else 0xFFD32F2F,
                            isIncome = isIncome,
                            date = state.date,
                            accountId = account.id,
                            categoryId = state.selectedCategory.value?.id
                        )
                        transactionRepository.addTransaction(mainTransaction)
                        transactionRepository.addTransaction(newTransaction)
                    }
                    _uiState.update { it.copy(isSaved = true) }
                } else {
                    if (state.isEditing && state.transactionId != null) {
                        val oldTx = transactionRepository.getTransactionById(state.transactionId.toString())
                        if (oldTx?.relatedTransactionId != null) {
                            val oldPartner = transactionRepository.getRelatedTransaction(
                                oldTx.relatedTransactionId.toString(), state.transactionId.toString()
                            )
                            if (oldPartner != null) deleteTransactionUseCase(oldPartner.id.toString())
                        }
                    }

                    val transactionTitle = if (state.clipboardText != null && state.selectedCategory.value == null) state.comment.ifBlank { "Без категории" } else title

                    val transaction = Transaction(
                        id = state.transactionId ?: UUID.randomUUID(),
                        title = transactionTitle,
                        subtitle = subtitle,
                        comment = state.comment,
                        amount = state.amount.value.toDouble(),
                        currency = account.currency,
                        icon = icon,
                        color = when (state.type) {
                            TransactionType.TRANSFER -> 0xFF9C27B0
                            TransactionType.INCOME -> 0xFF2E7D32
                            TransactionType.EXPENSE -> 0xFFD32F2F
                        },
                        isIncome = isIncome,
                        date = state.date,
                        accountId = account.id,
                        categoryId = state.selectedCategory.value?.id,
                        shop = state.shop.takeIf { it.isNotBlank() },
                        source = state.source,
                        sourceData = state.sourceData.takeIf { it.isNotBlank() }
                    )

                    val result = saveTransactionUseCase(transaction, state.isEditing)
                    if (result is SaveTransactionUseCase.Result.Error) {
                        ErrorHandler.emitError(result.message)
                        return@launch
                    }
                    _uiState.update { it.copy(isSaved = true) }
                }
            } catch (e: Exception) {
                ErrorHandler.emitError(e.message ?: "Ошибка сохранения")
            }
        }
        return true
    }

    fun getFilteredCategories(): List<Category> {
        val isIncome = _uiState.value.type == TransactionType.INCOME
        return _uiState.value.categories.filter { it.isIncome == isIncome }.sortedBy { it.name }
    }

    fun getRootCategories(): List<Category> {
        val isIncome = _uiState.value.type == TransactionType.INCOME
        return _uiState.value.categories
            .filter { it.isIncome == isIncome && it.parentId == null }
            .sortedBy { it.name }
    }

    fun getCategoryWithChildren(parentId: UUID): List<Category> {
        val isIncome = _uiState.value.type == TransactionType.INCOME
        val parent = _uiState.value.categories.find { it.id == parentId } ?: return emptyList()
        val children = _uiState.value.categories
            .filter { it.parentId == parentId && it.isIncome == isIncome }
            .sortedBy { it.name }
        return listOf(parent) + children
    }

    fun delete() {
        val id = _uiState.value.transactionId
        if (id != null) {
            viewModelScope.launch {
                val oldTx = transactionRepository.getTransactionById(id.toString())
                if (oldTx?.relatedTransactionId != null) {
                    val oldPartner = transactionRepository.getRelatedTransaction(
                        oldTx.relatedTransactionId.toString(), id.toString()
                    )
                    deleteTransactionUseCase(id.toString())
                    if (oldPartner != null) deleteTransactionUseCase(oldPartner.id.toString())
                } else {
                    deleteTransactionUseCase(id.toString())
                }
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