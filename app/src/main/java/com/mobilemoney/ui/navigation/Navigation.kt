package com.mobilemoney.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobilemoney.MobileMoneyApp
import com.mobilemoney.ui.screens.AccountFormScreen
import com.mobilemoney.ui.screens.CategoryFormScreen
import com.mobilemoney.ui.screens.SettingsScreen
import com.mobilemoney.ui.screens.TransactionFormScreen
import com.mobilemoney.ui.screens.TransactionListScreen
import java.util.UUID

sealed class Screen(val route: String) {
    data object TransactionList : Screen("transactions")
    data object CreateTransaction : Screen("create")
    data object EditTransaction : Screen("edit/{transactionId}") {
        fun createRoute(transactionId: UUID) = "edit/$transactionId"
    }
    data object Accounts : Screen("accounts")
    data object CreateAccount : Screen("account/create")
    data object EditAccount : Screen("account/edit/{accountId}") {
        fun createRoute(accountId: UUID) = "account/edit/$accountId"
    }
    data object Categories : Screen("categories")
    data object CreateCategory : Screen("category/create")
    data object EditCategory : Screen("category/edit/{categoryId}") {
        fun createRoute(categoryId: UUID) = "category/edit/$categoryId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun MobileMoneyNavigation() {
    val context = LocalContext.current
    val app = context.applicationContext as MobileMoneyApp
    var showLoading by remember { mutableStateOf(app.isFirstRun()) }

    LaunchedEffect(Unit) {
        if (app.isFirstRun()) {
            while (!app.isInitialized) {
                kotlinx.coroutines.delay(500)
            }
            showLoading = false
        }
    }

    if (showLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text("Инициализация базы данных...", modifier = Modifier.padding(top = 16.dp))
        }
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.TransactionList.route
    ) {
        composable(Screen.TransactionList.route) {
            TransactionListScreen(
                onAddClick = {
                    navController.navigate(Screen.CreateTransaction.route)
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Screen.EditTransaction.createRoute(transactionId))
                },
                onAccountsClick = {
                    navController.navigate(Screen.Accounts.route)
                },
                onCategoriesClick = {
                    navController.navigate(Screen.Categories.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.CreateTransaction.route) {
            TransactionFormScreen(
                transactionId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")?.let {
                UUID.fromString(it)
            }
            TransactionFormScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Accounts.route) {
            com.mobilemoney.ui.screens.AccountListScreen(
                onAddClick = {
                    navController.navigate(Screen.CreateAccount.route)
                },
                onAccountClick = { accountId ->
                    navController.navigate(Screen.EditAccount.createRoute(accountId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CreateAccount.route) {
            AccountFormScreen(
                accountId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(
                navArgument("accountId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")?.let {
                UUID.fromString(it)
            }
            AccountFormScreen(
                accountId = accountId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Categories.route) {
            com.mobilemoney.ui.screens.CategoryListScreen(
                onAddClick = {
                    navController.navigate(Screen.CreateCategory.route)
                },
                onCategoryClick = { categoryId ->
                    navController.navigate(Screen.EditCategory.createRoute(categoryId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CreateCategory.route) {
            CategoryFormScreen(
                categoryId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditCategory.route,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")?.let {
                UUID.fromString(it)
            }
            CategoryFormScreen(
                categoryId = categoryId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}