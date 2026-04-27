package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import com.mobilemoney.data.model.TransactionUi
import com.mobilemoney.data.repository.MockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TransactionListUiState(
    val transactions: List<TransactionUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TransactionListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        _uiState.value = TransactionListUiState(
            transactions = MockRepository.transactions.value,
            isLoading = false
        )
    }

    fun refreshTransactions() {
        loadTransactions()
    }

    fun deleteTransaction(id: java.util.UUID) {
        MockRepository.deleteTransaction(id)
        loadTransactions()
    }
}