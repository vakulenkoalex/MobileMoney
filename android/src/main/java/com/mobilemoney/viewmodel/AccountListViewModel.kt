package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.domain.model.Account
import com.mobilemoney.domain.usecase.account.GetAccountsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class AccountListState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AccountListViewModel(
    private val getAccountsUseCase: GetAccountsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountListState())
    val uiState: StateFlow<AccountListState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getAccountsUseCase()
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