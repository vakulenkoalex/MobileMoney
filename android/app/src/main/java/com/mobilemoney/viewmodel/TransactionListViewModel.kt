package com.mobilemoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.data.model.TransactionUi
import com.mobilemoney.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class TransactionListUiState(
    val transactions: List<TransactionUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TransactionListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository = MobileMoneyApp.getRepository(application)

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            repository.getTransactions()
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
            repository.deleteTransaction(id.toString())
        }
    }
}