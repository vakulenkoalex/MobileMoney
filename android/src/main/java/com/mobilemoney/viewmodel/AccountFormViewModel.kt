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
    val isSaved: Boolean = false,
    val autoCreateEnabled: Boolean = false,
    val cardMask: String = "",
    val regexForText: String = "",
    val cardMaskError: String? = null,
    val regexError: String? = null
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
                    name = account.name,
                    type = account.type,
                    currencyCode = account.currency,
                    currencySymbol = currency?.symbol ?: "₽",
                    icon = account.icon,
                    isDefault = account.isDefault,
                    isEditing = true,
                    accountId = accountId,
                    autoCreateEnabled = account.autoCreateEnabled,
                    cardMask = account.cardMask ?: "",
                    regexForText = account.regexForText ?: "",
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

    fun updateAutoCreateEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoCreateEnabled = enabled)
    }

    fun updateCardMask(mask: String) {
        _uiState.value = _uiState.value.copy(cardMask = mask, cardMaskError = null)
    }

    fun updateRegexForText(regex: String) {
        _uiState.value = _uiState.value.copy(regexForText = regex, regexError = null)
    }

    fun validateCardMask(): Boolean {
        val mask = _uiState.value.cardMask
        if (mask.isBlank()) return true
        return if (mask.length == 4 && mask.all { it.isDigit() }) {
            true
        } else {
            _uiState.value = _uiState.value.copy(cardMaskError = "Маска должна содержать ровно 4 цифры")
            false
        }
    }

    fun validateRegex(): Boolean {
        val regex = _uiState.value.regexForText
        if (regex.isBlank()) return true
        return try {
            val r = Regex(regex)
            val testText = "*1234 оплата 100.00 р. TEST. Баланс 500.00"
            val match = r.find(testText)
            val requiredGroups = listOf("amount", "shop", "cardMask", "balance")
            val allPresent = requiredGroups.all { match?.groups[it]?.value != null }
            if (!allPresent) {
                _uiState.value = _uiState.value.copy(regexError = "Regex должен содержать именованные группы: amount, shop, cardMask, balance")
                false
            } else {
                true
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(regexError = "Неверный regex: ${e.message}")
            false
        }
    }

    fun save(): Boolean {
        val state = _uiState.value

        if (state.name.isBlank()) {
            GlobalScope.launch {
                ErrorHandler.emitError("Введите название счёта")
            }
            return false
        }

        if (state.autoCreateEnabled) {
            var hasError = false
            if (state.cardMask.isBlank()) {
                _uiState.value = _uiState.value.copy(cardMaskError = "Заполните маску")
                hasError = true
            }
            if (state.regexForText.isBlank()) {
                _uiState.value = _uiState.value.copy(regexError = "Заполните regex")
                hasError = true
            }
            if (hasError) return false
            if (!validateCardMask()) return false
            if (!validateRegex()) return false
        }

        val account = Account(
            id = state.accountId ?: UUID.randomUUID(),
            name = state.name,
            type = state.type,
            currency = state.currencyCode,
            icon = state.icon,
            isDefault = state.isDefault,
            autoCreateEnabled = state.autoCreateEnabled,
            cardMask = state.cardMask.takeIf { it.isNotBlank() },
            regexForText = state.regexForText.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            createAccountUseCase(account, state.isDefault)
        }

        _uiState.value = state.copy(isSaved = true)
        return true
    }

}