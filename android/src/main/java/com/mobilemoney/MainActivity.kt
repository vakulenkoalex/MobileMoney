package com.mobilemoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.mobilemoney.di.DI
import com.mobilemoney.ui.common.ErrorHandler
import com.mobilemoney.ui.navigation.MobileMoneyNavigation
import com.mobilemoney.ui.theme.MobileMoneyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            ErrorHandler.errorFlow.collect { error ->
                ErrorHandler.showError(this@MainActivity, error)
            }
        }

        setContent {
            var loginState by remember { mutableStateOf(DI.syncRepository.isLoggedIn()) }

            MobileMoneyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MobileMoneyNavigation(
                        isLoggedIn = loginState,
                        onLoginSuccess = { loginState = true },
                        transactionListViewModel = DI.transactionListViewModel,
                        transactionFormViewModel = DI.transactionFormViewModel,
                        parseClipboardTransactionUseCase = DI.parseClipboardTransactionUseCase,
                        accountListViewModel = DI.accountListViewModel,
                        accountFormViewModel = DI.accountFormViewModel,
                        categoryListViewModel = DI.categoryListViewModel,
                        categoryFormViewModel = DI.categoryFormViewModel,
                        settingsViewModel = DI.settingsViewModel,
                        messageRegexListViewModel = DI.messageRegexListViewModel,
                        messageRegexFormViewModel = DI.messageRegexFormViewModel,
                        messageListViewModel = DI.messageListViewModel,
                        senderListViewModel = DI.senderListViewModel,
                        senderFormViewModel = DI.senderFormViewModel,
                        loginViewModel = DI.loginViewModel
                    )
                }
            }
        }
    }
}
