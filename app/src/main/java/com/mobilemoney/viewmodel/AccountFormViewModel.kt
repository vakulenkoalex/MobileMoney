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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class AccountFormState(
    val name: String = "",
    val currencyCode: String = "RUB",
    val currencySymbol: String = "₽",
    val icon: String = "wallet",
    val isEditing: Boolean = false,
    val accountId: UUID? = null,
    val currencies: List<CurrencyOption> = listOf(
        CurrencyOption("RUB", "₽", "Российский рубль"),
        CurrencyOption("USD", "$", "Доллар США"),
        CurrencyOption("EUR", "€", "Евро"),
        CurrencyOption("KZT", "₸", "Казахстанский тенге")
    ),
    val icons: List<IconOption> = listOf(
        IconOption("wallet", "Наличные"),
        IconOption("credit_card", "Карта"),
        IconOption("account_balance", "Счёт"),
        IconOption("savings", "Накопления"),
        IconOption("home", "Дом"),
        IconOption("business", "Бизнес"),
        IconOption("school", "Обучение"),
        IconOption("favorite", "Личное")
    ),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false
)

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String
)

data class IconOption(
    val name: String,
    val label: String
)

class AccountFormViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DatabaseRepository = MobileMoneyApp.getRepository(application)

    private val _uiState = MutableStateFlow(AccountFormState())
    val uiState: StateFlow<AccountFormState> = _uiState.asStateFlow()

    fun loadAccount(accountId: UUID) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val account = repository.getAccounts().first().find { it.id == accountId }
            if (account != null) {
                val currency = uiState.value.currencies.find { it.code == account.currency }
                _uiState.value = _uiState.value.copy(
                    name = account.name,
                    currencyCode = account.currency,
                    currencySymbol = currency?.symbol ?: "₽",
                    icon = account.icon,
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

    fun save(): Boolean {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Введите название счёта")
            return false
        }

        val account = AccountUi(
            id = state.accountId ?: UUID.randomUUID(),
            name = state.name,
            currency = state.currencyCode,
            icon = state.icon
        )

        viewModelScope.launch {
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