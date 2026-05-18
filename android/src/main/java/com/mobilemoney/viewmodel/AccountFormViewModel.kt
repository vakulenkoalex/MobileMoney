package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.data.config.AccountIcons
import com.mobilemoney.data.config.Currencies
import com.mobilemoney.domain.model.Account
import com.mobilemoney.domain.model.AccountType
import com.mobilemoney.domain.usecase.account.CreateAccountUseCase
import com.mobilemoney.domain.usecase.account.GetAccountsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import com.mobilemoney.ui.common.ErrorHandler
import java.util.UUID

data class AccountFormState(
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val currencyCode: String = "RUB",
    val currencySymbol: String = "₽",
    val icon: String = "wallet",
    val isDefault: Boolean = false,
    val isEditing: Boolean = false,
    val accountId: UUID? = null,
    val currencies: List<com.mobilemoney.data.config.CurrencyConfig> = Currencies.all,
    val icons: List<com.mobilemoney.data.config.IconOption> = AccountIcons.all,
    val accountTypes: List<AccountType> = AccountType.entries,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class AccountFormViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountFormState())
    val uiState: StateFlow<AccountFormState> = _uiState.asStateFlow()

    fun loadAccount(accountId: UUID) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val account = getAccountsUseCase().first().find { it.id == accountId }
            if (account != null) {
                val currency = uiState.value.currencies.find { it.code == account.currency }
                _uiState.value = _uiState.value.copy(
                    name = account.name,
                    type = account.type,
                    currencyCode = account.currency,
                    currencySymbol = currency?.symbol ?: "₽",
                    icon = account.icon,
                    isDefault = account.isDefault,
                    isEditing = true,
                    accountId = accountId,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Счёт не найден"
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateCurrency(currencyCode: String) {
        val currency = uiState.value.currencies.find { it.code == currencyCode }
        _uiState.value = _uiState.value.copy(
            currencyCode = currencyCode,
            currencySymbol = currency?.symbol ?: "₽"
        )
    }

    fun updateIcon(icon: String) {
        _uiState.value = _uiState.value.copy(icon = icon)
    }

    fun updateIsDefault(isDefault: Boolean) {
        _uiState.value = _uiState.value.copy(isDefault = isDefault)
    }

    fun updateType(type: AccountType) {
        _uiState.value = _uiState.value.copy(type = type)
    }

    fun save(): Boolean {
        val state = _uiState.value

        if (state.name.isBlank()) {
            GlobalScope.launch {
                ErrorHandler.emitError("Введите название счёта")
            }
            return false
        }

        val account = Account(
            id = state.accountId ?: UUID.randomUUID(),
            name = state.name,
            type = state.type,
            currency = state.currencyCode,
            icon = state.icon,
            isDefault = state.isDefault
        )

        viewModelScope.launch {
            createAccountUseCase(account, state.isDefault)
        }

        _uiState.value = state.copy(isSaved = true)
        return true
    }

}