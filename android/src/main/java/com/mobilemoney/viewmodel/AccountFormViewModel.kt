package com.mobilemoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemoney.ui.config.AccountIcons
import com.mobilemoney.ui.config.Currencies
import com.mobilemoney.ui.config.CurrencyConfig
import com.mobilemoney.ui.config.IconOption
import com.mobilemoney.domain.model.Account
import com.mobilemoney.domain.model.AccountType
import com.mobilemoney.domain.usecase.account.CreateAccountUseCase
import com.mobilemoney.domain.usecase.account.GetAccountsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.mobilemoney.ui.common.ErrorHandler
import com.mobilemoney.ui.common.FormField
import java.util.UUID

data class AccountFormState(
    val name: FormField = FormField(label = "Название счёта"),
    val type: AccountType = AccountType.CASH,
    val currencyCode: String = "RUB",
    val currencySymbol: String = "₽",
    val icon: String = "wallet",
    val isDefault: Boolean = false,
    val isEditing: Boolean = false,
    val accountId: UUID? = null,
    val currencies: List<CurrencyConfig> = Currencies.all,
    val icons: List<IconOption> = AccountIcons.all,
    val accountTypes: List<AccountType> = AccountType.entries,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,
    val autoCreateEnabled: Boolean = false,
    val cardMask: FormField = FormField(label = "Маска карты / идентификатор")
)

class AccountFormViewModel(
    private val getAccountsUseCase: GetAccountsUseCase,
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountFormState())
    val uiState: StateFlow<AccountFormState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = AccountFormState()
    }

    fun loadAccount(accountId: UUID) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val account = getAccountsUseCase().first().find { it.id == accountId }
            if (account != null) {
                val currency = uiState.value.currencies.find { it.code == account.currency }
                _uiState.value = _uiState.value.copy(
                    name = _uiState.value.name.withValue(account.name),
                    type = account.type,
                    currencyCode = account.currency,
                    currencySymbol = currency?.symbol ?: "₽",
                    icon = account.icon,
                    isDefault = account.isDefault,
                    isEditing = true,
                    accountId = accountId,
                    autoCreateEnabled = account.autoCreateEnabled,
                    cardMask = _uiState.value.cardMask.withValue(account.cardMask ?: ""),
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
        _uiState.value = _uiState.value.copy(
            name = _uiState.value.name.withValue(name)
        )
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

    fun updateAutoCreateEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoCreateEnabled = enabled)
    }

    fun updateCardMask(mask: String) {
        _uiState.value = _uiState.value.copy(
            cardMask = _uiState.value.cardMask.withValue(mask)
        )
    }

    fun save(): Boolean {
        val state = _uiState.value
        var hasError = false

        val cleanName = state.name.validate()
        if (!cleanName.isValid) {
            _uiState.value = state.copy(name = cleanName)
            hasError = true
        }

        if (state.autoCreateEnabled) {
            val cleanCardMask = state.cardMask.validate()
            if (!cleanCardMask.isValid) {
                _uiState.value = state.copy(cardMask = cleanCardMask)
                hasError = true
            }
        }

        if (hasError) {
            viewModelScope.launch {
                ErrorHandler.emitError("Заполните обязательные поля")
            }
            return false
        }

        val account = Account(
            id = state.accountId ?: UUID.randomUUID(),
            name = state.name.value,
            type = state.type,
            currency = state.currencyCode,
            icon = state.icon,
            isDefault = state.isDefault,
            autoCreateEnabled = state.autoCreateEnabled,
            cardMask = state.cardMask.value.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            createAccountUseCase(account, state.isDefault)
        }

        _uiState.value = state.copy(isSaved = true)
        return true
    }
}
