package com.mobilemoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class AccountListState(
    val accounts: List<AccountUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AccountListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository = MobileMoneyApp.getRepository(application)

    private val _uiState = MutableStateFlow(AccountListState())
    val uiState: StateFlow<AccountListState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAccounts()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { accounts ->
                    _uiState.value = _uiState.value.copy(
                        accounts = accounts,
                        isLoading = false
                    )
                }
        }
    }
}