package com.mobilemoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
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
            MobileMoneyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MobileMoneyNavigation()
                }
            }
        }
    }
}