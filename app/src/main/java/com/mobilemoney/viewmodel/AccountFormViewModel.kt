package com.mobilemoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.data.config.AccountIcons
import com.mobilemoney.data.config.Currencies
import com.mobilemoney.data.model.AccountUi
import com.mobilemoney.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class AccountFormState(
    val name: String = "",
    val typeId: String = "cash",
    val currencyCode: String = "RUB",
    val currencySymbol: String = "₽",
    val icon: String = "wallet",
    val isDefault: Boolean = false,
    val isEditing: Boolean = false,
    val accountId: UUID? = null,
    val currencies: List<com.mobilemoney.data.config.CurrencyConfig> = Currencies.all,
    val icons: List<com.mobilemoney.data.config.IconOption> = AccountIcons.all,
    val accountTypes: List<com.mobilemoney.data.local.AccountTypeEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

class AccountFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository = MobileMoneyApp.getRepository(application)

    private val _uiState = MutableStateFlow(AccountFormState())
    val uiState: StateFlow<AccountFormState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAccountTypes().collect { types ->
                _uiState.value = _uiState.value.copy(accountTypes = types)
            }
        }
    }

    fun loadAccount(accountId: UUID) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val account = repository.getAccounts().first().find { it.id == accountId }
            if (account != null) {
                val currency = uiState.value.currencies.find { it.code == account.currency }
                _uiState.value = _uiState.value.copy(
                    name = account.name,
                    typeId = account.typeId,
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

    fun updateTypeId(typeId: String) {
        _uiState.value = _uiState.value.copy(typeId = typeId)
    }

    fun save(): Boolean {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Введите название счёта")
            return false
        }

        val account = AccountUi(
            id = state.accountId ?: UUID.randomUUID(),
            name = state.name,
            typeId = state.typeId,
            currency = state.currencyCode,
            icon = state.icon,
            isDefault = state.isDefault
        )

        viewModelScope.launch {
            if (state.isDefault) {
                repository.clearDefaultAccounts()
            }
            if (state.isEditing) {
                repository.updateAccount(account)
            } else {
                repository.addAccount(account)
            }
        }

        _uiState.value = state.copy(isSaved = true)
        return true
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}