package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.Transaction
import com.mobilemoney.domain.repository.SyncRepository
import com.mobilemoney.domain.usecase.transaction.GetTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TransactionListViewModel(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            getTransactionsUseCase()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { transactions ->
                    _uiState.value = _uiState.value.copy(
                        transactions = transactions,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun refreshTransactions() {
        loadTransactions()
    }

    fun deleteTransaction(id: java.util.UUID) {
        viewModelScope.launch {
            // TODO: inject delete use case
        }
    }
}